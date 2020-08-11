package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.*;
import org.srg.smartclient.utils.ContextualRuntimeException;
import org.srg.smartclient.utils.Utils;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class JDBCHandler extends AbstractDSHandler {
    public interface JDBCPolicy {
        // http://java.avdiel.com/Tutorials/JDBCPaging.html
        void withConnectionDo(String database, Utils.CheckedFunction<Connection, Void> callback) throws Exception;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JDBCPolicy policy;

    public JDBCHandler(JDBCPolicy jdbcPolicy, IDSRegistry dsRegistry, DataSource datasource) {
        super(dsRegistry, datasource);
        policy = jdbcPolicy;
    }

    protected static boolean isSubEntityFetchRequired(DSField dsf){
        return dsf.isMultiple()
                || DSField.FieldType.ENTITY.equals(dsf.getType());
    }

    protected static String formatColumnNameToAvoidAnyPotentionalDuplication(DataSource ds, DSField field) {
        return "%s_%s"
                .formatted(
                        field.getDbName(),
                        ds.getTableName()
                );

    }

    private String formatFieldNameFor(boolean formatForSelect, DSField dsf) {
        final ForeignRelation effectiveRelation;
        final String effectiveColumn;

        if ( isSubEntityFetchRequired(dsf) ) {

            if (!formatForSelect) {
                throw new IllegalStateException("MNot supported yet.");
            }

            // Populate extra information for the sake of troubleshooting
            final String extraInfo;
            final ForeignKeyRelation foreignKeyRelation = describeForeignKey(dsf);
            if (logger.isDebugEnabled()) {
                extraInfo = "Sub-entity placeholder for '%s' (will be fetched as a subsequent request)"
                        .formatted(
                                foreignKeyRelation
                        );
            } else {
                extraInfo = "Sub-entity placeholder for '%s' (will be fetched as a subsequent request)"
                        .formatted(foreignKeyRelation.foreign().dataSourceId());
            }

            /*
             * Correspondent entity will be fetched by the subsequent query,
             * therefore it is required to reserve space in the response
             */
            effectiveRelation = new ForeignRelation(foreignKeyRelation.dataSource().getId(), foreignKeyRelation.dataSource(),
                    dsf.getName(), dsf );

            effectiveColumn = "NULL  /*  %s  */"
                    .formatted(extraInfo);
        } else {
            effectiveRelation = determineEffectiveField(dsf);

            // If a custom SQL snippet is provided for column -- use it
            if (effectiveRelation.field().isCustomSQL()
                    && effectiveRelation.field().getCustomSelectExpression() != null
                    && !effectiveRelation.field().getCustomSelectExpression().isBlank()) {
                effectiveColumn = effectiveRelation.field().getCustomSelectExpression();
            } else {
                effectiveColumn = effectiveRelation.formatAsSQL();
            }
        }

        final String formattedFieldName = formatColumnNameToAvoidAnyPotentionalDuplication(
                effectiveRelation.dataSource(),
                effectiveRelation.field()
            );

        if (!formatForSelect) {
            return formattedFieldName;
        }


        /*
         * It is required to introduce a column alias, to avoid column name duplication
         */
        return "%s AS %s"
                .formatted(
                    effectiveColumn,
                    formattedFieldName
                );
    }

    protected String formatFieldNameForSqlOrderClause(DSField dsf) {
        return formatFieldNameFor(false, dsf);
    }

    protected String formatFieldNameForSqlSelectClause(DSField dsf) {
        return formatFieldNameFor(true, dsf);
    }

    protected DSResponse handleFetch(DSRequest request) throws Exception {
        final SQLExecutionContext sqlExecutionContext = new SQLExecutionContext();

        final int pageSize = request.getEndRow() == -1  ? -1 : request.getEndRow() - request.getStartRow();

        final OperationBinding operationBinding = getEffectiveOperationBinding(DSRequest.OperationType.FETCH);

        // -- LIMIT
        final String paginationClause =  pageSize <= 0 ? "" : String.format("LIMIT %d OFFSET %d",
                request.getEndRow(),  request.getStartRow());

        // -- SELECT

        // -- filter requested columns if any provided, or stick with all
        if (request.getOutputs() == null || request.getOutputs().isBlank()) {
            sqlExecutionContext.getRequestedFields().addAll(getDataSource().getFields());
        } else {
            for ( String s: request.getOutputs().split(",")) {
                final String fn = s.trim();
                if (!fn.isBlank()) {
                    final DSField dsf = getField(fn);
                    if (dsf == null) {
                        throw new RuntimeException("%s: nothing known about requested field '%s', data source: %s."
                                .formatted(getClass().getSimpleName(), fn, getDataSource().getId()));
                    }

                    sqlExecutionContext.getRequestedFields().add(dsf);
                }
            }
        }

        if (request.getAdditionalOutputs() != null
                && !request.getAdditionalOutputs().isBlank()) {
            final Map<DSField, List<ForeignRelation>> additionalOutputs = Stream.of(request.getAdditionalOutputs().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.isBlank())
                    .map( descr -> {
                        final String[] parsed = descr.split("!");

                        if (parsed.length != 2) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', valid format is 'localFieldName!relatedDataSourceID.relatedDataSourceFieldName'."
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    )
                            );
                        }

                        final String sourceFieldName = parsed[0].trim();

                        final DSField sourceField = JDBCHandler.this.getField(sourceFieldName);
                        if (sourceField == null) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', nothing known about field '%s'."
                                    .formatted(
                                            dataSource().getId(),
                                            descr,
                                            sourceFieldName
                                    )
                            );
                        }

                        final ForeignKeyRelation fkRelation;

                        try {
                            fkRelation = describeForeignKey(sourceField);
                        } catch (Throwable t) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    t
                            );
                        }

                        final ForeignRelation fRelation;

                        try {
                            fRelation = describeForeignRelation(parsed[1].trim());
                        } catch (Throwable t) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    t
                            );
                        }

                        if (!fkRelation.foreign().dataSourceId().equals(fRelation.dataSourceId())) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    )
                            );
                        }

                        return new AbstractMap.SimpleImmutableEntry<>(sourceField, fRelation);
                    })
                    .collect(
                            Collectors.groupingBy(
                                    AbstractMap.SimpleImmutableEntry::getKey,
                                    Collectors.mapping(
                                            AbstractMap.SimpleImmutableEntry::getValue,
                                            Collectors.toList()
                                    )
                            )
                    );

            sqlExecutionContext.getAdditionalOutputs().putAll(additionalOutputs);
        }

        final String selectClause = sqlExecutionContext.getRequestedFields()
            .stream()
                .map(this::formatFieldNameForSqlSelectClause)
                .collect(Collectors.joining(",\n  "));


        // -- FROM
        final String fromClause = getDataSource().getTableName();

        // -- JOIN ON
        final String joinClause = getFields()
            .stream()
            .filter( dsf -> dsf.isIncludeField()
                    /*
                    * Entities will be fetched separately via sub-entity fetch request,
                    * therefore exclude this field from the sql join.
                    */
                    && !DSField.FieldType.ENTITY.equals(dsf.getType()))
            .map( dsf -> {

                final ImportFromRelation relation = describeImportFrom(dsf);

                return " JOIN %s ON %s.%s = %s.%s"
                        .formatted(
                                relation.foreignDataSource().getTableName(),
                                this.getDataSource().getTableName(), relation.sourceField().getDbName(),
                                relation.foreignDataSource().getTableName(), relation.foreignKey().getDbName()
                        );
            })
            .collect(Collectors.joining(" \n "));


        // -- WHERE
        sqlExecutionContext.getFilterData().addAll(generateFilterData(request.getTextMatchStyle(), request.getData()));

        final List<Object[]> data;
        if (pageSize > 0) {
            data = new ArrayList<>(pageSize);
        } else {
            data = new LinkedList<>();
        }

        final int[] totalRows = new int[] {-1};

        policy.withConnectionDo(this.getDataSource().getDbName(), conn-> {

            // -- fetch data
            final String orderClause = request.getSortBy() == null ? "" :  " ORDER BY \n" +
                    request.getSortBy().stream()
                            .map(s -> {
                                String order = "";
                                switch (s.charAt(0)) {
                                    case '-':
                                        order = " DESC";
                                    case '+':
                                        s = s.substring(1);
                                    default:
                                        final DSField dsf = getField(s);
                                        if (dsf == null) {
                                            throw new RuntimeException("Data source '%s': nothing known about field '%s' listed in order by clause."
                                                    .formatted(getDataSource().getId(), s));
                                        }

                                        return "%s.%s%s"
                                                .formatted(
                                                        "opaque",
                                                        formatFieldNameForSqlOrderClause(dsf),
                                                        order
                                                );
                                }
                            })
                            .collect(Collectors.joining(", "));

            final String whereClause = sqlExecutionContext.getFilterData().isEmpty() ?  "" : sqlExecutionContext.getFilterData().stream()
                .map(fd -> fd.sql("opaque"))
                .collect(Collectors.joining("\n\t\t AND "));

            // -- generate query
            final String genericQuery;
            {
                final Map<String, Object> templateContext = SQLTemplateEngine.createContext(request, selectClause, fromClause, joinClause, whereClause, "");
                sqlExecutionContext.setTemplateContext(templateContext);

                templateContext.put("effectiveSelectClause", selectClause);

                final String effectiveFROM = operationBinding == null
                        || operationBinding.getTableClause() == null
                        || operationBinding.getTableClause().isBlank()
                        ? fromClause : operationBinding.getTableClause();
                templateContext.put("effectiveTableClause", effectiveFROM);

                final String effectiveWhere = operationBinding == null
                        || operationBinding.getWhereClause() == null
                        || operationBinding.getWhereClause().isBlank()
                        ? whereClause : operationBinding.getWhereClause();
                templateContext.put("effectiveWhereClause", effectiveWhere);


                final String effectiveJoin = operationBinding == null
                        || operationBinding.getAnsiJoinClause() == null
                        || operationBinding.getAnsiJoinClause().isBlank()
                        ? joinClause : operationBinding.getAnsiJoinClause();
                templateContext.put("effectiveAnsiJoinClause", effectiveJoin);

                genericQuery = SQLTemplateEngine.processSQL(templateContext,
                        """
                        (
                            SELECT ${effectiveSelectClause}
                                FROM ${effectiveTableClause}
                            <#if effectiveAnsiJoinClause?has_content>
                                ${effectiveAnsiJoinClause}
                            </#if>
                        ) opaque                                                                                                              
                        <#if effectiveWhereClause?has_content>
                            WHERE ${effectiveWhereClause}
                        </#if>                                                                                                              
                        """
                );
            }

            // -- calculate total
            /*
             * Opaque query is required for a proper filtering by calculated fields
             */
            @SuppressWarnings("SqlNoDataSourceInspection")
            final String countQuery = "SELECT count(*) FROM %s"
                    .formatted( genericQuery);

            if (logger.isTraceEnabled()) {
                logger.trace("DataSource %s fetch count(*) query:\n%s\n\nparams:\n%s"
                        .formatted(
                                getDataSource().getId(),
                                countQuery,
                                sqlExecutionContext.getFilterData().stream()
                                        .flatMap(fd -> StreamSupport.stream(fd.values().spliterator(), false))
                                        .map("%s"::formatted)
                                        .collect(Collectors.joining(", "))
                        )
                );
            }

            sqlExecutionContext.setEffectiveSQL(countQuery);

            try (PreparedStatement st = conn.prepareStatement(countQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                int idx =0;

                for (IFilterData fd: sqlExecutionContext.getFilterData()) {
                    idx = fd.setStatementParameters(idx, st);
                }

                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    totalRows[0] = rs.getInt(1);
                }
            } catch (Throwable t) {
                throw new ContextualRuntimeException("SQL count query execution failed.", t, sqlExecutionContext);
            }

            // -- fetch data
            /*
             * Opaque query is required for a proper filtering by calculated fields
             */
            @SuppressWarnings("SqlNoDataSourceInspection")
            final String opaqueFetchQuery = """
                 SELECT * FROM %s
                    %s
                    %s
            """.formatted(genericQuery, orderClause, paginationClause);

            if (logger.isTraceEnabled()) {
                logger.trace("DataSource %s fetch query:\n%s\n\nparams:\n%s"
                    .formatted(
                        getDataSource().getId(),
                            opaqueFetchQuery,
                            sqlExecutionContext.getFilterData().stream()
                                .flatMap(fd -> sqlExecutionContext.getFilterData().stream())
                                .map("%s"::formatted)
                                .collect(Collectors.joining(", "))
                    )
                );
            }

            sqlExecutionContext.setEffectiveSQL(opaqueFetchQuery);

            try(PreparedStatement st = conn.prepareStatement(opaqueFetchQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)){
                st.setFetchSize(pageSize);
                st.setMaxRows(pageSize);
                st.setFetchDirection(ResultSet.FETCH_FORWARD);

                int idx =0;
                for (IFilterData fd: sqlExecutionContext.getFilterData()) {
                    idx = fd.setStatementParameters(idx, st);
                }

                final Map<String, Object> pkValues = sqlExecutionContext.getPkValues();
                pkValues.clear();

                try (ResultSet rs = st.executeQuery() ) {
                    while (rs.next())  {

                        final Object[] r = new Object[getFields().size()];

                        int i =0;

                        // ORIGINAL FIELD ORDER MUST BE PRESERVED
                        for (DSField dsf: sqlExecutionContext.getRequestedFields()) {
                            Object v = rs.getObject(i + 1);
                            if (rs.wasNull()) {
                                v = null;
                            }
                            r[i++] = v;

                            if (dsf.isPrimaryKey()) {
                                pkValues.put(dsf.getName(), v);
                            }
                        }

                        for(int j=0; j<sqlExecutionContext.getRequestedFields().size(); ++j) {
                            final DSField dsf = sqlExecutionContext.getRequestedFields().get(j);

                            r[j] = postProcessFieldValue(sqlExecutionContext, dsf, r[j]);
                        }

                        pkValues.clear();

                        final Object[] postProcessed = postProcessRow(sqlExecutionContext, r);

                        assert postProcessed.length == r.length;
                        data.add(postProcessed);
                    }
                    return null;
                } catch (Throwable t) {
                    if (t instanceof ContextualRuntimeException) {
                        throw t;
                    }
                    throw new ContextualRuntimeException("SQL fetch query execution failed.", t, sqlExecutionContext);
                }
            }
        });

        return DSResponse.success(request.getStartRow(), request.getStartRow() + data.size(), totalRows[0],
                sqlExecutionContext.getRequestedFields(),
                data);
    }

    protected Object[] postProcessRow(SQLExecutionContext ctxt, Object[] row) {
        return row;
    }

    protected Object postProcessFieldValue(SQLExecutionContext ctxt, DSField dsf, Object value) {

        if (!isSubEntityFetchRequired(dsf)) {
            return value;
        }

        /*
         * Fetch sub-entities
         */

        logger.trace("Processing multi-value and/or Entity relations for data source '%s', requested fields '%s'..."
                .formatted(
                        getDataSource().getId(),
                        ctxt.getRequestedFields()
                )
        );

        final ForeignKeyRelation foreignKeyRelation;

        if (dsf.getIncludeFrom() != null
                && !dsf.getIncludeFrom().isBlank()) {
            /*
             * Use 'includeFrom' if it is provided
             */
            final ImportFromRelation importFromRelation = describeImportFrom(dsf);
            foreignKeyRelation = importFromRelation.toForeignDisplayKeyRelation();
        } else {
            foreignKeyRelation = describeForeignKey(dsf);
        }

        final List<ForeignRelation> ffs = ctxt.getAdditionalOutputs().get(dsf);
        final String entityOutputs = ffs == null ? null : ffs.stream()
                .map(ForeignRelation::fieldName)
                .collect(Collectors.joining(", "));

        final DSResponse response;
        try {
            response = fetchForeignEntity(foreignKeyRelation, entityOutputs, ctxt.getPkValues());
            assert response != null;
        } catch ( Throwable t) {
            throw new RuntimeException("Subsequent entity fetch failed: %s, filters: %s"
                    .formatted(
                            foreignKeyRelation,
                            ctxt.getPkValues().entrySet().stream()
                                    .map( e -> "'%s': %s"
                                            .formatted(
                                                    e.getKey(),
                                                    e.getValue()
                                            )
                                    )
                                    .collect(Collectors.joining(","))
                    ),
                    t
            );
        }

        if (0 != response.getStatus()) {
            throw new RuntimeException("Subsequent entity fetch failed: %s, %s, filters: %s"
                    .formatted(
                            response.getData().getGeneralFailureMessage(),
                            foreignKeyRelation,
                            ctxt.getPkValues().entrySet().stream()
                                    .map( e -> "'%s': %s"
                                            .formatted(
                                                    e.getKey(),
                                                    e.getValue()
                                            )
                                    )
                                    .collect(Collectors.joining(","))
                    )
            );
        }

        Object o = response.getData().getRawDataResponse();
        assert o != null;

        return o;
    }

    protected DSResponse fetchForeignEntity(ForeignKeyRelation foreignKeyRelation, String outputs, Map<String, Object> filtersAndKeys) throws Exception {
        logger.debug("Performing foreign fetch for relation '%s' with criteria: %s"
                .formatted(
                        foreignKeyRelation,
                        filtersAndKeys
                )
            );

        if (filtersAndKeys.size() > 1) {
            throw new IllegalStateException("Fetch Entity does not supports composite keys");
        }

        final DSHandler dsHandler = this.getDataSourceHandlerById(foreignKeyRelation.foreign().dataSourceId());
        if (dsHandler == null) {
            throw new RuntimeException( "Foreign data source handler with id '%s' is not registered."
                    .formatted(foreignKeyRelation.foreign().dataSourceId())
            );
        }

        final DSRequest fetchEntity = new DSRequest();
        fetchEntity.setDataSource(dsHandler.id());
        fetchEntity.setOperationType(DSRequest.OperationType.FETCH);
        fetchEntity.setOutputs(outputs);

        /*
         * if type is not provided this indicates that the only PKs should be fetched.
         *
         * @see <a href="https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/JpaHibernateRelations.html">JPA & Hibernate Relations</a>
         */
        if (null == foreignKeyRelation.sourceField().getType()) {
            logger.trace(("Field type is not provided for the source field '%s.%s', therefore the only primary key[s] " +
                    "will be fetched by the foreign fetch %s.")
                    .formatted(
                            foreignKeyRelation.dataSource().getId(),
                            foreignKeyRelation.sourceField().getName(),
                            foreignKeyRelation
                    )
            );

            final DataSource foreignDS = foreignKeyRelation.foreign().dataSource();

            final String pkNames = foreignDS.getFields().stream()
                    .filter(DSField::isPrimaryKey)
                    .map(DSField::getName)
                    .collect(Collectors.joining(", "));

            fetchEntity.setOutputs(pkNames);
        }

        fetchEntity.wrapAndSetData(Map.of(foreignKeyRelation.foreign().fieldName(), filtersAndKeys.values().iterator().next()));

        return dsHandler.handle(fetchEntity);
    }

    protected  List<IFilterData> generateFilterData(DSRequest.TextMatchStyle textMatchStyle, IDSRequestData data ) {
        if (data instanceof Map) {
            return ((Map<String, Object>) data).entrySet()
                    .stream()
                    .map(e -> {
                        final DSField dsf = getField(e.getKey());

                        if (dsf == null) {
                            throw new RuntimeException("DataSource '%s': nothing known about field '%s'"
                                    .formatted(
                                            getDataSource().getId(),
                                            e.getKey()
                                    )
                            );
                        }

                        @SuppressWarnings("SwitchStatementWithTooFewBranches")
                        final Object value = switch (dsf.getType()) {
                            case TEXT -> switch (textMatchStyle) {
                                case EXACT -> e.getValue();

                                case SUBSTRING -> "%%%s%%".formatted(e.getValue());

                                default -> "%s%%".formatted(e.getValue());
                            };
                            default -> e.getValue();
                        };


                        @SuppressWarnings("SwitchStatementWithTooFewBranches")
                        String filterStr = switch (dsf.getType()) {
                            case TEXT -> "%s like ?";
                            default -> "%s = ?";
                        };

                        final ForeignRelation effectiveField = determineEffectiveField(dsf);
                        return new FilterData(effectiveField, filterStr, value);
                    })
                    .collect(Collectors.toList());
        } else if (data == null){
            return List.of();
        } else {
            throw new IllegalStateException("DataSource '%s': data has unsupported format '%s'."
                    .formatted(
                            getDataSource().getId(),
                            data.getClass().getCanonicalName()
                    )
            );
        }
    }

    protected interface IFilterData {
        String sql();
        String sql(String aliasOrTable);

        Iterable<Object> values();

        default int setStatementParameters(int idx, PreparedStatement preparedStatement) throws SQLException {
            for (Object v : values()) {
                preparedStatement.setObject(++idx, v);
            }
            return idx;
        }

    }

    protected static class FilterData implements IFilterData{
        private final ForeignRelation dsFieldPair;
        private final String sqlTemplate;
        private transient String formattedSql;
        private final Object value;

        public FilterData(ForeignRelation dsFieldPair, String sqlTemplate, Object value) {
            this.dsFieldPair = dsFieldPair;
            this.sqlTemplate = sqlTemplate;
            this.value = value;
        }

        @SuppressWarnings("unused")
        public FilterData(ForeignRelation dsFieldPair, String sqlTemplate, Object... values) {
            this.dsFieldPair = dsFieldPair;
            this.sqlTemplate = sqlTemplate;
            this.value = values;
        }

        @Override
        public String sql() {
            if (formattedSql == null) {
                formattedSql = this.sql(dsFieldPair.dataSource().getTableName());
            }
            return formattedSql;
        }

        @Override
        public String sql(String aliasOrTable) {
            assert sqlTemplate != null;

            return sqlTemplate.formatted(
                    "%s.%s".formatted(
                            aliasOrTable,
                            formatColumnNameToAvoidAnyPotentionalDuplication(
                                    getDsFieldPair().dataSource(),
                                    getDsFieldPair().field()
                            )
                    )
            );
        }

        public DSField field() {
            return dsFieldPair.field();
        }

        protected ForeignRelation getDsFieldPair() {
            return dsFieldPair;
        }

        public Iterable<Object> values() {
            return Collections.singletonList(value);
        }

        @Override
        public String toString() {
            return "FilterData{" +
                    "field=" + dsFieldPair +
                    ", sqlTemplate='" + sqlTemplate + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    public static class FetchContext {
        private List<DSField> requestedFields = new LinkedList<>();
        private Map<DSField, List<ForeignRelation>> additionalOutputs = new HashMap<>();
        private List<IFilterData> filterData = new LinkedList<>();

        private Map<String, Object> pkValues = new HashMap<>();

        public List<DSField> getRequestedFields() {
            return requestedFields;
        }

        public Map<DSField, List<ForeignRelation>> getAdditionalOutputs() {
            return additionalOutputs;
        }

        public Map<String, Object> getPkValues() {
            return pkValues;
        }

        public void setPkValue(Map<String, Object> pkValues) {
            this.pkValues = pkValues;
        }

        public List<IFilterData> getFilterData() {
            return filterData;
        }
    }

    public static class SQLExecutionContext extends FetchContext {
        private Map<String, Object> templateContext;
        private String effectiveSQL;

        public String getEffectiveSQL() {
            return effectiveSQL;
        }

        public void setEffectiveSQL(String effectiveSQL) {
            this.effectiveSQL = effectiveSQL;
        }

        public Map<String, Object> getTemplateContext() {
            return templateContext;
        }

        public void setTemplateContext(Map<String, Object> templateContext) {
            this.templateContext = templateContext;
        }
    }
}

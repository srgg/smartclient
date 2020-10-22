package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.*;
import org.srg.smartclient.utils.ContextualRuntimeException;
import org.srg.smartclient.utils.Utils;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.srg.smartclient.isomorphic.DSField.FieldType.TEXT;


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

    @Override
    protected DSResponse handleUpdate(DSRequest request) throws Exception {
        if (! (request.getData() instanceof Map)) {
            throw new RuntimeException("Bad request: operation 'UPDATE', the map of modified and PK fields " +
                    "must be provided in the  'data' field.");
        }

        final OperationBinding operationBinding = getEffectiveOperationBinding(DSRequest.OperationType.FETCH);
        final SQLUpdateContext<JDBCHandler> sqlUpdateContext = new SQLUpdateContext<>(this, request, operationBinding);


        // --
        final DSResponse response[] = {null};

        policy.withConnectionDo(this.getDataSource().getDbName(), conn-> {

            try (PreparedStatement st = conn.prepareStatement(sqlUpdateContext.getUpdateSQL(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                int idx =0;

                final List<IFilterData> l = sqlUpdateContext.getModifiedData();
                for (IFilterData fd: l) {
                    idx = fd.setStatementParameters(idx, st);
                }

                final List<IFilterData> ll = sqlUpdateContext.getPkFieldData();
                for (IFilterData fd: ll) {
                    idx = fd.setStatementParameters(idx, st);
                }

                final int qnt = st.executeUpdate();


                if (qnt == 0) {
                    // There is no updated/affected records
                    throw new RuntimeException("Zero rows were updated.");
                }
            } catch (Throwable t) {
                conn.rollback();
                throw new ContextualRuntimeException("SQL update query execution failed.", t, sqlUpdateContext);
            }

            /*
             * It is required to return modified row back to the client.
             *
             * This can be done either by:
             *  1) comprehensive fetch from DB
             *  2) by modifying "old values" sent from client w/o real  retch.
             *
             * The first option is more durable, since it will handle all side effects like calculated fields? operation bindings
             * with respect of security rules tat can be affected by side effects.
             */


            final DSRequest fr = new DSRequest();
            fr.setDataSource(request.getDataSource());
            fr.setOperationType(DSRequest.OperationType.FETCH);
            fr.setOperationId(request.getOperationId());
            fr.setComponentId(request.getComponentId());
            fr.wrapAndSetData(sqlUpdateContext.getPkValues());

            // return the only fields that were provided within the request, except metadata
            fr.setOutputs(
                request.getOldValues().keySet().stream()
                    .filter( s -> !s.startsWith(getMetaDataPrefix()))
                    .collect(Collectors.joining(", "))
            );

            final DSResponse r =  handleFetch(fr);

            if (r.getStatus() == DSResponse.STATUS_SUCCESS ) {
                conn.commit();
            } else {
                conn.rollback();
            }

            response[0] = r;
            return null;
        });

        final DSResponse fetchRespone = response[0];

        if (fetchRespone.getStatus() != DSResponse.STATUS_SUCCESS) {
            return fetchRespone;
        }

        DSResponse r = DSResponse.successUpdate(fetchRespone.getData());

        return r;
    }

    @Override
    protected DSResponse handleFetch(DSRequest request) throws Exception {
        return doHandleFetch(request, null, true);
    }

    protected DSResponse doHandleFetch(DSRequest request, Connection connection, boolean calculateTotal) throws Exception {
        final OperationBinding operationBinding = getEffectiveOperationBinding(DSRequest.OperationType.FETCH);
        final SQLFetchContext<JDBCHandler> sqlFetchContext = new SQLFetchContext<>(this, request, operationBinding);

        final List<Object[]> data;
        if (sqlFetchContext.getPageSize() > 0) {
            data = new ArrayList<>(sqlFetchContext.getPageSize());
        } else {
            data = new LinkedList<>();
        }

        final int[] totalRows = new int[] {-1};

        policy.withConnectionDo(this.getDataSource().getDbName(), conn-> {
            // -- calculate total
            /*
             * Opaque query is required for a proper filtering by calculated fields
             */
            @SuppressWarnings("SqlNoDataSourceInspection")
            final String countQuery = "SELECT count(*) FROM %s"
                    .formatted( sqlFetchContext.getGenericQuery());

            if (logger.isTraceEnabled()) {
                logger.trace("DataSource %s fetch count(*) query:\n%s\n\nparams:\n%s"
                        .formatted(
                                getDataSource().getId(),
                                countQuery,
                                sqlFetchContext.getFilterData().stream()
                                        .flatMap(fd -> StreamSupport.stream(fd.values().spliterator(), false))
                                        .map("%s"::formatted)
                                        .collect(Collectors.joining(", "))
                        )
                );
            }

            sqlFetchContext.setEffectiveSQL(countQuery);

            try (PreparedStatement st = conn.prepareStatement(countQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                int idx =0;

                for (IFilterData fd: sqlFetchContext.getFilterData()) {
                    idx = fd.setStatementParameters(idx, st);
                }

                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    totalRows[0] = rs.getInt(1);
                }
            } catch (Throwable t) {
                throw new ContextualRuntimeException("SQL count query execution failed.", t, sqlFetchContext);
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
            """.formatted(sqlFetchContext.getGenericQuery(), sqlFetchContext.getOrderClause(), sqlFetchContext.getPaginationClause());

            if (logger.isTraceEnabled()) {
                logger.trace("DataSource %s fetch query:\n%s\n\nparams:\n%s"
                    .formatted(
                        getDataSource().getId(),
                        opaqueFetchQuery,
                        sqlFetchContext.getFilterData().stream()
                                .flatMap(fd -> sqlFetchContext.getFilterData().stream())
                                .flatMap(fd -> StreamSupport.stream(fd.values().spliterator(), false))
                                .map("%s"::formatted)
                                .collect(Collectors.joining(", "))
                    )
                );
            }

            sqlFetchContext.setEffectiveSQL(opaqueFetchQuery);

            try(PreparedStatement st = conn.prepareStatement(opaqueFetchQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)){
                st.setFetchSize(sqlFetchContext.getPageSize());
                st.setMaxRows(sqlFetchContext.getPageSize());
                st.setFetchDirection(ResultSet.FETCH_FORWARD);

                int idx =0;
                for (IFilterData fd: sqlFetchContext.getFilterData()) {
                    idx = fd.setStatementParameters(idx, st);
                }

                final Map<String, Object> rowPkValues =  new HashMap<>();
                try (ResultSet rs = st.executeQuery() ) {
                    while (rs.next())  {

                        final Object[] r = new Object[getFields().size()];

                        int i =0;

                        // ORIGINAL FIELD ORDER MUST BE PRESERVED
                        for (DSField dsf: sqlFetchContext.getRequestedFields()) {
                            Object v = rs.getObject(i + 1);
                            if (rs.wasNull()) {
                                v = null;
                            }
                            r[i++] = v;

                            if (dsf.isPrimaryKey()) {
                                rowPkValues.put(dsf.getName(), v);
                            }
                        }

                        for(int j = 0; j< sqlFetchContext.getRequestedFields().size(); ++j) {
                            final DSField dsf = sqlFetchContext.getRequestedFields().get(j);
                            r[j] = postProcessFieldValue(sqlFetchContext, rowPkValues, dsf, r[j]);
                        }

                        rowPkValues.clear();

                        final Object[] postProcessed = postProcessRow(sqlFetchContext, r);

                        assert postProcessed.length == r.length;
                        data.add(postProcessed);
                    }
                    return null;
                } catch (Throwable t) {
                    if (t instanceof ContextualRuntimeException) {
                        throw t;
                    }
                    throw new ContextualRuntimeException("SQL fetch query execution failed.", t, sqlFetchContext);
                }
            }
        });

        return DSResponse.successFetch(request.getStartRow(), request.getStartRow() + data.size(), totalRows[0],
                sqlFetchContext.getRequestedFields(),
                data);
    }

    protected Object[] postProcessRow(SQLFetchContext<JDBCHandler> ctx, Object[] row) {
        return row;
    }

    protected Object postProcessFieldValue(SQLFetchContext<JDBCHandler> ctx, Map<String, Object> rowPkValues, DSField dsf, Object value) {

        if (!isSubEntityFetchRequired(dsf)) {
            return value;
        }

        /*
         * Fetch sub-entities
         */

        logger.trace("Processing multi-value and/or Entity relations for data source '%s', requested fields '%s'..."
                .formatted(
                        getDataSource().getId(),
                        ctx.getRequestedFields()
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

        final List<ForeignRelation> ffs = ctx.getAdditionalOutputs().get(dsf);
        final String entityOutputs = ffs == null ? null : ffs.stream()
                .map(ForeignRelation::fieldName)
                .collect(Collectors.joining(", "));

        final DSResponse response;
        try {
            response = fetchForeignEntity(foreignKeyRelation, entityOutputs, rowPkValues);
            assert response != null;
        } catch ( Throwable t) {
            throw new RuntimeException("Subsequent entity fetch failed: %s, filters: %s"
                    .formatted(
                            foreignKeyRelation,
                            rowPkValues.entrySet().stream()
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
                            rowPkValues.entrySet().stream()
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

    protected  List<IFilterData> generateFilterData(DSRequest.OperationType operationType, DSRequest.TextMatchStyle textMatchStyle, IDSRequestData data ) {
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

                        final String filterStr;

                        if (TEXT.equals(dsf.getType()) && (!DSRequest.TextMatchStyle.EXACT.equals(textMatchStyle)) ) {
                            filterStr = "%s like ?";
                        } else {
                            filterStr = "%s = ?";
                        };

                        final ForeignRelation effectiveField = determineEffectiveField(dsf);
                        effectiveField.setSqlFieldAlias(
                            operationType == DSRequest.OperationType.FETCH ?
                                formatColumnNameToAvoidAnyPotentialDuplication(
                                        effectiveField.dataSource(),
                                        effectiveField.field()
                                )
                                : effectiveField.field().getDbName());

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

    protected static String formatColumnNameToAvoidAnyPotentialDuplication(DataSource ds, DSField field) {
        return "%s_%s"
                .formatted(
                        field.getDbName(),
                        ds.getTableName()
                );
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
                dsFieldPair.formatAsSQL(aliasOrTable)
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

    public static abstract class AbstractSQLContext<H extends JDBCHandler> {
        private final H dsHandler;
        private final DSRequest request;
        private final OperationBinding operationBinding;

//        private Map<String, Object> pkValues = new HashMap<>();


        public AbstractSQLContext(H dsHandler, DSRequest request, OperationBinding operationBinding) throws Exception {
            this.request = request;
            this.operationBinding = operationBinding;
            this.dsHandler = dsHandler;
        }

        protected DataSource dataSource() {
            assert dsHandler != null;
            return dsHandler.dataSource();
        }

        protected H dsHandler() {
            assert dsHandler != null;
            return dsHandler;
        }

        public OperationBinding operationBinding() {
            return operationBinding;
        }

        protected DSRequest request() {
            assert request != null;
            return request;
        }

        public static Map<String, Object> convertFilterDataToMap(Collection<IFilterData> ifds) {
            final Map<String, Object> m = new HashMap<>();

            ifds.forEach(
                    ifd -> {
                        assert ifd instanceof FilterData;
                        final FilterData fd = (FilterData) ifd;

                        m.put(fd.field().getName(), fd.value);
                    }
            );

            return m;
        }
    }
}

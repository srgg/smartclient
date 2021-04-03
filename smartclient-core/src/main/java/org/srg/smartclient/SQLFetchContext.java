package org.srg.smartclient;

import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.OperationBinding;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;
import org.srg.smartclient.utils.ContextualRuntimeException;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLFetchContext<H extends JDBCHandler> extends JDBCHandler.AbstractSQLContext<JDBCHandler> {
    private static Logger logger = LoggerFactory.getLogger(SQLFetchContext.class);
    private String genericQuery;
    private int pageSize;
    private String orderClause;
    private String paginationClause;

    private List<DSField> requestedFields = new LinkedList<>();
    private Map<DSField, List<RelationSupport.ForeignRelation>> additionalOutputs = new HashMap<>();
    private List<JDBCHandler.IFilterData> filterData = new LinkedList<>();

    private Map<String, Object> templateContext;

    private String effectiveSQL;

    public SQLFetchContext(H dsHandler, DSRequest request, OperationBinding operationBinding) throws Exception {
        super(dsHandler, request, operationBinding);
        init();
    }

    public List<DSField> getRequestedFields() {
        return requestedFields;
    }

    public Map<DSField, List<RelationSupport.ForeignRelation>> getAdditionalOutputs() {
        return additionalOutputs;
    }

    public List<JDBCHandler.IFilterData> getFilterData() {
        return filterData;
    }

    public Map<String, Object> getTemplateContext() {
        return templateContext;
    }

    public String getEffectiveSQL() {
        return effectiveSQL;
    }

    public void setEffectiveSQL(String effectiveSQL) {
        this.effectiveSQL = effectiveSQL;
    }

    protected String formatFieldNameForSqlOrderClause(DSField dsf) {
        return formatFieldNameFor(false, dsf);
    }


    private String formatFieldNameFor(boolean formatForSelect, DSField dsf) {
        final RelationSupport.ForeignRelation effectiveRelation;
        final String effectiveColumn;

        if (dsHandler().isSubEntityFetchRequired(dsf)) {

            if (!formatForSelect) {
                throw new IllegalStateException("MNot supported yet.");
            }

            // Populate extra information for the sake of troubleshooting
            final String extraInfo;
            final RelationSupport.ForeignKeyRelation foreignKeyRelation = dsHandler().describeForeignKey(dsf);
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
            effectiveRelation = new RelationSupport.ForeignRelation(foreignKeyRelation.dataSource().getId(), foreignKeyRelation.dataSource(),
                    dsf.getName(), dsf);

            effectiveColumn = "NULL  /*  %s  */"
                    .formatted(extraInfo);
        } else {
            effectiveRelation = dsHandler().determineEffectiveField(dsf);

            // If a custom SQL snippet is provided for column -- use it
            if (effectiveRelation.field().isCustomSQL()
                    && effectiveRelation.field().getCustomSelectExpression() != null
                    && !effectiveRelation.field().getCustomSelectExpression().isBlank()) {
                effectiveColumn = effectiveRelation.field().getCustomSelectExpression();
            } else {
                effectiveColumn = effectiveRelation.formatAsSQL();
            }
        }

        final String formattedFieldName = JDBCHandler.formatColumnNameToAvoidAnyPotentialDuplication(
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


    protected String formatFieldNameForSqlSelectClause(DSField dsf) {
        return formatFieldNameFor(true, dsf);
    }

    public String getOrderClause() {
        return orderClause;
    }

    public String getPaginationClause() {
        return paginationClause;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getGenericQuery() {
        return genericQuery;
    }

    protected void init() throws IOException, TemplateException {
        this.pageSize = request().getEndRow() == -1 ? -1 : request().getEndRow() - request().getStartRow();

        // -- LIMIT
        this.paginationClause = pageSize <= 0 ? "" : String.format("LIMIT %d OFFSET %d",
                request().getEndRow(), request().getStartRow());

        // -- fetch data
        this.orderClause = request().getSortBy() == null ? "" :  " ORDER BY \n" +
                request().getSortBy().stream()
                        .map(s -> {
                            String order = "";
                            switch (s.charAt(0)) {
                                case '-':
                                    order = " DESC";
                                case '+':
                                    s = s.substring(1);
                                default:
                                    final DSField dsf = dsHandler().getField(s);
                                    if (dsf == null) {
                                        throw new ContextualRuntimeException("Data source '%s': nothing known about field '%s' listed in order by clause."
                                                .formatted(dataSource().getId(), s), this);
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

        // -- SELECT

        // -- filter requested columns if any provided, or stick with all
        if (request().getOutputs() == null || request().getOutputs().isBlank()) {
            this.requestedFields.addAll(dsHandler().getDataSource().getFields());
        } else {
            for (String s : request().getOutputs().split(",")) {
                final String fn = s.trim();
                if (!fn.isBlank()) {
                    final DSField dsf = dsHandler().getField(fn);
                    if (dsf == null) {
                        throw new ContextualRuntimeException("%s: nothing known about requested field '%s', data source: %s."
                                .formatted(getClass().getSimpleName(), fn, dataSource().getId()), this);
                    }

                    this.requestedFields.add(dsf);
                }
            }
        }

        if (request().getAdditionalOutputs() != null
                && !request().getAdditionalOutputs().isBlank()) {
            final Map<DSField, List<RelationSupport.ForeignRelation>> additionalOutputs = Stream.of(request().getAdditionalOutputs().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.isBlank())
                    .map(descr -> {
                        final String[] parsed = descr.split("!");

                        if (parsed.length != 2) {
                            throw new ContextualRuntimeException("Data source '%s': Invalid additionalOutputs value '%s', valid format is 'localFieldName!relatedDataSourceID.relatedDataSourceFieldName'."
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    this
                            );
                        }

                        final String sourceFieldName = parsed[0].trim();

                        final DSField sourceField = dsHandler().getField(sourceFieldName);
                        if (sourceField == null) {
                            throw new ContextualRuntimeException("Data source '%s': Invalid additionalOutputs value '%s', nothing known about field '%s'."
                                    .formatted(
                                            dataSource().getId(),
                                            descr,
                                            sourceFieldName
                                    ),
                                    this
                            );
                        }

                        final RelationSupport.ForeignKeyRelation fkRelation;

                        try {
                            fkRelation = dsHandler().describeForeignKey(sourceField);
                        } catch (Throwable t) {
                            throw new ContextualRuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    t,
                                    this
                            );
                        }

                        final RelationSupport.ForeignRelation fRelation;

                        try {
                            fRelation = dsHandler().describeForeignRelation(dataSource(), sourceField, parsed[1].trim());
                        } catch (Throwable t) {
                            throw new ContextualRuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    t,
                                    this
                            );
                        }

                        if (!fkRelation.foreign().dataSourceId().equals(fRelation.dataSourceId())) {
                            throw new ContextualRuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    this
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

            this.additionalOutputs.putAll(additionalOutputs);
        }

        // -- SELECT

        /*
         * Ensure that all the filtered(criteria) fields will be requested/queried.
         *
         * Otherwise it WHERE clause will produce an error.
         */
        Set<String> extraFieldNames;

        if (request().getData() instanceof Map m) {
            // It is required to make a copy of the original keySet to prevent it from the modifications
            extraFieldNames = new HashSet<>(m.keySet());
        } else if (request().getData() instanceof AdvancedCriteria ac) {
            extraFieldNames = ac.getCriteriaFieldNames();
        } else if (request().getData() == null) {
            extraFieldNames = Set.of();
        } else {
            throw new IllegalStateException("Unsupported IDSRequestData type '%s'".formatted(request().getData().getClass().getCanonicalName()));
        }

        if (!extraFieldNames.isEmpty()) {
            extraFieldNames.removeIf(n -> requestedFields.stream().anyMatch(dsf -> dsf.getName().equals(n)));
        }
        final Set<DSField> extraFields = extraFieldNames.stream()
                .map( n ->  {
                    final DSField dsf = this.dataSource().getField(n);

                    if (dsf == null) {
                        /*
                         *  This still can be a valid state, if criteria was binded to represent stored function parameter
                         *  that is specified as part of a template.
                         */
                        final Set<String> excludedFields = Arrays.stream(this.operationBinding().getExcludeCriteriaFields().split(","))
                                .map( s -> s.trim())
                                .collect(Collectors.toSet());

                        if (!excludedFields.contains(n)) {
                            // Ok, that is definitely WRONG
                            throw new ContextualRuntimeException("%s: nothing known about an extra field '%s', data source: %s."
                                .formatted(getClass().getSimpleName(), n, dataSource().getId()), this);
                        }
                    }

                    return dsf;
                })
                .filter(dsf -> dsf !=null)
                .collect(Collectors.toSet());


        /*
         * It is quite important to do not mess fields order and preserve original Requested Fields order.
         */
        final List<DSField> effectiveFields = new ArrayList<>(getRequestedFields().size() + extraFields.size());
        effectiveFields.addAll(getRequestedFields());
        effectiveFields.addAll(extraFields);

        final String selectClause = effectiveFields
                .stream()
                .map(this::formatFieldNameForSqlSelectClause)
                .collect(Collectors.joining(",\n  "));


        // -- FROM
        final String fromClause = dataSource().getTableName();

        // -- JOIN ON
        final List<RelationSupport.ForeignKeyRelation> foreignKeyRelations = dsHandler().getFields()
                .stream()
                .filter(dsf -> dsf.isIncludeField()
                        /*
                         * Entities will be handled separately via sub-entity fetch request,
                         * therefore exclude this field from the sql join.
                         */
                        && !DSField.FieldType.ENTITY.equals(dsf.getType())

                        /**
                         * Multiple record inclusion will be handled via a separate subquery
                         * in SELECT clause,  utilizing  the includeSummaryFunction mechanism
                         */
                        // TODO: consider checking source field from effective relation as well
                        && !dsf.isMultiple()
                )
                .map(dsf -> {
                    final RelationSupport.ImportFromRelation relation = dsHandler().describeImportFrom(dsf);
                    return relation.foreignKeyRelations();
                })
                .flatMap( Collection::stream /*fkrls -> fkrls.stream()*/ )

                /* It is required to generate one join per unique ForeignKeyRelation value */
                .filter(new Predicate<>() {
                    final List<RelationSupport.ForeignKeyRelation> unique = new LinkedList<>();

                    @Override
                    public boolean test(RelationSupport.ForeignKeyRelation fkrl) {

                        for (RelationSupport.ForeignKeyRelation f :unique) {
                            if (
                                    f.dataSource().equals(fkrl.dataSource())
                                    && f.sourceField().equals(fkrl.sourceField())
                                    && f.foreign().dataSource().equals(fkrl.foreign().dataSource())
                                    && f.foreign().field().equals(fkrl.foreign().field())
                            ) {
                                return false;
                            }
                        }
                        unique.add(fkrl);
                        return true;
                    }
                })
                .collect(Collectors.toList());

        final String joinClause = JDBCHandler.AbstractSQLContext.generateSQLJoin(foreignKeyRelations);


        // -- WHERE
        final Predicate<String> exclusionPredicate = createCriteriaExclusionPredicate(
                operationBinding() != null? operationBinding().getExcludeCriteriaFields() : null);

        this.filterData = dsHandler().generateFilterData(DSRequest.OperationType.FETCH,
                request().getTextMatchStyle(),
                request().getData(),
                exclusionPredicate
            );


        final String whereClause = this.getFilterData().isEmpty() ?  "" : this.getFilterData().stream()
                .map(fd -> fd.sql("opaque"))
                .collect(Collectors.joining("\n\t\t AND "));

        // -- generate query
        {
            this.templateContext = SQLTemplateEngine.createContext(request(), selectClause, fromClause, joinClause, whereClause, "");

            templateContext.put("effectiveSelectClause", selectClause);

            final String effectiveFROM = operationBinding() == null
                    || operationBinding().getTableClause() == null
                    || operationBinding().getTableClause().isBlank()
                    ? fromClause : operationBinding().getTableClause();
            templateContext.put("effectiveTableClause", effectiveFROM);

            final String effectiveWhere = operationBinding() == null
                    || operationBinding().getWhereClause() == null
                    || operationBinding().getWhereClause().isBlank()
                    ? whereClause : operationBinding().getWhereClause();
            templateContext.put("effectiveWhereClause", effectiveWhere);


            final String effectiveJoin = operationBinding() == null
                    || operationBinding().getAnsiJoinClause() == null
                    || operationBinding().getAnsiJoinClause().isBlank()
                    ? joinClause : operationBinding().getAnsiJoinClause();
            templateContext.put("effectiveAnsiJoinClause", effectiveJoin);


            final String defaultQuery = """
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
                    """;

            final String effectiveQuery = operationBinding() == null
                    || operationBinding().getCustomSQL() == null
                    || operationBinding().getCustomSQL().isBlank()
                    ? defaultQuery : """
                    (
                        %s
                    ) a
                    """.formatted(operationBinding().getCustomSQL());

            this.genericQuery = SQLTemplateEngine.processSQL(templateContext, effectiveQuery);

            /*
             *  It seems that FreeMarker does not support recursive interpolations, therefore, as temporary workaround,
             *  it is required to re-process  Query to do interpolations for the placeholders that
             *  was introduced during the first interpolation.
             */
            this.genericQuery = SQLTemplateEngine.processSQL(templateContext,this.genericQuery);
        }
    }
}

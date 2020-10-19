package org.srg.smartclient;

import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.OperationBinding;

import java.io.IOException;
import java.util.*;
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
                                        throw new RuntimeException("Data source '%s': nothing known about field '%s' listed in order by clause."
                                                .formatted(dataSource().getId(), s));
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
                        throw new RuntimeException("%s: nothing known about requested field '%s', data source: %s."
                                .formatted(getClass().getSimpleName(), fn, dataSource().getId()));
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
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', valid format is 'localFieldName!relatedDataSourceID.relatedDataSourceFieldName'."
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    )
                            );
                        }

                        final String sourceFieldName = parsed[0].trim();

                        final DSField sourceField = dsHandler().getField(sourceFieldName);
                        if (sourceField == null) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', nothing known about field '%s'."
                                    .formatted(
                                            dataSource().getId(),
                                            descr,
                                            sourceFieldName
                                    )
                            );
                        }

                        final RelationSupport.ForeignKeyRelation fkRelation;

                        try {
                            fkRelation = dsHandler().describeForeignKey(sourceField);
                        } catch (Throwable t) {
                            throw new RuntimeException("Data source '%s': Invalid additionalOutputs value '%s', "
                                    .formatted(
                                            dataSource().getId(),
                                            descr
                                    ),
                                    t
                            );
                        }

                        final RelationSupport.ForeignRelation fRelation;

                        try {
                            fRelation = dsHandler().describeForeignRelation(parsed[1].trim());
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

            this.additionalOutputs.putAll(additionalOutputs);
        }

        final String selectClause = this.getRequestedFields()
                .stream()
                .map(this::formatFieldNameForSqlSelectClause)
                .collect(Collectors.joining(",\n  "));


        // -- FROM
        final String fromClause = dataSource().getTableName();

        // -- JOIN ON
        final String joinClause = dsHandler().getFields()
                .stream()
                .filter(dsf -> dsf.isIncludeField()
                        /*
                         * Entities will be fetched separately via sub-entity fetch request,
                         * therefore exclude this field from the sql join.
                         */
                        && !DSField.FieldType.ENTITY.equals(dsf.getType()))
                .map(dsf -> {

                    final RelationSupport.ImportFromRelation relation = dsHandler().describeImportFrom(dsf);

                    return " JOIN %s ON %s.%s = %s.%s"
                            .formatted(
                                    relation.foreignDataSource().getTableName(),
                                    dataSource().getTableName(), relation.sourceField().getDbName(),
                                    relation.foreignDataSource().getTableName(), relation.foreignKey().getDbName()
                            );
                })
                .collect(Collectors.joining(" \n "));


        // -- WHERE
        this.filterData = dsHandler().generateFilterData(DSRequest.OperationType.FETCH, request().getTextMatchStyle(), request().getData());


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

            this.genericQuery = SQLTemplateEngine.processSQL(templateContext,"""
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
    }
}

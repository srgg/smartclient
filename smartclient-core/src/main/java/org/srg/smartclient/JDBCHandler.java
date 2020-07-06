package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.*;

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

    private  Logger logger = LoggerFactory.getLogger(getClass());

    private final JDBCPolicy policy;

    public JDBCHandler(JDBCPolicy jdbcPolicy, IDSRegistry dsRegistry, DataSource datasource) {
        super(dsRegistry, datasource);
        policy = jdbcPolicy;
    }

    protected  String formatFieldName(DSField dsf) {
        if (DSField.FieldType.ENTITY.equals(dsf.getType())) {
            /**
             * Correspondent entity will be fetched by the subsequent query,
             * therefore it is required to reserve space in the response
             */
            return "NULL as \"%s.%s\"".formatted(
                    getDataSource().getTableName(),
                    dsf.getDbName()
            );
        } else if (dsf.isIncludeField()){
            final ImportFromRelation relation = describeImportFrom(dsf);
            return "%s.%s"
                    .formatted(
                            relation.foreignDataSource().getTableName(),
                            relation.foreignDisplay().getDbName()
                    );
        } else {
                return "%s.%s".formatted(
                        getDataSource().getTableName(),
                        dsf.getDbName()
                );
        }
    }

    protected DSResponse handleFetch(DSRequest request) throws Exception {
        final int pageSize = request.getEndRow() == -1  ? -1 : request.getEndRow() - request.getStartRow();

        // -- LIMIT
        final String paginationClause =  pageSize <= 0 ? "" : String.format("LIMIT %d OFFSET %d",
                request.getEndRow(),  request.getStartRow());

        // -- SELECT

        // -- filter requested columns if any provided, or stick with all
        final List<DSField> requestedFields;

        if (request.getOutputs() == null || request.getOutputs().isBlank()) {
            requestedFields = getDataSource().getFields();
        } else {
            requestedFields = request.getOutputs() == null ? null : Stream.of(request.getOutputs().split(","))
                    .map(str -> str.trim().toLowerCase())
                    .filter(s -> !s.isEmpty() && !s.isBlank())
                    .map( fn -> {
                        final DSField dsf = getField( fn);

                        if (dsf == null) {
                            throw new RuntimeException("%s: nothing known about requested field '%s', data soure: %s."
                                    .formatted(getClass().getSimpleName(), fn, getDataSource().getId()));
                        }

                        return dsf;
                    } )
                    .collect(Collectors.toList());
        }

        final String selectClause = String.format("SELECT %s",
                String.join(",\n  " ,
                    requestedFields
                        .stream()
                            .map( dsf -> {
                                // If a custom SQL snippet is provided for column -- use it
                                if (dsf.isCustomSQL()
                                        && dsf.getCustomSelectExpression() != null
                                        && !dsf.getCustomSelectExpression().isBlank()) {
                                    return "%s AS \"%s\""
                                            .formatted(
                                                    dsf.getCustomSelectExpression(),
                                                    dsf.getDbName()
                                            );
                                } else {
                                    return formatFieldName(dsf);
                                }
                            })
                            .collect(Collectors.toList())
                )
        );

        // -- FROM
        final String fromClause = String.format("FROM %s", getDataSource().getTableName());

        // -- JOIN ON
        final String joinClause = String.join(" \n ",
                getFields()
                    .stream()
                    .filter( dsf -> dsf.isIncludeField())
                    .map( dsf -> {

                        final ImportFromRelation relation = describeImportFrom(dsf);

                        return " JOIN %s ON %s.%s = %s.%s"
                                .formatted(
                                        relation.foreignDataSource().getTableName(),
                                        this.getDataSource().getTableName(), relation.sourceField().getDbName(),
                                        relation.foreignDataSource().getTableName(), relation.foreignKey().getDbName()
                                );
                    })
                    .collect(Collectors.toList())
        );


        // -- ORDER BY
        final String orderClause = request.getSortBy() == null ? "" : "ORDER BY \n" +
                request.getSortBy().stream()
                .map(s -> {
                    String order = "";
                    switch (s.charAt(0)) {
                        case '-':
                            order = " DESC";
                        case '+':
                            s = s.substring(1);
                        default:
                            return "%s.%s%s"
                                    .formatted(
                                        getDataSource().getTableName(),
                                        getField(s).getDbName(),
                                        order
                                    );
                    }
                })
                .collect(Collectors.joining(", "));

        // -- WHERE
        final List<IFilterData> filterData  = generateFilterData(request.getTextMatchStyle(), request.getData());

        final String whereClause = filterData.isEmpty() ?  "" : " \n\tWHERE \n\t\t" +
                String.join("\n\t\t AND ",
                        filterData.stream()
                                .map(fd -> fd.sql())
                                .collect(Collectors.toList())
                );

        final List<Object[]> data;
        if (pageSize > 0) {
            data = new ArrayList<>(pageSize);
        } else {
            data = new LinkedList<>();
        }

        final int totalRows[] = new int[] {-1};

        policy.withConnectionDo(this.getDataSource().getDbName(), conn-> {
            // -- calculate total
            final String countQuery = String.join("\n ", "SELECT count(*)", fromClause, joinClause, whereClause);

            if (logger.isDebugEnabled()) {
                logger.debug("DataSource %s fetch count(*) query:\n%s\n\nparams:\n%s"
                        .formatted(
                                getDataSource().getId(),
                                countQuery,
                                filterData.stream()
                                        .flatMap(fd -> StreamSupport.stream(fd.values().spliterator(), false))
                                        .map(d-> "%s".formatted(d))
                                        .collect(Collectors.joining(", "))
                        )
                );
            }

            try (PreparedStatement st = conn.prepareStatement(countQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                int idx =0;

                for (IFilterData fd: filterData) {
                    idx = fd.setStatementParameters(idx, st);
                }

                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    totalRows[0] = rs.getInt(1);
                }
            }

            // -- fetch data
            final String query = String.join("\n ", Arrays.asList(selectClause, fromClause, joinClause, whereClause, orderClause, paginationClause ));

            if (logger.isDebugEnabled()) {
                logger.debug("DataSource %s fetch query:\n%s\n\nparams:\n%s"
                    .formatted(
                        getDataSource().getId(),
                        query,
                            filterData.stream()
                                .flatMap(fd -> StreamSupport.stream(filterData.spliterator(), false))
                                .map(d-> "%s".formatted(d))
                                .collect(Collectors.joining(", "))
                    )
                );
            }

            try(PreparedStatement st = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)){
                st.setFetchSize(pageSize);
                st.setMaxRows(pageSize);
                st.setFetchDirection(ResultSet.FETCH_FORWARD);

                int idx =0;
                for (IFilterData fd: filterData) {
                    idx = fd.setStatementParameters(idx, st);
                }

                final Map<String, Object> pkValues = new HashMap<>();

                try (ResultSet rs = st.executeQuery() ) {
                    while (rs.next())  {

                        final Object[] r = new Object[getFields().size()];

                        int i =0;

                        // ORIGINAL FIELD ORDER MUST BE USED
                        for (DSField dsf: requestedFields) {
                            Object v = rs.getObject(i + 1);
                            if (rs.wasNull()) {
                                v = null;
                            }
                            r[i++] = v;

                            if (dsf.isPrimaryKey()) {
                                pkValues.put(dsf.getName(), v);
                            }
                        }

                        for(int j=0; j<requestedFields.size(); ++j) {
                            final DSField dsf = requestedFields.get(j);

                            if (DSField.FieldType.ENTITY.equals(dsf.getType())) {
                                final ForeignKeyRelation foreignKeyRelation = describeForeignKey(dsf);

                                final DSResponse response;
                                try {
                                    response = fetchForeignEntityById(foreignKeyRelation, pkValues);
                                    assert response != null;
                                } catch ( Throwable t) {
                                    throw new RuntimeException("Subsequent entity fetch failed: %s, filters: %s"
                                            .formatted(
                                                    foreignKeyRelation,
                                                    pkValues.entrySet().stream()
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
                                                    pkValues.entrySet().stream()
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

                                r[j] = response.getData().getRawDataResponse();
                                assert r[j] != null;
                            }
                        }

                        pkValues.clear();

                        data.add(r);
                    }
                    return null;
                }
            }
        });

        return DSResponse.success(request.getStartRow(), request.getStartRow() + data.size(), totalRows[0],
                requestedFields,
                data);
    }

    protected DSResponse fetchForeignEntityById(ForeignKeyRelation foreignKeyRelation, Map<String, Object> filtersAndKeys) throws Exception {
        logger.trace("Feet");
//        if (filtersAndKeys.size() > 1) {
//            throw new IllegalStateException("Fetch Entity does not supports composite keys");
//        }

        final DSHandler dsHandler = this.getDataSourceHandlerById(foreignKeyRelation.foreign().dataSourceId());

        final DSRequest fetchEntity = new DSRequest();
        fetchEntity.setDataSource(dsHandler.id());
        fetchEntity.setOperationType(DSRequest.OperationType.FETCH);
        fetchEntity.wrapAndSetData(Map.of(foreignKeyRelation.foreign().fieldName(), filtersAndKeys.values().iterator().next()));

        final DSResponse response = dsHandler.handle(fetchEntity);
        return response;
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

                        final Object value = switch (dsf.getType()) {
                            case TEXT -> switch (textMatchStyle) {
                                case EXACT -> e.getValue();

                                case SUBSTRING -> "%%%s%%".formatted(e.getValue());

                                default -> "%s%%".formatted(e.getValue());
                            };
                            default -> e.getValue();
                        };


                        String filterStr = switch (dsf.getType()) {
                            case TEXT -> "%s like ?";
                            default -> "%s = ?";
                        };

                        filterStr = filterStr.formatted(formatFieldName(dsf));

                        return new FilterData(dsf, filterStr, value);
                    })
                    .collect(Collectors.toList());
        } else if (data == null){
            return Collections.EMPTY_LIST;
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
        Iterable<Object> values();

        default int setStatementParameters(int idx, PreparedStatement preparedStatement) throws SQLException {
            for (Object v : values()) {
                preparedStatement.setObject(++idx, v);
            }
            return idx;
        }

    }

    protected static class FilterData implements IFilterData{
        private final DSField field;
        private final String sql;
        private final Object value;

        public FilterData(DSField field, String sql, Object value) {
            this.field = field;
            this.sql = sql;
            this.value = value;
        }

        public FilterData(DSField field, String sql, Object... values) {
            this.field = field;
            this.sql = sql;
            this.value = values;
        }

        @Override
        public String sql() {
            return sql;
        }

        public DSField field() {
            return field;
        }

        public Iterable<Object> values() {
            return Collections.singletonList(value);
        }
    }
}

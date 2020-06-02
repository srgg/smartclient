package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public class JDBCHandler extends AbstractDSHandler {
    public interface JDBCPolicy {
        // http://java.avdiel.com/Tutorials/JDBCPaging.html
        void withConnectionDo(String database, Utils.CheckedFunction<Connection, Void> callback) throws Exception;
    }

    private static Logger logger = LoggerFactory.getLogger(JDBCHandler.class);

    private final JDBCPolicy policy;

    public JDBCHandler(JDBCPolicy jdbcPolicy, IDSDispatcher dispatcher, DataSource datasource) {
        super(dispatcher, datasource);
        policy = jdbcPolicy;
    }

    protected  String formatFieldName(DSField dsf) {
        if (dsf.isIncludeField()){
            final ImportFromRelation relation = describeImportFrom(dsf);
            return "%s.%s"
                    .formatted(
                            relation.foreignDataSource().getTableName(),
                            relation.foreignDisplay().getDbName()
                    );
        } else {
            return "%s.%s".formatted(
                    getDatasource().getTableName(),
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
        final String selectClause = String.format("SELECT %s",
                String.join(",\n  " ,
                    getFields()
                        .stream()
                        .map( dsf -> formatFieldName(dsf))
                        .collect(Collectors.toList())
                )
        );

        // -- FROM
        final String fromClause = String.format("FROM %s", getDatasource().getTableName());

        // -- JOIN ON
        final String joinClause = String.join(",\n ",
                getFields()
                    .stream()
                    .filter( dsf -> dsf.isIncludeField())
                    .map( dsf -> {

                        final ImportFromRelation relation = describeImportFrom(dsf);

                        return " JOIN %s ON %s.%s = %s.%s"
                                .formatted(
                                        relation.foreignDataSource().getTableName(),
                                        this.getDatasource().getTableName(), relation.sourceField().getDbName(),
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
                                        getDatasource().getTableName(),
                                        getField(s).getDbName(),
                                        order
                                    );
                    }
                })
                .collect(Collectors.joining(", "));

        // -- WHERE
        final List<FilterData> filterData  = request.getData() == null ? Collections.EMPTY_LIST :
                ((Map<String,Object>)request.getData()).entrySet()
                    .stream()
                    .map(e -> {
                        final DSField dsf = getField(e.getKey());

                        if (dsf == null) {
                            throw new RuntimeException("DataSource '%s': nothing known about field '%s'"
                                    .formatted(
                                            request.getDataSource(),
                                            e.getKey()
                                    )
                            );
                        }

                        final Object value = switch (dsf.getType()) {
                            case TEXT ->
                                    switch ( request.getTextMatchStyle()) {
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

        final String whereClause = filterData.isEmpty() ?  "" : " \n\tWHERE \n\t\t" +
                String.join("\n\t\t AND ",
                        filterData.stream()
                                .map(fd -> fd.sqlFilter)
                                .collect(Collectors.toList())
                );

        final List<Object[]> data;
        if (pageSize > 0) {
            data = new ArrayList<>(pageSize);
        } else {
            data = new LinkedList<>();
        }

        final int totalRows[] = new int[] {-1};

        policy.withConnectionDo(this.getDatasource().getDbName(), connn-> {
            // -- calculate total
            final String countQuery = String.join("\n ", "SELECT count(*)", fromClause, joinClause, whereClause);

            if (logger.isDebugEnabled()) {
                logger.debug("DataSource %s fetch count(*) query:\n%s"
                        .formatted(
                                getDatasource().getId(),
                                countQuery
                        )
                );
            }

            try (PreparedStatement st = connn.prepareStatement(countQuery, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                int idx =0;
                for (FilterData fd: filterData) {
                    st.setObject(++idx, fd.value());
                }

                //applyParameters(st, params);

                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    totalRows[0] = rs.getInt(1);
                }
            }

            // -- fetch data
            final String query = String.join("\n ", Arrays.asList(selectClause, fromClause, joinClause, whereClause, orderClause, paginationClause ));

            if (logger.isDebugEnabled()) {
                logger.debug("DataSource %s fetch query:\n%s"
                    .formatted(
                        getDatasource().getId(),
                        query
                    )
                );
            }

            try(PreparedStatement st = connn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)){
                st.setFetchSize(pageSize);
                st.setMaxRows(pageSize);
                st.setFetchDirection(ResultSet.FETCH_FORWARD);

                int idx =0;
                for (FilterData fd: filterData) {
                    st.setObject(++idx, fd.value());
                }

                try (ResultSet rs = st.executeQuery() ) {
                    while (rs.next())  {
                        final Object[] r = new Object[getFields().size()];

                        int i =0;
                        // ORIGINAL FIELD ORDER MUST BE USED
                        for (DSField dsf: getDatasource().getFields()) {
                            Object v = rs.getObject(i + 1);
                            if (rs.wasNull()) {
                                v = null;
                            }
                            r[i++] = v;
                        }

                        data.add(r);
                    }
                    return null;
                }
            }
        });

        return DSResponse.success(request.getStartRow(), request.getStartRow() + data.size(), totalRows[0],
                dataSource().getFields(),
                data);
    }

    protected record FilterData(DSField field, String sqlFilter, Object value) {}
}

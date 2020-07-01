package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.isomorphic.IDSRequestData;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;
import org.srg.smartclient.isomorphic.criteria.Criteria;

import java.util.*;
import java.util.stream.Collectors;

/**
 * https://isomorphic.atlassian.net/wiki/spaces/Main/pages/525155/5.+Adding+support+for+AdvancedCriteria
 * https://stackoverrun.com/ru/q/5891230
 */
public class AdvancedJDBCHandler extends JDBCHandler {
    public AdvancedJDBCHandler(JDBCPolicy jdbcPolicy, IDSRegistry dsRegistry, DataSource datasource) {
        super(jdbcPolicy, dsRegistry, datasource);
    }

    @Override
    public boolean allowAdvancedCriteria() {
        return true;
    }

    private IFilterData generateFD(Criteria ac) {
        if (ac.getFieldName() != null && !ac.getFieldName().isBlank()) {

            if (ac.getCriteria() != null && !ac.getCriteria().isEmpty()) {
                throw new IllegalStateException(
                        "Hm, I was sure that this case is impossible, but if you goty this error, I need to rethink this"
                );
            }

            final DSField dsf = getField(ac.getFieldName());

            if (dsf == null) {
                throw new RuntimeException("DataSource '%s': nothing known about field '%s'"
                        .formatted(
                                getDataSource().getId(),
                                ac.getFieldName()
                        )
                );
            }

            String filterStr;
            Object values = null;
            boolean isNot = false;

            switch (ac.getOperator()) {
                case NOT_BLANK:
                case NOT_NULL:

                    filterStr = "%s IS NOT NULL";
                    break;

                case IS_BLANK:
                case IS_NULL:
                    filterStr = "%s IS NULL";
                    break;

                case INOT_CONTAINS:
                    isNot = true;

                case ENDS_WITH:
                case IENDS_WITH:
                case CONTAINS:
                case ICONTAINS:

                    final String pattern = switch (ac.getOperator()) {
                        case ENDS_WITH, IENDS_WITH -> "%%%s";
                        case CONTAINS, ICONTAINS,INOT_CONTAINS -> "%%%s%%";
                        default -> throw new IllegalStateException();
                    };

                    values = pattern.formatted(ac.getValue());

                    if (ac.getOperator().name().charAt(0) == 'I') {
                        // Case Insensitive

                        if (isNot) {
                            filterStr = "LOWER(%s) NOT LIKE LOWER(?)";
                        } else {
                            filterStr = "LOWER(%s) LIKE LOWER(?)";
                        }
                    } else {
                        if (isNot) {
                            filterStr = "%s NOT LIKE ?";
                        } else {
                            filterStr = "%s LIKE ?";
                        }
                    }
                    break;

                case NOT_EQUAL:
                    isNot = true;

                case EQUALS:

                    values = ac.getValue();

                    if (isNot) {
                        filterStr = "%s <> ?";
                    } else {
                        filterStr = "%s = ?";
                    }
                    break;

                default:
                    throw new RuntimeException("DataSource '%s': unsupported operator '%s'"
                            .formatted(
                                    getDataSource().getId(),
                                    ac.getOperator()
                            )
                    );
            }

            filterStr = filterStr.formatted(
                    formatFieldName(dsf)
            );


            return ValueFilterData.create(dsf, filterStr, values);
        } else {
            switch (ac.getOperator()) {
                case OR:
                case AND:
                    break;
                default:
                    throw new IllegalStateException("DataSource '%s': unsupported operator '%s'"
                            .formatted(
                                    getDataSource().getId(),
                                    ac.getOperator()
                            )
                    );
            }

            final List<IFilterData> fds = ac.getCriteria().stream()
                    .map(this::generateFD)
                    .collect(Collectors.toList());

            return new CompositeFilterData(ac.getOperator().name(), fds);
        }

    }


    @Override
    protected List<IFilterData> generateFilterData(DSRequest.TextMatchStyle textMatchStyle, IDSRequestData data) {
        if (data instanceof AdvancedCriteria ac) {
            return Collections.singletonList(generateFD(ac));

        } else {
            return super.generateFilterData(textMatchStyle, data);
        }
    }

    protected static class ValueFilterData extends FilterData {
        private final List<Object> values;

        public ValueFilterData(DSField field, String sql) {
            super(field, sql, (Object) null);
            //noinspection unchecked
            this.values = Collections.EMPTY_LIST;
        }

        public ValueFilterData(DSField field, String sql, Collection<?> values) {
            super(field, sql, (Object) null);
            this.values = new ArrayList<>(values);
        }

        public ValueFilterData(DSField field, String sql, Object value) {
            super(field, sql, (Object) null);
            this.values = Collections.singletonList(value);
        }

        public ValueFilterData(DSField field, String sql, Object... values) {
            super(field, sql, (Object) null);
            this.values = Arrays.asList(values);
        }

        @Override
        public Iterable<Object> values() {
            return values;
        }

        public static ValueFilterData create(DSField field, String sql, Object value) {
            if (value == null) {
                return new ValueFilterData(field, sql);
            } else if (value instanceof Collection) {
                return new ValueFilterData(field, sql, (Collection<?>)value);
            } else {
                return new ValueFilterData(field, sql, value);
            }
        }
    }

    protected class CompositeFilterData implements IFilterData, Iterable<Object> {
        private final String operator;
        private final List<IFilterData> filterDataList;

        public CompositeFilterData(String operator, List<IFilterData> filterDataList) {
            this.operator = operator;
            this.filterDataList = filterDataList;
        }


        @Override
        public String sql() {
            final String sql = "( %s )"
                    .formatted(
                            filterDataList.stream()
                                .map(IFilterData::sql)
                                .collect(Collectors.joining(" " + operator + " ")
                            )
                    );
            final StringBuilder sbld = new StringBuilder("(");

            return sql;
        }

        @Override
        public Iterable<Object> values() {
            return this;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<>() {
                final Iterator<IFilterData> filterDataIterator = CompositeFilterData.this.filterDataList.iterator();
                Iterator<Object> objectIterator = null;

                @Override
                public boolean hasNext() {
                    do {
                        if (objectIterator == null || !objectIterator.hasNext()) {
                            if (filterDataIterator.hasNext()) {
                                final IFilterData fd = filterDataIterator.next();
                                objectIterator = fd.values().iterator();
                            } else {
                                return false;
                            }
                        }
                    } while (!objectIterator.hasNext());

                    return true;//objectIterator.hasNext();
                }

                @Override
                public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    assert objectIterator != null;
                    return objectIterator.next();
                }
            };
        }
    }
}
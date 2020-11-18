package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.isomorphic.IDSRequestData;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;
import org.srg.smartclient.isomorphic.criteria.Criteria;

import java.util.*;
import java.util.function.Predicate;
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

    private IFilterData generateFD(Criteria ac, Predicate<String> exclusionPredicate ) {
        if (ac.getFieldName() != null && !ac.getFieldName().isBlank()) {
            if (exclusionPredicate.test(ac.getFieldName())){
                return null;
            }

            if (ac.getCriteria() != null && !ac.getCriteria().isEmpty()) {
                throw new IllegalStateException(
                        "Hm, I was sure that this case is impossible, but if you got this error, I need to rethink this"
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

                case NOT_STARTS_WITH:
                case INOT_STARTS_WITH:

                case NOT_ENDS_WITH:
                case INOT_ENDS_WITH:

                case NOT_CONTAINS:
                case INOT_CONTAINS:
                    isNot = true;

                case ENDS_WITH:
                case IENDS_WITH:

                case CONTAINS:
                case ICONTAINS:

                case STARTS_WITH:
                case ISTARTS_WITH:

                    final String pattern = switch (ac.getOperator()) {
                        case ENDS_WITH, IENDS_WITH, NOT_ENDS_WITH, INOT_ENDS_WITH-> "%%%s";
                        case CONTAINS, ICONTAINS, NOT_CONTAINS, INOT_CONTAINS -> "%%%s%%";
                        case STARTS_WITH, ISTARTS_WITH, NOT_STARTS_WITH, INOT_STARTS_WITH -> "%s%%";
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

                case INOT_EQUAL:
                case NOT_EQUAL:
                    isNot = true;

                case IEQUALS:
                case EQUALS:

                    values = ac.getValue();

                    if (ac.getOperator().name().charAt(0) == 'I') {
                        // Case Insensitive
                        if (isNot) {
                            filterStr = "LOWER(%s) <> LOWER(?)";
                        } else {
                            filterStr = "LOWER(%s) = LOWER(?)";
                        }
                    } else {
                        // Case Sensitive
                        if (isNot) {
                            filterStr = "%s <> ?";
                        } else {
                            filterStr = "%s = ?";
                        }
                    }
                    break;

                case BETWEEN:
                    /*
                     * SQL between is inclusive, therefore it can not be used for non inclusive conditions.
                     */
                    values = Arrays.asList(ac.getStart(), ac.getEnd());
                    filterStr = "(%1$s > ? AND %1$s < ?)";
                    break;

                case BETWEEN_INCLUSIVE:
                    values = Arrays.asList(ac.getStart(), ac.getEnd());
                    filterStr = "%s BETWEEN ? AND ?";
                    break;

                case GREATER_OR_EQUAL:
                    values = ac.getValue();
                    filterStr = "%s >= ?";
                    break;

                case LESS_OR_EQUAL:
                    values = ac.getValue();
                    filterStr = "%s <= ?";
                    break;

                default:
                    throw new RuntimeException("DataSource '%s': unsupported operator '%s'"
                            .formatted(
                                    getDataSource().getId(),
                                    ac.getOperator()
                            )
                    );
            }

            ForeignRelation effectiveField = determineEffectiveField(dsf);

            effectiveField = effectiveField.createWithSqlFieldAlias(
                formatColumnNameToAvoidAnyPotentialDuplication(
                    effectiveField.dataSource(),
                    effectiveField.field()
                )
            );

            return ValueFilterData.create(effectiveField, filterStr, values);
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
                    .map(criteria -> this.generateFD(criteria, exclusionPredicate))
                    .filter( fd -> fd != null)
                    .collect(Collectors.toList());
            if (fds.isEmpty()) {
                return null;
            } else {
                return new CompositeFilterData(ac.getOperator().name(), fds);
            }
        }
    }


    @Override
    protected List<IFilterData> generateFilterData(
            DSRequest.OperationType operationType,
            DSRequest.TextMatchStyle textMatchStyle,
            IDSRequestData data,
            Predicate<String> exclusionPredicate) {
        if (data instanceof AdvancedCriteria ac) {
            final IFilterData fd = generateFD(ac, exclusionPredicate);

            if (fd == null) {
                return Collections.EMPTY_LIST;
            } else {
                return Collections.singletonList(fd);
            }
        } else {
            return super.generateFilterData(operationType, textMatchStyle, data, exclusionPredicate);
        }
    }

    protected static class ValueFilterData extends FilterData {
        private final List<Object> values;


        public ValueFilterData(ForeignRelation dsFieldPair, String sql) {
            super(dsFieldPair, sql, (Object) null);

            //noinspection unchecked
            this.values = Collections.EMPTY_LIST;
        }

        public ValueFilterData(ForeignRelation dsFieldPair, String sql, Collection<?> values) {
            super(dsFieldPair, sql, (Object) null);
            this.values = new ArrayList<>(values);
        }

        public ValueFilterData(ForeignRelation dsFieldPair, String sql, Object value) {
            super(dsFieldPair, sql, (Object) null);
            this.values = Collections.singletonList(value);
        }

        public ValueFilterData(ForeignRelation dsFieldPair, String sql, Object... values) {
            super(dsFieldPair, sql, (Object) null);
            this.values = Arrays.asList(values);
        }

        @Override
        public Iterable<Object> values() {
            return values;
        }

        public static ValueFilterData create(ForeignRelation dsFieldPair, String sql, Object value) {
            if (value == null) {
                return new ValueFilterData(dsFieldPair, sql);
            } else if (value instanceof Collection) {
                return new ValueFilterData(dsFieldPair, sql, (Collection<?>)value);
            } else {
                return new ValueFilterData(dsFieldPair, sql, value);
            }
        }
    }

    protected class CompositeFilterData implements IFilterData, Iterable<Object> {
        private final String operator;
        private final List<IFilterData> filterDataList;
        private transient String formattedSql;

        public CompositeFilterData(String operator, List<IFilterData> filterDataList) {
            this.operator = operator;
            this.filterDataList = filterDataList;
        }


        @Override
        public String sql() {
            if (formattedSql == null) {
                formattedSql = "( %s )"
                    .formatted(
                        filterDataList.stream()
                            .map(IFilterData::sql)
                            .collect(Collectors.joining(" " + operator + " ")
                            )
                    );
            }

            return formattedSql;
        }

        @Override
        public String sql(String aliasOrTable) {
            final String effectiveSql = "( %s )"
                .formatted(
                    filterDataList.stream()
                        .map( fd -> fd.sql(aliasOrTable))
                        .collect(Collectors.joining(" " + operator + " "))
                );

            return effectiveSql;
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

        @Override
        public String toString() {
            return "CompositeFilterData{" +
                    "operator='" + operator + '\'' +
                    ", filterDataList=" + filterDataList +
                    ", formattedSql='" + formattedSql + '\'' +
                    '}';
        }
    }
}

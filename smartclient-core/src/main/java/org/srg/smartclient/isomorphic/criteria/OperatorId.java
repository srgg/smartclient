package org.srg.smartclient.isomorphic.criteria;

import com.fasterxml.jackson.annotation.JsonValue;

// https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=type..OperatorId
public enum OperatorId {
    NOT_BLANK("notBlank"),

    /**
     * all subcriteria (criterion.criteria) are true
     */
    AND("and");

    private String jsonValue;

    OperatorId(String jsonValue){
        this.jsonValue = jsonValue;
    }

    @JsonValue
    final String jsonValue() {
        return this.jsonValue;
    }

//
//    /**
//     * shortcut for "greaterThan" + "lessThan" + "and".
//     */
//    BETWEEN,
//
//    /**
//     * shortcut for "greaterOrEqual" + "lessOrEqual" + "and".
//     */
//    BETWEEN_INCLUSIVE,
//
//    /**
//     * Contains as sub-string (match case)
//     */
//    CONTAINS,
//
//    /**
//     * Contains as sub-string (match case) another field value (specify fieldName as criterion.value)
//     */
//    CONTAINS_FIELD,
//
//    /**
//     *  Ends with (match case)
//     */
//    ENDS_WITH,
//
//    /**
//     * Ends with (match case) another field value (specify fieldName as criterion.value)
//     */
//    ENDS_WITH_FIELD,
//
//    /**
//     * exactly equal to
//     */
//    EQUALS,
//
//    /**
//     * matches another field (match case, specify fieldName as criterion.value)
//      */
//    EQUALS_FIELD,
//    /**
//     * Greater than or equal to
//     */
//    GREATER_OR_EQUAL,
//    /**
//     * Greater than or equal to another field (specify fieldName as criterion.value)
//     */
//    GREATER_OR_EQUAL_FIELD,
//
//    /**
//     *  Greater than
//     */
//    GREATER_THAN,
//
//    /**
//     * Greater than another field (specify fieldName as criterion.value)
//     */
//    GREATER_THAN_FIELD,
//
//    /**
//     * shortcut for "greaterOrEqual" + "and" + "lessOrEqual" (case insensitive)
//     */
//    IBETWEEN_INCLUSIVE,
//
//    /**
//     * Contains as sub-string (case insensitive)
//     */
//    ICONTAINS,
//
//    /**
//     * Contains as sub-string (case insensitive) another field value (specify fieldName as criterion.value)
//      */
//    ICONTAINS_FIELD,
//
//    /**
//     * Ends with (case insensitive)
//      */
//    IENDS_WITH,
//
//    /**
//     * Ends with (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    IENDS_WITH_FIELD,
//
//    /**
//     * exactly equal to, if case is disregarded
//     */
//    IEQUALS,
//
//    /**
//     * matches another field (case insensitive, specify fieldName as criterion.value)
//     */
//    IEQUALS_FIELD,
//
//    /**
//     * value is in a set of values.
//     */
//    IN_SET,
//
//    /**
//     * Does not contain as sub-string (case insensitive)
//     */
//    INOT_CONTAINS,
//
//    /**
//     * Does not contain as sub-string (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    INOT_CONTAINS_FIELD,
//
//    /**
//     * Does not end with (case insensitive)
//     */
//    INOT_ENDS_WITH,
//
//    /**
//     * Does not end with (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    INOT_ENDS_WITH_FIELD,
//
//    /**
//     * not equal to, if case is disregarded
//     */
//    INOT_EQUAL,
//
//    /**
//     * does not match another field (case insensitive, specify fieldName as criterion.value)
//     */
//    INOT_EQUAL_FIELD,
//
//    /**
//     * Does not start with (case insensitive)
//     */
//    INOT_STARTS_WITH,
//
//    /**
//     * Does not start with (case insensitive) another field value (specify fieldName as criterion.value)
//      */
//    INOT_STARTS_WITH_FIELD,
//
//    /**
//     * Regular expression match (case insensitive)
//     */
//    IREGEXP,
//
//    /**
//     * value is null
//     */
//    IS_NULL,
//
//    /**
//     * Starts with (case insensitive)
//     */
//    ISTARTS_WITH,
//
//    /**
//     * Starts with (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    ISTARTS_WITH_FIELD,
//
//    /**
//     * Less than or equal to
//     */
//    LESS_OR_EQUAL,
//
//    /**
//     * Less than or equal to another field (specify fieldName as criterion.value)
//     */
//    LESS_OR_EQUAL_FIELD,
//
//    /**
//     * Less than
//     */
//    LESS_THAN,
//
//    /**
//     * Less than another field (specify fieldName as criterion.value)
//     */
//    LESS_THAN_FIELD,
//
//
//    /**
//     * all subcriteria (criterion.criteria) are false
//     */
//    NOT,
//
//    /**
//     * Does not contain as sub-string (match case)
//     */
//    NOT_CONTAINS,
//
//    /**
//     * Does not contain as sub-string (match case) another field value (specify fieldName as criterion.value)
//     */
//    NOT_CONTAINS_FIELD,
//
//    /**
//     * Does not end with (match case)
//     */
//    NOT_ENDS_WITH,
//
//    /**
//     * Does not end with (match case) another field value (specify fieldName as criterion.value)
//     */
//    NOT_ENDS_WITH_FIELD,
//
//    /**
//     * not equal to
//     */
//    NOT_EQUAL,
//
//    /**
//     * does not match another field (match case, specify fieldName as criterion.value)
//     */
//    NOT_EQUAL_FIELD,
//
//    /**
//     * value is not in a set of values.
//     */
//    NOT_IN_SET,
//
//    /**
//     * value is non-null.
//     */
//    NOT_NULL,
//
//    /**
//     * Does not start with (match case)
//     */
//    NOT_STARTS_WITH,
//
//    /**
//     * Does not start with (match case) another field value (specify fieldName as criterion.value)
//     */
//    NOT_STARTS_WITH_FIELD,
//
//    /**
//     * at least one subcriteria (criterion.criteria) is true
//     */
//    OR,
//
//    /**
//     * Regular expression match
//     */
//    REGEXP,
//
//    /**
//     * Starts with (match case)
//     */
//    STARTS_WITH,
//
//
//    /**
//     * Starts with (match case) another field value (specify fieldName as criterion.value)
//     */
//    STARTS_WITH_FIELD
}

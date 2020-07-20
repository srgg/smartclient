package org.srg.smartclient.isomorphic.criteria;

import com.fasterxml.jackson.annotation.JsonValue;

// https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=type..OperatorId
public enum OperatorId {
    IS_BLANK("isBlank"),
    NOT_BLANK("notBlank"),

    /**
     * all subcriteria (criterion.criteria) are true
     */
    AND("and"),

    /**
     *  (criterion.criteria) are false
     */
    NOT("not"),

    /**
     * at least one subcriteria (criterion.criteria) is true
     */
    OR("or"),


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
    /**
     * Contains as sub-string (match case)
     */
    CONTAINS("contains"),
//
//    /**
//     * Contains as sub-string (match case) another field value (specify fieldName as criterion.value)
//     */
//    CONTAINS_FIELD,
//
    /**
     *  Ends with (match case)
     */
    ENDS_WITH("endsWith"),

    /**
     * Does not end with (match case)
     */
    NOT_ENDS_WITH("notEndsWith"),


    /**
     *  Ends with (case insensitive)
     */
    IENDS_WITH("iEndsWith"),

    /**
     * Does not end with (case insensitive)
     */
    INOT_ENDS_WITH("iNotEndsWith"),

//
//    /**
//     * Ends with (match case) another field value (specify fieldName as criterion.value)
//     */
//    ENDS_WITH_FIELD,
//
    /**
     * exactly equal to
     */
    EQUALS("equals"),

    NOT_EQUAL("notEqual"),

    /**
     * exactly equal to, if case is disregarded
     */
    IEQUALS("iEquals"),

    /**
     * not equal to, if case is disregarded
     */
    INOT_EQUAL("iNotEqual"),


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

    /**
     * Does not contain as sub-string (match case)
     */
    NOT_CONTAINS("notContains"),

    /**
     * Contains as sub-string (case insensitive)
     */
    ICONTAINS("iContains"),

    /**
     * Does not contain as sub-string (case insensitive)
     */
    INOT_CONTAINS("iNotContains"),

//    /**
//     * Contains as sub-string (case insensitive) another field value (specify fieldName as criterion.value)
//      */
//    ICONTAINS_FIELD,
//
//   /**
//     * Ends with (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    IENDS_WITH_FIELD,
//
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
//

//    /**
//     * Does not contain as sub-string (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    INOT_CONTAINS_FIELD,
//
//
//    /**
//     * Does not end with (case insensitive) another field value (specify fieldName as criterion.value)
//     */
//    INOT_ENDS_WITH_FIELD,
//
//
//    /**
//     * does not match another field (case insensitive, specify fieldName as criterion.value)
//     */
//    INOT_EQUAL_FIELD,
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
    /**
     * value is null
     */
    IS_NULL("isNull"),

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
//     * Does not contain as sub-string (match case) another field value (specify fieldName as criterion.value)
//     */
//    NOT_CONTAINS_FIELD,
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
    /**
     * value is non-null.
     */
    NOT_NULL("notNull"),

//
//    /**
//     * Does not start with (match case) another field value (specify fieldName as criterion.value)
//     */
//    NOT_STARTS_WITH_FIELD,
//
//    /**
//     * Regular expression match
//     */
//    REGEXP,
//
    /**
     * Starts with (case insensitive)
     */
    ISTARTS_WITH("iStartsWith"),

    /**
     * Does not start with (case insensitive)
     */
    INOT_STARTS_WITH("iNotStartWith"),

    /**
     * Starts with (match case)
     */
    STARTS_WITH("startsWith"),

    /**
     * Does not start with (match case)
     */
    NOT_STARTS_WITH("notStartsWith"),

//
//    /**
//     * Starts with (match case) another field value (specify fieldName as criterion.value)
//     */
//    STARTS_WITH_FIELD

    NOOP("");
    private String jsonValue;

    OperatorId(String jsonValue){
        this.jsonValue = jsonValue;
    }

    @JsonValue
    final String jsonValue() {
        return this.jsonValue;
    }
}

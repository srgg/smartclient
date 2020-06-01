package org.srg.smartclient.isomorphic.criteria;

import java.util.List;

// https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=class..Criterion
public class Criteria {

    private String _constructor;

    private String fieldName;

    private OperatorId operator;

    /**
     * Start value of a criterion with an operator of type "valueRange".
     */
    private Object start;

    /**
     * End value of a criterion with an operator of type "valueRange".
     */
    private Object end;

    /**
     * Value to be used in the application of this criterion.
     */
    private Object value;

    private List<Criteria>  criteria;

    public String get_constructor() {
        return _constructor;
    }

    public void set_constructor(String _constructor) {
        this._constructor = _constructor;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public OperatorId getOperator() {
        return operator;
    }

    public void setOperator(OperatorId operator) {
        this.operator = operator;
    }

    public Object getStart() {
        return start;
    }

    public void setStart(Object start) {
        this.start = start;
    }

    public Object getEnd() {
        return end;
    }

    public void setEnd(Object end) {
        this.end = end;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<Criteria> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<Criteria> criteria) {
        this.criteria = criteria;
    }
}

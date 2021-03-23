package org.srg.smartclient.isomorphic.criteria;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.srg.smartclient.utils.Serde;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return operator == null ? OperatorId.AND : operator;
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

    /**
     * Utility function
     */
    public Set<String> getCriteriaFieldNames() {
        final HashSet<String> r = new HashSet<>();

        if (getFieldName() != null && !getFieldName().isBlank()) {
            r.add(getFieldName());
        }

        if (getCriteria() != null) {
            for (Criteria c : getCriteria()) {
                final Set<String> rc = c.getCriteriaFieldNames();
                r.addAll(rc);
            }
        }

        return r;
    }

    @Override
    public String toString() {
        try {
            return "Criteria {\n\t%s\n}".formatted(
                    Serde.toJson(this)
            );
        } catch (JsonProcessingException e) {
            return "Criteria {\n\t%s\n}".formatted(
                    super.toString()
            );
        }
    }
}

package org.srg.smartclient.isomorphic.criteria;

//https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=object..AdvancedCriteria

import com.fasterxml.jackson.core.JsonProcessingException;
import org.srg.smartclient.isomorphic.IDSRequestData;
import org.srg.smartclient.utils.Serde;

public class AdvancedCriteria extends Criteria implements IDSRequestData {
    private boolean strictSQLFiltering;

    public boolean isStrictSQLFiltering() {
        return strictSQLFiltering;
    }

    public void setStrictSQLFiltering(boolean strictSQLFiltering) {
        this.strictSQLFiltering = strictSQLFiltering;
    }

    @Override
    public String toString() {
        try {
            return "AdvancedCriteria {\n\t%s\n}".formatted(
                    Serde.toJson(this)
                );
        } catch (JsonProcessingException e) {
            return "AdvancedCriteria {\n\t%s\n}".formatted(
                    super.toString()
            );
        }
    }
}

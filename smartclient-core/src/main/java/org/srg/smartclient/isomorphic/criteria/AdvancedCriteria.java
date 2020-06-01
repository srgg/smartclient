package org.srg.smartclient.isomorphic.criteria;

//https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=object..AdvancedCriteria

import org.srg.smartclient.isomorphic.IDSRequestData;

public class AdvancedCriteria extends Criteria implements IDSRequestData {
    private boolean strictSQLFiltering;

    public boolean isStrictSQLFiltering() {
        return strictSQLFiltering;
    }

    public void setStrictSQLFiltering(boolean strictSQLFiltering) {
        this.strictSQLFiltering = strictSQLFiltering;
    }
}

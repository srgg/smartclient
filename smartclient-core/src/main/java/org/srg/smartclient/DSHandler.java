package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

public interface DSHandler {
    String id();
    DataSource dataSource();
    DSResponse handle(DSRequest request) throws Exception;

    default boolean allowAdvancedCriteria() {
        return false;
    }
}

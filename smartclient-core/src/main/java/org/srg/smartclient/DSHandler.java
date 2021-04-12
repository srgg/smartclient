package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

public interface DSHandler extends IHandler {
    DataSource dataSource();

    default boolean allowAdvancedCriteria() {
        return false;
    }
}

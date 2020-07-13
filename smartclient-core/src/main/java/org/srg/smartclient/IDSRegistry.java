package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DataSource;

public interface IDSRegistry {
    DSHandler getHandlerByName(String dsId);

    default DataSource getDataSourceById(String dsId) {
        final DSHandler dsHandler = getHandlerByName(dsId);
        if (dsHandler == null) {
            return null;
        }

        return dsHandler.dataSource();
    }

}

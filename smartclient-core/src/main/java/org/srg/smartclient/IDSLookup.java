package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DataSource;

public interface IDSLookup {
    IHandler getHandlerById(String id);

    default DSHandler getDataSourceHandlerById(String id) {
        final IHandler handler = this.getHandlerById(id);
        if (handler instanceof DSHandler dsHandler) {
            return dsHandler;
        }

        throw new RuntimeException("Handler '%s' is not an instance of 'DSHandler'."
                .formatted(id)
        );
    }

    default DataSource getDataSourceById(String dsId) {
        final IHandler handler = getHandlerById(dsId);
        if (handler == null) {
            return null;
        }

        if (handler instanceof DSHandler dsHandler) {
            return dsHandler.dataSource();
        }

        throw new RuntimeException("Handler '%s' is not an instance of 'DSHandler'."
            .formatted(dsId)
        );
    }
}

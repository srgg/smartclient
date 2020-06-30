package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DataSource;

public interface IDSRegistry {
    DataSource getDataSourceById(String dsId);
}

package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DataSource;

import java.util.Collection;

public interface IDSRegistry extends IDSLookup{
    Collection<DSHandler> handlers();
}

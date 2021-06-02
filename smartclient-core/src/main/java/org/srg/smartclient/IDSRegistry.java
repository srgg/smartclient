package org.srg.smartclient;

import java.util.Collection;

public interface IDSRegistry extends IDSLookup{
    Collection<IHandler> handlers();
}

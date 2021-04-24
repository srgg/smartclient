package org.srg.smartclient.dmi;

import org.srg.smartclient.IHandler;

public abstract class AbstractDSDMIHandler implements IHandler {
    private final String id;

    protected AbstractDSDMIHandler(String id) {
        this.id = id;
    }

    abstract protected Object getInstance() throws Exception;

    @Override
    public String id() {
        return id;
    }
}

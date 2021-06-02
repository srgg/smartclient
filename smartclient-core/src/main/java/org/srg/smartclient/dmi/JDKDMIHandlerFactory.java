package org.srg.smartclient.dmi;

import org.srg.smartclient.IHandler;
import org.srg.smartclient.isomorphic.ServerObject;

import java.lang.reflect.InvocationTargetException;

public class JDKDMIHandlerFactory {

    public IHandler createDMIHandler(ServerObject so) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return switch (so.getLookupStyle()) {
            case New -> new JDKHandler(so.getId(), so.getClassName(), so.getMethodName());
            case Factory -> new FactoryHandler(so.getId(), so.getClassName(), so.getMethodName());
            default -> throw new IllegalStateException("Unsupported DMI lookup style '%s'."
                .formatted(so.getLookupStyle()));
        };
    }
}

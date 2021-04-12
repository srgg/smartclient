package org.srg.smartclient.dmi;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FactoryHandler extends AbstractDSDMIHandler {
    private final Object factory;
    private final Method factoryMethod;
    private final String dmiMethodName;

    public FactoryHandler(String id, String factoryClassName, String dmiMethodName) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        super(id);
        Class factoryClass = Class.forName(factoryClassName);
        this.factoryMethod = MethodUtils.getMatchingAccessibleMethod(factoryClass, "create");
        factory = ConstructorUtils.invokeConstructor(factoryClass);
        this.dmiMethodName = dmiMethodName;
    }

    @Override
    public DSResponse handle(DSRequest request) throws Exception {
        final Object instance = factoryMethod.invoke(factory);
        return (DSResponse) MethodUtils.invokeMethod(instance, dmiMethodName, request);
    }
}

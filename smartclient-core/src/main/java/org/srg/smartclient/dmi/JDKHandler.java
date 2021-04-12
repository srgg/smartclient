package org.srg.smartclient.dmi;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JDKHandler extends AbstractDSDMIHandler {
    private final Class handlerClass;
    private final Method method;

    public JDKHandler(String id, String className, String methodName) throws ClassNotFoundException {
        super(id);
        this.handlerClass = Class.forName(className);
        this.method = MethodUtils.getMatchingAccessibleMethod(handlerClass, methodName, DSRequest.class);
        if (method == null) {
            throw new RuntimeException("JDKDMIHandler: Can't find method '%s' in class '%s'."
                    .formatted(methodName, className)
            );
        }
    }

    @Override
    public DSResponse handle(DSRequest request) throws Exception {
        final Object instance;
        if (Modifier.isStatic(method.getModifiers())) {
            instance = null;
        } else {
            instance = ConstructorUtils.invokeConstructor(handlerClass);
        }
        return (DSResponse) method.invoke(instance, request);

    }
}

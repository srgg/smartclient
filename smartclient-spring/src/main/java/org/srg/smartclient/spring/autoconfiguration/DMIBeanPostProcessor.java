package org.srg.smartclient.spring.autoconfiguration;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.core.annotation.Order;
import org.srg.smartclient.IDSDispatcher;
import org.srg.smartclient.annotations.SmartClientDMIHandler;
import org.srg.smartclient.dmi.AbstractDSDMIHandler;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import java.lang.reflect.Method;

@Order
public class DMIBeanPostProcessor implements BeanPostProcessor {
    public static class BeanDMIHandler extends AbstractDSDMIHandler {
        private Object bean;
        private String methodName;

        public BeanDMIHandler(String id, Object bean, String methodName) {
            super(id);
            this.bean = bean;
            this.methodName = methodName;
        }

        @Override
        protected Object getInstance() {
            return bean;
        }

        protected Method getMethod(Object instance) {
            final Method m  = MethodUtils.getMatchingAccessibleMethod(instance.getClass(), methodName, DSRequest.class);

            if (m == null) {
                throw new RuntimeException("DMIBeanPostProcessor: Can't find method '%s' in class '%s'."
                        .formatted(methodName, instance.getClass().getCanonicalName())
                );
            }
            return m;
        }

        @Override
        public DSResponse handle(DSRequest request) throws Exception {
            final Object instance = getInstance();
            final Method m = getMethod(instance);

            return (DSResponse) m.invoke(instance, request);
        }
    }

    @Autowired
    private IDSDispatcher dispatcher;

    private static Logger logger = LoggerFactory.getLogger(DMIBeanPostProcessor.class);
    private Object getTargetObject(Object proxy, String beanName) throws BeansException {
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            try {
                return ((Advised) proxy).getTargetSource().getTarget();
            } catch (ScopeNotActiveException ex) {
                // There is nothing to do

                logger.debug("Skipping bean %s, can't get target object from proxy."
                        .formatted(beanName), ex);
            } catch (Exception e) {
                throw new FatalBeanException("Error getting target of JDK proxy", e);
            }
        }
        return proxy;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Object proxy = this.getTargetObject(bean, beanName);
        final SmartClientDMIHandler annotation = proxy.getClass().getAnnotation(SmartClientDMIHandler.class);
        if (annotation != null) {
            this.logger.info("{}: processing bean of type {}",
                    this.getClass().getSimpleName(), proxy.getClass().getName());
            final BeanDMIHandler h = new BeanDMIHandler(annotation.id(), bean, annotation.methodName() );
            dispatcher.registerHandler(h);
        }
        return bean;
    }
}

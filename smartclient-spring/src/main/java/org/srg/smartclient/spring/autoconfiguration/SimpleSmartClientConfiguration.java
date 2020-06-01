package org.srg.smartclient.spring.autoconfiguration;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.srg.smartclient.IDSDispatcher;
import org.srg.smartclient.spring.AutomaticDSHandlerRegistrar;
import org.srg.smartclient.spring.EnableSmartClient;
import org.srg.smartclient.spring.SmartClientProperties;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base {@code Configuration} class providing common structure for enabling and using Smart Client. Customization is
 * available by implementing the {@link SmartClientConfigurer} interface. The main components are created as lazy proxies that
 * only initialize when a method is called. This is to prevent (as much as possible) configuration cycles from
 * developing when these components are needed in a configuration resource that itself provides a
 * {@link SmartClientConfigurer}.
 *
 * @see EnableSmartClient
 */
@Configuration
public class SimpleSmartClientConfiguration {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired
    private SmartClientProperties smartClientProperties;

    private boolean initialized = false;
    private SmartClientConfigurer configurer;
    private AutomaticDSHandlerRegistrar registrar = new AutomaticDSHandlerRegistrar();

    private AtomicReference<IDSDispatcher> dsDispatcher = new AtomicReference<>();

    @Bean
    public IDSDispatcher dsDispatcher() throws Exception {
        return createLazyProxy(dsDispatcher, IDSDispatcher.class);
    }


    private <T> T createLazyProxy(AtomicReference<T> reference, Class<T> type) {
        ProxyFactory factory = new ProxyFactory();
        factory.setTargetSource(new ReferenceTargetSource<>(reference));
        factory.addAdvice(new PassthruAdvice());
        factory.setInterfaces(new Class<?>[] { type });
        @SuppressWarnings("unchecked")
        T proxy = (T) factory.getProxy();
        return proxy;
    }

    /**
     * Sets up the basic components by extracting them from the {@link SmartClientConfigurer configurer}, defaulting to some
     * sensible values as long as a unique DataSource is available.
     *
     * @throws Exception if there is a problem in the configurer
     */
    protected void initialize() throws Exception {
        if (initialized) {
            return;
        }

        Map<String, SmartClientConfigurer> beans =  context.getBeansOfType(SmartClientConfigurer.class);

        SmartClientConfigurer configurer = getConfigurer(beans.values());
        dsDispatcher.set(configurer.getDsDispatcher());
        initialized = true;
    }

    private class PassthruAdvice implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return invocation.proceed();
        }

    }

    private class ReferenceTargetSource<T> extends AbstractLazyCreationTargetSource {

        private AtomicReference<T> reference;

        public ReferenceTargetSource(AtomicReference<T> reference) {
            this.reference = reference;
        }

        @Override
        protected Object createObject() throws Exception {
            initialize();
            return reference.get();
        }
    }

    protected SmartClientConfigurer getConfigurer(Collection<SmartClientConfigurer> configurers) throws Exception {
        if (this.configurer != null) {
            return this.configurer;
        }
        if (configurers == null || configurers.isEmpty()) {
            throw new IllegalStateException();
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException(
                    "To use a custom BatchConfigurer the context must contain precisely one, found "
                            + configurers.size());
        }
        this.configurer = configurers.iterator().next();
        return this.configurer;
    }

    @Bean
    public AutomaticDSHandlerRegistrar smartClientRegistrar() throws Exception {
        registrar.setDsDispatcher(dsDispatcher());
        registrar.setProperties(smartClientProperties);
        return registrar;
    }
}


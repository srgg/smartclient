package org.srg.smartclient.spring.autoconfiguration;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.srg.smartclient.IDSDispatcher;
import org.srg.smartclient.JpaDSDispatcher;
import org.srg.smartclient.spring.SmartClientProperties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * A {@link BasicSmartClientConfigurer} tailored for JPA.
 */
public class JpaSmartClientConfigurer extends BasicSmartClientConfigurer {
    private final EntityManagerFactory entityManagerFactory;

    protected JpaSmartClientConfigurer(SmartClientProperties properties, DataSource dataSource,
                                       EntityManagerFactory entityManagerFactory) {

        super(properties, dataSource);
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    protected IDSDispatcher buildDSDispatcher() {
        return new JpaDSDispatcher(entityManagerFactory, JpaSmartClientConfigurer.this.getJdbcPolicy());
    }

    @Override
    protected PlatformTransactionManager createTransactionManager() {
        return new JpaTransactionManager(this.entityManagerFactory);
    }
}

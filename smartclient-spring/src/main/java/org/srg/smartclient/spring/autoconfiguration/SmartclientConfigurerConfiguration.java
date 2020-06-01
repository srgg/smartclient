package org.srg.smartclient.spring.autoconfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.srg.smartclient.spring.SmartClientProperties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Provide a {@link SmartClientConfigurer} according to the current environment.
 *
 */
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(SmartClientConfigurer.class)
@Configuration(proxyBeanMethods = false)
public class SmartclientConfigurerConfiguration {

    public SmartclientConfigurerConfiguration(){

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(name = "entityManagerFactory")
    static class JdbcBatchConfiguration {
        @Bean
        BasicSmartClientConfigurer smartСlientConfigurer(SmartClientProperties properties, DataSource dataSource) {
            return new BasicSmartClientConfigurer(properties,dataSource);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EntityManagerFactory.class)
    @ConditionalOnBean(name = "entityManagerFactory")
    static class JpaBatchConfiguration {

        @Bean
        JpaSmartClientConfigurer smartСlientConfigurer(SmartClientProperties properties, DataSource dataSource,
                                                       EntityManagerFactory entityManagerFactory) {
            return new JpaSmartClientConfigurer(properties, dataSource, entityManagerFactory);
        }
    }
}

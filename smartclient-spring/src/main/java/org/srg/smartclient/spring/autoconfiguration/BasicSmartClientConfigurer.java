package org.srg.smartclient.spring.autoconfiguration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.srg.smartclient.DSDispatcher;
import org.srg.smartclient.IDSDispatcher;
import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.spring.SmartClientProperties;

import javax.sql.DataSource;

/**
 * Basic {@link SmartClientConfigurer} implementation.
 */
public class BasicSmartClientConfigurer implements SmartClientConfigurer, InitializingBean {
    private final SmartClientProperties properties;
    private final DataSource dataSource;
    private PlatformTransactionManager transactionManager;
    private IDSDispatcher dsDispatcher;
    private DataSourceJDBCPolicy dataSourceJDBCPolicy;


    public BasicSmartClientConfigurer(SmartClientProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() throws Exception {
        return this.transactionManager;
    }

    @Override
    public IDSDispatcher getDsDispatcher() throws Exception {
        return dsDispatcher;
    }

    protected JDBCHandler.JDBCPolicy getJdbcPolicy() {
        return dataSourceJDBCPolicy;
    }

    protected IDSDispatcher buildDSDispatcher() {
        return new DSDispatcher(BasicSmartClientConfigurer.this.getJdbcPolicy());
    }

    private PlatformTransactionManager buildTransactionManager() {
        PlatformTransactionManager transactionManager = createTransactionManager();
//            if (this.transactionManagerCustomizers != null) {
//                this.transactionManagerCustomizers.customize(transactionManager);
//            }
        return transactionManager;
    }

    protected PlatformTransactionManager createTransactionManager() {
        return new DataSourceTransactionManager(this.dataSource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            Assert.state(dataSource != null, "DataSource must be set.");

            dataSourceJDBCPolicy = new DataSourceJDBCPolicy(dataSource);

            this.transactionManager = buildTransactionManager();
            this.dsDispatcher = buildDSDispatcher();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize Smart Client", ex);
        }
    }
}

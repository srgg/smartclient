package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;

public class JpaDSDispatcher extends DSDispatcher {
    private static Logger logger = LoggerFactory.getLogger(JpaDSDispatcher.class);
    private EntityManagerFactory emf;
    private JPAAwareHandlerFactory jpaAwareHandlerFactory = new JPAAwareHandlerFactory();

    public JpaDSDispatcher(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public JpaDSDispatcher(EntityManagerFactory emf, JDBCHandler.JDBCPolicy jdbcPolicy) {
        super(jdbcPolicy);
        this.emf = emf;
    }

    public String registerJPAEntity(Class<?> entityClass) {
        final JDBCHandler handler = jpaAwareHandlerFactory.createHandler(
                emf,
                getJdbcPolicy(),
                this,
                entityClass
        );

        registerDatasource(handler);

        return handler.id();
    }
}

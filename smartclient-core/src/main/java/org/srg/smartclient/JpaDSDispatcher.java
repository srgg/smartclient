package org.srg.smartclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DataSource;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JpaDSDispatcher extends DSDispatcher {
    private static Logger logger = LoggerFactory.getLogger(JpaDSDispatcher.class);
    private EntityManagerFactory emf;
    private JPAAwareHandlerFactory jpaAwareHandlerFactory = new JPAAwareHandlerFactory();
    private Map<Class, JDBCHandler> jpaHandlers = new ConcurrentHashMap<>();

    public JpaDSDispatcher(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public JpaDSDispatcher(EntityManagerFactory emf, JDBCHandler.JDBCPolicy jdbcPolicy) {
        super(jdbcPolicy);
        this.emf = emf;
    }


    public void registerJPAEntities(Class<?>... entityClasses) {
        final List<JDBCHandler> handlers = new ArrayList<>(entityClasses.length);
        final Map<Class, JDBCHandler> handlerMap = new HashMap<>();

        for (Class<?> cls: entityClasses) {
            JDBCHandler h = jpaAwareHandlerFactory.createHandler(
                    emf,
                    getJdbcPolicy(),
                    this,
                    cls
            );

            handlers.add(h);
            handlerMap.put(cls, h);
        }

        registerHandlers(handlers.toArray(new IHandler[0]));
        jpaHandlers.putAll(handlerMap);

        // - Dump generated DataSource definitions, if enabled
        if (logger.isDebugEnabled()) {
            for(IHandler h :handlers) {
                if (h instanceof DSHandler dsh) {
                    String dsDefinition;
                    try {
                        dsDefinition = DSDeclarationBuilder.build(this, "<URL-PLACE-HOLDER>", dsh.dataSource(), true);
                    } catch (Exception e) {
                        dsDefinition = "Can't serialize Data Source definition, unexpected error occurred: %s"
                                .formatted(
                                        e.getMessage()
                                );

                        logger.warn(dsDefinition, e);
                    }

                    Class<?> ec = null;
                    for (Map.Entry<Class, JDBCHandler> e: handlerMap.entrySet()) {
                        if (e.getValue().equals(h)) {
                            ec = e.getKey();
                        }
                    }

                    logger.debug("DataSource definition for entity '%s' has been built:\n%s"
                            .formatted(
                                    ec.getCanonicalName(),
                                    dsDefinition
                            )
                    );
                }
            }
        }
    }

    public IHandler getHandlerByClass(Class<?> entityClass) {
        return jpaHandlers.get(entityClass);
    }
}

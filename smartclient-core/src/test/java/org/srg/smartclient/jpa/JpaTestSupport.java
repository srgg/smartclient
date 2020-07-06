package org.srg.smartclient.jpa;

import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class JpaTestSupport {
    private JpaTestSupport() {
    }

    public static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Class ...entityClasses) {
        return new HibernatePersistenceProvider()
                .createContainerEntityManagerFactory(
                        getPU(persistenceUnitName,
                                Arrays.stream(entityClasses)
                                    .map( c -> c.getCanonicalName())
                                .collect(Collectors.toList())
                        ),
                        Map.of(
//                                "hibernate.connection.url", "jdbc:h2:mem:test;USER=sa;DB_CLOSE_DELAY=0;MODE=MySql;TRACE_LEVEL_SYSTEM_OUT=0;DATABASE_TO_LOWER=TRUE;INIT=RUNSCRIPT FROM 'classpath:test-data.sql';",
                                "hibernate.connection.url", "jdbc:h2:mem:test;USER=sa;DB_CLOSE_DELAY=0;MODE=MySql;TRACE_LEVEL_SYSTEM_OUT=0;DATABASE_TO_LOWER=TRUE;",
                                "hibernate.connection.driver_class", "org.h2.Driver",
                                "hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect",
                                "hibernate.hbm2ddl.auto", "none",
                                "hibernate.connection.username", "sa",
                                "hibernate.connection.password","sa",
                                "hibernate.show_sql", "true"
                        )
                );
    }

    private static PersistenceUnitInfo getPU(String name, List<String> managedClassNames){
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return name;
            }

            @Override
            public String getPersistenceProviderClassName() {
                return "org.hibernate.jpa.HibernatePersistenceProvider";
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return null;
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public List<URL> getJarFileUrls() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return managedClassNames;
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return true;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return null;
            }

            @Override
            public ValidationMode getValidationMode() {
                return null;
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {

            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }

}

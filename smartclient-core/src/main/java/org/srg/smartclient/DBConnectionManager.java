package org.srg.smartclient;

import org.apache.commons.beanutils.BeanUtils;
import org.srg.smartclient.isomorphic.Config;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class DBConnectionManager implements JDBCHandler.JDBCPolicy {
    protected static class DBContext {
        private DataSource dataSource;

        public DataSource getDataSource() {
            return dataSource;
        }
    }

    private static DBConnectionManager instance = new DBConnectionManager();
    private transient Map<String, DBContext> databaseMap = new HashMap<>();
    private transient String defaultDatabase;

    public static DBConnectionManager get() {
        return instance;
    }

    protected Config.SQLConfig getSqlConfig() throws IOException {
        return Config.get().getSql();
    }

    protected String getDefaultDatabase() throws IOException {
        if (defaultDatabase == null) {
            defaultDatabase = getSqlConfig().getDefaultDatabase();
        }

        return defaultDatabase;
    }

    protected Connection getConnection(String database) throws Exception {
        if (database == null || database.isEmpty()) {
            database = getDefaultDatabase();
        }

        DBContext dbCtx = databaseMap.get(database);
        if (dbCtx == null) {
            final Config.SQLConfig.Connection connectionInfo = getSqlConfig().getConnections().get(database);
            dbCtx = new DBContext();

            final Class<? extends DataSource> dsClazz;
            try {
                dsClazz = (Class<? extends DataSource>) Class.forName(connectionInfo.getDriverClass());
            } catch (ClassNotFoundException e) {
                throw e;
            }

            try {
                dbCtx.dataSource = dsClazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw e;
            }

            BeanUtils.populate(dbCtx.dataSource, connectionInfo.getDriverProperties());
            databaseMap.put(database, dbCtx);
        }

        return dbCtx.getDataSource().getConnection();
    }

    @Override
    public void withConnectionDo(String database, Utils.CheckedFunction<Connection, Void> callback) throws Exception {
        try (Connection connection = this.getConnection(database)){
            callback.apply(connection);
        }
    }
}

package org.srg.smartclient.isomorphic;

import org.srg.smartclient.utils.JsonSerde;
import org.srg.smartclient.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * https://www.smartclient.com/smartclient-12.0/isomorphic/system/reference/?id=group..server_properties&ref=group:iscInstall
 */
final public class Config {
    /**
     * https://www.smartclient.com/isomorphic/system/reference/?id=group..sqlSettings
     */
    public static class SQLConfig {
        /**
         * https://www.smartclient.com/smartclient-12.0/isomorphic/system/reference/?id=group..sqlDataSource
         */
//        public static class Pool {
//            public enum WhenExhaustedAction {
//                fail,
//                block,
//                grow
//            }
//
//            private boolean enabled = true;
//            private int maxActive = -1;
//            private int maxIdle = -1;
//            private int minIdle = -1;
//            private WhenExhaustedAction whenExhaustedAction = WhenExhaustedAction.grow;
//            private boolean testOnBorrow = true;
//            private boolean testOnReturn = false;
//            private boolean testWhileIdle = false;
//            private int timeBetweenEvictionRunsMillis = -1;
//            private int minEvictableIdleTimeMillis = -1;
//            private int numTestsPerEvictionRun = -1;
//        }

        public static class Connection {
            public static class Database {
                public enum DatabaseType {
                    mysql,
                    h2
                }
                private DatabaseType type;
                private boolean ansiMode = false;

                public DatabaseType getType() {
                    return type;
                }

                public boolean isAnsiMode() {
                    return ansiMode;
                }
            }

            private String name;
            private Database database = new Database();
            private String driverClass;
            private Map<String, Object> driverProperties = new HashMap<>();

            public String getName() {
                return name;
            }

            public Database getDatabase() {
                return database;
            }

            public String getDriverClass() {
                return driverClass;
            }

            public Map<String, Object> getDriverProperties() {
                return Collections.unmodifiableMap(driverProperties);
            }
        }

        private String defaultDatabase;
        private Map<String, Connection> connections = new HashMap<>();

        public String getDefaultDatabase() {
            return defaultDatabase;
        }

        public Map<String, Connection> getConnections() {
            return Collections.unmodifiableMap(connections);
        }
    }

    private SQLConfig sql = new SQLConfig();

    private static Config instance;

    static {
        try {
            instance = loadAndParse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Config get() {
        return instance;
    }

    private Config() {}

    public SQLConfig getSql() {
        return sql;
    }

    private static Map<String, Object> makeHierarchyFromProps(Properties props) {
        final Map<String, Object> root = new HashMap<>();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            String[] path = ((String)entry.getKey()).split("\\.");
            Map<String,Object> parent = root;
            int n = path.length - 1;
            for (int i = 0; i < n; ++i) {
                String p = path[i];
                Object child = parent.get(p);

                if (child instanceof Map) {
                    parent = (Map<String, Object>) child;
                } else {
                    Map<String,Object> _p = new HashMap<>();
                    parent.put(p, _p);
                    parent = _p;

                    if (child != null) {
                        _p.put(p, child);
                    }
                }
            }

            final Object o = parent.get(path[n]);

            if (o instanceof Map) {
                ((Map<String, Object>)o).put(path[n], entry.getValue());
            } else {
                parent.put(path[n], entry.getValue());
            }
        }
        return root;
    }

    protected static Config loadAndParse() throws IOException {
        final Config cfg = new Config();

        final URL url = Utils.getResource("server.properties");
        final Properties props = new Properties();

        try (InputStream is = new FileInputStream(url.getPath()) ) {
            props.load(is);
        }

        final Map<String, Object> hierarchy = makeHierarchyFromProps(props);

        // -- parse SQL config
        final SQLConfig sqlConfig = new SQLConfig();
        final Map<String, ?>  sql = (Map<String, ?>) hierarchy.get("sql");
        sqlConfig.defaultDatabase = (String) sql.get("defaultDatabase");
        for (Map.Entry<String, ?> e: sql.entrySet()) {
            if (!(e.getValue() instanceof Map)) {
                // skip single values (like a 'defaultDatabase') that should be processed already
                continue;
            }

            final Map<String, ?> v = (Map<String, ?>) e.getValue();

            final SQLConfig.Connection c = new SQLConfig.Connection();
            c.name = e.getKey();

            // -- parse Database section
            final Map<String, Object> db = (Map<String, Object>) v.get("database");
            c.database = JsonSerde.createMapper().convertValue(db, SQLConfig.Connection.Database.class);

            // -- parse driver section
            final Object d = v.get("driver");
            final String driverClass;
            final Map<String, Object> driverProps;
            if (d instanceof String) {
                driverClass = (String)d;
                driverProps = Collections.emptyMap();
            } else if (d instanceof Map) {
                driverProps = new HashMap<>((Map<String, Object>)d);
                driverClass = (String) driverProps.remove("driver");
            } else {
                throw new IllegalStateException();
            }

            c.driverClass = driverClass;
            c.driverProperties = driverProps;

            // --
            sqlConfig.connections.put(c.getName(), c);
        }

        cfg.sql = sqlConfig;
        return cfg;
    }
}

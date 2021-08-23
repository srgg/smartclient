package org.srg.smartclient;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.h2.Driver;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.utils.Serde;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.TimeZone;

public abstract class AbstractJDBCHandlerTest<H extends JDBCHandler> extends AbstractHandlerTest<H> {
    protected static class ExtraField extends AbstractHandlerTest.ExtraFieldBase{

        public static String OneToMany_FetchEntireEntities = Employee_RolesFromEmployeeRole;

        public static String OneToMany_FetchOnlyIds = """
                [
                    {
                        name:"roles"
                        ,foreignKey:"EmployeeRoleDS.employee"
                        ,multiple:true
                    }
                ]""";

        public static String Project_IncludeManagerEmailFromEmployee = """
                [
                     {
                         name:"manager_email"
                         ,type:"TEXT"
                         ,includeFrom:"EmployeeDS.email"
                         ,includeVia:"manager"
                         ,canEdit:false
                         ,hidden:false
                     }
                ]""";

        public static String Project_IncludeFromClient = """
                [
                    {
                        name:"client"
                        ,foreignKey:"ClientDS.id"
                        ,displayField:"clientName"
                        ,foreignDisplayField:"name"
                        ,dbName:"client_id"
                        ,canEdit:false
                    },
                    {
                        name:"clientName"
                        ,type:"TEXT"
                        ,includeFrom:"ClientDS.name"
                        ,includeVia:"client"
                        ,canEdit:false
                        ,hidden:true
                    }
                ]""";

        public static String IncludeFromLocation = """
                [
                    {
                        name: 'location'
                        ,dbName: 'location_id'
                        ,type: "INTEGER"
                        ,foreignKey:"LocationDS.id"
                        ,displayField:"locationCity"
                    },
                    {
                        name:"locationCity"
                        ,type:"TEXT"
                        ,includeFrom:"LocationDS.city"
                    }
                ]""";

        public static String Email = """
                [
                    {
                        name: "email",
                        type: "text"
                    }
                ]
                """;

        public static String FiredAt = """
                [
                    {
                        name: "firedAt",
                        type: "datetime"
                    }
                ]""";

        public static String SqlCalculated = """
                [
                    {
                        name: "calculated",
                        type: "text",
                        customSelectExpression: "CONCAT(employee.id, '_', employee.name)"
                    }
                ]
                """;

        private ExtraField(){}
    }

    private static File dbHome = new File("./target/db");
    private Server dbServer;
    protected JdbcDataSource jdbcDataSource = new JdbcDataSource();
    private Connection connection;
    private JDBCHandler.JDBCPolicy jdbcPolicy;


    @BeforeAll
    public static void setupObjectMapper() {
        JsonTestSupport.defaultMapper = Serde.createMapper();
    }

    @AfterAll
    public static void shutdownDB() {
    }

    protected Void initDB(javax.sql.DataSource ds) {
        Flyway flyway = new Flyway(
                new FluentConfiguration()
                        .dataSource(ds)
                        .locations("classpath:db")
        );

        flyway.clean();
        flyway.migrate();

        return null;
    }

    @Override
    protected JDBCHandler.JDBCPolicy getJDJdbcPolicy() {
        return (db, callback) -> {
            jdbcPolicy.withConnectionDo(db, callback);
        };
    }

    private static Server startDBServer(String jdbcUrls) {
        String url = jdbcUrls;
        String port = "9092";
        String user = "sa";
        String password = "sa";
        try {
            final Server dbServer;
            if (url.contains("/mem:")) {
                dbServer = Server.createTcpServer("-tcpPort", port, "-baseDir", dbHome.getAbsolutePath(), "-tcpAllowOthers");
            } else {
                dbServer = Server.createTcpServer("-tcpPort", port, "-ifExists", "-baseDir", dbHome.getAbsolutePath(), "-tcpAllowOthers");
            }

            System.out.println("Starting embedded database on port " + dbServer.getPort() + " with url " + url);
            dbServer.start();
            System.out.println("Embedded database started. Data stored in: " + dbHome.getAbsolutePath());

            // Clear all db files (if any) from previous run
            DeleteDbFiles.execute(dbHome.getAbsolutePath(), "test", true);

            if (!url.contains("/mem:")) {
                // Create DB
                String url2 = String.format("jdbc:h2:%s/test;USER=%s;PASSWORD=%s", dbHome.getAbsolutePath(), user, password);
                DriverManager.registerDriver(new Driver());
                DriverManager.getConnection(url2).close();
            }

            return dbServer;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to start database", e);
        }
    }

    @BeforeEach
    public void setupDataSources() throws Exception {

        // SET TZ to UTC, otherwise timestamps stored in DB as GMT will be treated as local timezone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final String url = "jdbc:h2:mem:test:˜/test;DB_CLOSE_DELAY=0;AUTOCOMMIT=OFF;database_to_lower=true;USER=sa;PASSWORD=sa";
//        final String url ="jdbc:h2:tcp://localhost:9092/test;DB_CLOSE_DELAY=0;AUTOCOMMIT=OFF;database_to_lower=true;USER=sa;PASSWORD=sa";
        dbServer = startDBServer(url);
        jdbcDataSource.setURL(url);
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("sa");

        connection = jdbcDataSource.getConnection();
        initDB(jdbcDataSource);
        connection.commit();

        jdbcPolicy = (db, callback) -> {
            try (Connection conn = jdbcDataSource.getConnection()) {
                callback.apply(conn);
            }
        };
    }

    @AfterEach
    public void closeDbConnection() throws SQLException {
        if (dbServer == null) {
            return;
        }

        if (connection != null) {
            connection.createStatement().execute("drop all objects delete files");
            connection.close();
        }

        connection = null;
        dbServer.stop();
    }

}

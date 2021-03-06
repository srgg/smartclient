package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.utils.Serde;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractJDBCHandlerTest<H extends JDBCHandler> {
    protected static class ExtraField {
        public static String ManyToMany = """
                    [
                        {
                            name:"teamMembers"
                            ,tableName:"project_team"
                            ,type:"integer"
                            ,foreignKey:"EmployeeDS.id"
                            ,multiple:true
                        }
                    ]
                """;

        public static String OneToMany_FetchEntireEntities = """
                [
                    {
                        name:"roles"
                        ,foreignKey:"EmployeeRoleDS.employee"
                        ,type:"EmployeeRoleDS"
                        ,foreignDisplayField:"role"
                        ,multiple:true
                    }
                ]""";

        public static String OneToMany_FetchOnlyIds = """
                [
                    {
                        name:"roles"
                        ,foreignKey:"EmployeeRoleDS.employee"
                        ,multiple:true
                    }
                ]""";

        public static String Project_IncludeManagerFromEmployee = """
                [
                     {
                         name:"manager"
                         ,foreignKey:"EmployeeDS.id"
                         ,displayField:"employeeFullName"
                         ,foreignDisplayField:"fullName"
                         ,dbName:"manager_id"
                         ,canEdit:false
                     },
                     {
                         name:"employeeFullName"
                         ,type:"TEXT"
                         ,includeFrom:"EmployeeDS.calculated"
                         ,includeVia:"manager"
                         ,canEdit:false
                         ,hidden:true
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

    protected static class Handler {
        public static String Country = """
                {
                  id: "CountryDS",
                  tableName: "countries",
                  fields: [
                    {
                      name: "id",
                      type: "integer",
                      required: true,
                      primaryKey: true
                    },
                    {
                      name: "name",
                      type: "text"
                    }
                  ]
                }""";

        public static String Location = """
                {
                  id: "LocationDS",
                  tableName: "locations",
                  fields: [
                    {
                      name: "id",
                      type: "integer",
                      required: true,
                      primaryKey: true
                    },
                    {
                      name: "city",
                      type: "text"
                    }
                  ]
                }""";

        public static String Client = """
                {
                  id: "ClientDS",
                  tableName: "client",
                  fields: [
                    {
                      name: "id",
                      type: "integer",
                      required: true,
                      primaryKey: true
                    },
                    {
                      name: "name",
                      type: "text"
                    }
                  ]
                }""";

        public static String Employee = """
                {
                   id: 'EmployeeDS',
                   serverType: 'sql',
                   tableName: 'employee',
                   fields: [
                       {
                           name: 'id',
                           type: 'integer',
                           primaryKey: true
                       },
                       {
                           name: 'name',
                           type: 'text',
                           required: true
                       }
                   ]
                }""";

        public static String EmployeeRole = """
                {
                    id: 'EmployeeRoleDS',
                    tableName: 'employee_role',
                    fields:[
                        {
                            name:"role"
                            ,type:"TEXT"
                            ,primaryKey:true
                            ,canEdit:false
                        },
                        {
                            name:"employee"
                            ,dbName:"employee_id"
                            ,foreignKey:"EmployeeDS.id"
                            ,type: "integer"
                            ,displayField:"employeeCalculated"
                            ,foreignDisplayField:"calculated"
                            ,primaryKey:true
                            ,canEdit:false
                            ,hidden:true
                        },
                        {
                            name:"employeeCalculated"
                            ,type:"TEXT"
                            ,includeFrom:"EmployeeDS.calculated"
                            ,includeVia:"employee"
                            ,canEdit:false
                            ,hidden:true
                        }
                    ]
                }""";

        public static String Project = """
                {
                    id: 'ProjectDS',
                    tableName: 'project',
                    fields:[
                        {
                            name:"id"
                            ,type:"INTEGER"
                            ,primaryKey:true
                            ,canEdit:false
                            ,hidden:true
                        },
                        {
                            name:"name"
                            ,type:"TEXT"
                            ,canEdit:false
                        }
                    ]
                }""";

        private Handler() {}
    }

    protected JdbcDataSource jdbcDataSource = new JdbcDataSource();
    private IDSRegistry dsRegistry;
    private Connection connection;
    private JDBCHandler.JDBCPolicy jdbcPolicy;
    protected H handler;


    @BeforeAll
    public static void setupDB() {
        JsonTestSupport.defaultMapper = Serde.createMapper();
    }

    @AfterAll
    public static void shutdownDB() {
    }

    protected Void initDB() {
        Flyway flyway = new Flyway(
                new FluentConfiguration()
                        .dataSource(jdbcDataSource)
                        .locations("classpath:db")
        );

        flyway.clean();
        flyway.migrate();

        return null;
    }

    @BeforeEach
    public void setupDataSources() throws Exception {
        jdbcDataSource.setURL("jdbc:h2:mem:test:˜/test;DB_CLOSE_DELAY=0;AUTOCOMMIT=OFF;database_to_lower=true");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("sa");

        connection = jdbcDataSource.getConnection();
        initDB();
        connection.commit();

        jdbcPolicy = (db, callback) -> {
            try (Connection conn = jdbcDataSource.getConnection()) {
                callback.apply(conn);
            }
        };

        dsRegistry = Mockito.mock(IDSRegistry.class);
        handler = withHandlers(Handler.Employee);
    }

    @AfterEach
    public void closeDbConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }

        connection = null;
    }

    protected H doInitHandler(DataSource ds) throws Exception {
        final Class<H> hc = getHandlerClass();

        final Constructor<H> constructor = hc.getConstructor(JDBCHandler.JDBCPolicy.class, IDSRegistry.class, DataSource.class);
        final H handler = Mockito.spy(
                constructor.newInstance(jdbcPolicy, dsRegistry, ds)
        );

        Mockito.doReturn(ds)
                .when(dsRegistry)
                .getDataSourceById(
                        Mockito.matches(ds.getId())
                );

        Mockito.doReturn(handler)
                .when(dsRegistry)
                .getHandlerByName(
                        Mockito.matches(ds.getId())
                );

        return handler;
    }

    abstract protected Class<H> getHandlerClass();

    protected static <H extends JDBCHandler> H withExtraFields(H h, String... extraFields) {
        for (String ef : extraFields) {
            final List<DSField> ofs = new LinkedList<>(h.getDataSource().getFields());
            final List<DSField> efs = JsonTestSupport.fromJSON(new TypeReference<>() {}, ef);
            ofs.addAll(efs);

            h.getDataSource().setFields(ofs);
        }

        return h;
    }

//    protected static <H extends JDBCHandler> H withExtraFields(H h, ExtraField... extraFields) {
//        for (ExtraField ef : extraFields) {
//            final List<DSField> efs = JsonTestSupport.fromJSON(new TypeReference<>() {}, ef.fieldDefinition);
//            withExtraFields(h, efs.toArray(new String[]{}));
//        }
//        return h;
//    }

//    protected H withExtraFields(ExtraField... extraFields) {
//        return withExtraFields(handler, extraFields);
//    }

    protected H withExtraFields(String... extraFields) {
        return withExtraFields(handler, extraFields);
    }

    protected H withHandler(String dataSourceDefinition) throws Exception {
        final DataSource ds = JsonTestSupport.fromJSON(DataSource.class, dataSourceDefinition);
        return doInitHandler(ds);
    }

    protected H withHandlers(String... handlers) throws Exception {
        H handler = null;
        for (String h:handlers) {
            handler = withHandler(h);
        }
        return handler;
    }
}

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
import org.srg.smartclient.utils.JsonSerde;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractJDBCHandlerTest<H extends JDBCHandler> {
    protected enum ExtraField {
        ManyToMany("""
                    [
                        {
                            name:"teamMembers"
                            ,tableName:"project_team"
                            ,type:"integer"
                            ,foreignKey:"EmployeeDS.id"
                            ,multiple:true
                        }
                    ]
                """),

        OneToMany_FetchEntireEntities("""
                [
                    {
                        name:"roles"
                        ,foreignKey:"EmployeeRoleDS.employee"
                        ,type:"EmployeeRoleDS"
                        ,foreignDisplayField:"role"
                        ,multiple:true
                    }
                ]"""
        ),

        OneToMany_FetchOnlyIds("""
                [
                    {
                        name:"roles"
                        ,foreignKey:"EmployeeRoleDS.employee"
                        ,multiple:true
                    }
                ]"""
        ),

        Project_IncludeFromEmployee(""" 
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
                           
                ]"""),

        Project_IncludeFromClient("""
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
                ]"""),

        IncludeFromLocation("""
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
                ]"""),

        Email("""
                [
                    {
                        name: "email",
                        type: "text"
                    }
                ]
                """),

        FiredAt("""
                [
                    {
                        name: "firedAt",
                        type: "datetime"
                    }
                ]"""),

        SqlCalculated("""
                [
                    {
                        name: "calculated",
                        type: "text",
                        customSelectExpression: "CONCAT(employee.id, '_', employee.name)"                    
                    }
                ]
                """);

        final String fieldDefinition;


        ExtraField(String fieldDefinition) {
            this.fieldDefinition = fieldDefinition;
        }
    }

    protected enum Handler {
        Location("""
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
                      name: "country",
                      type: "text"
                    },
                    {
                      name: "city",
                      type: "text"
                    }
                  ]
                }"""),

        Client("""
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
                }"""),

        Employee("""
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
                }"""),

        EmployeeRole("""
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
                }"""),

        Project(""" 
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
                }""");

        final String dsDefinition;

        Handler(String dsDefinition) {
            this.dsDefinition = dsDefinition;
        }
    }

    protected JdbcDataSource jdbcDataSource = new JdbcDataSource();
    private IDSRegistry dsRegistry;
    private Connection connection;
    private JDBCHandler.JDBCPolicy jdbcPolicy;
    protected H handler;


    @BeforeAll
    public static void setupDB() {
        JsonTestSupport.defaultMapper = JsonSerde.createMapper();
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

    protected static <H extends JDBCHandler> H withExtraFields(H h, ExtraField... extraFields) {
        for (ExtraField ef : extraFields) {
            final List<DSField> ofs = new LinkedList<>(h.getDataSource().getFields());
            final List<DSField> efs = JsonTestSupport.fromJSON(new TypeReference<>() {
            }, ef.fieldDefinition);
            ofs.addAll(efs);

            h.getDataSource().setFields(ofs);
        }
        return h;
    }

    protected void withExtraFields(ExtraField... extraFields) {
        withExtraFields(handler, extraFields);
    }

    protected H withHandler(String dataSourceDefinition) throws Exception {
        final DataSource ds = JsonTestSupport.fromJSON(DataSource.class, dataSourceDefinition);
        return doInitHandler(ds);
    }

    protected H withHandlers(Handler... handlers) throws Exception {
        H handler = null;
        for (Handler h:handlers) {
            handler = withHandler(h.dsDefinition);
        }
        return handler;
    }
}

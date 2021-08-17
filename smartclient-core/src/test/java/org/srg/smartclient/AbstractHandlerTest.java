package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.runtime.IDSRuntime;
import org.srg.smartclient.utils.Serde;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractHandlerTest<H extends DSHandler> {
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

    protected static class ExtraFieldBase {
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

        // ManyToMany
        public static String Project_IncludeTeamMembersFromFromEmployee = """
                    [
                        {
                           name:'teamMembers',
                           type:'ENTITY',
                           foreignKey:'EmployeeDS.id',
                           foreignDisplayField:'name',
                           displayField:'projectName',
                           dbName:'project_id',
                           tableName:'project_team',
                           multiple:true,
                           joinTable:{
                              tableName:'project_team',
                              sourceColumn:'project_id',
                              destColumn:'employee_id'
                           }
                        }
                    ]
                """;

        // OneToMany
        public static String Employee_RolesFromEmployeeRole = """
                [
                    {
                        name:"roles"
                        ,foreignKey:"EmployeeRoleDS.employee"
                        ,type:"EmployeeRoleDS"
                        ,foreignDisplayField:"role"
                        ,multiple:true
                    }
                ]""";

    }

    protected IDSRuntime dsRuntime;
    protected H handler;

    @BeforeAll
    public static void setupDB() {
        JsonTestSupport.defaultMapper = Serde.createMapper();
    }

    @BeforeEach
    public void setupMockitoHooks() throws Exception {
        dsRuntime = Mockito.mock(IDSRuntime.class);
        handler = withHandlers(Handler.Employee);
    }

    @AfterEach
    public void unsetMockitoHooks() {
        Mockito.reset(dsRuntime);
        dsRuntime = null;

        Mockito.reset(handler);
        handler = null;
    }

    protected Class<H> getHandlerClass() {
        throw new IllegalStateException("AbstractHandlerTest.getHandlerClass() must be overridden in order to uase it.");
    }

    protected JDBCHandler.JDBCPolicy getJDJdbcPolicy() {
        throw new IllegalStateException("AbstractHandlerTest.getJDJdbcPolicy() must be overridden in order to uase it.");
    }

    protected H doInitHandler(DataSource ds) throws Exception {
        final Class<H> hc = getHandlerClass();

        final H handler;
        if (hc.equals(DSHandler.class)) {
            handler = Mockito.mock(hc);

            Mockito.doReturn(ds)
                    .when(handler)
                    .dataSource();
        } else {
            final Constructor<H> constructor = hc.getConstructor(JDBCHandler.JDBCPolicy.class, IDSRuntime.class, DataSource.class);
            final H instance = constructor.newInstance(getJDJdbcPolicy(), dsRuntime, ds);
            handler = Mockito.spy(instance);
        }

        Mockito.doReturn(ds)
                .when(dsRuntime)
                .getDataSourceById(
                        Mockito.matches(ds.getId())
                );

        Mockito.doReturn(handler)
                .when(dsRuntime)
                .getDataSourceHandlerById(
                        Mockito.matches(ds.getId())
                );

        Mockito.doReturn(handler)
                .when(dsRuntime)
                .getHandlerById(
                        Mockito.matches(ds.getId())
                );

        Mockito.when(dsRuntime.getImportFromRelation(Mockito.matches(ds.getId()),Mockito.anyString()))
                .thenAnswer( (inv) -> {
                    String dsId = inv.getArgument(0);
                    assert dsId.equals(ds.getId());

                    String fieldName = inv.getArgument(1);
                    DSField dsf = ds.getField(fieldName);
                    return RelationSupport.describeImportFrom(dsRuntime, ds, dsf);
                });

        Mockito.when(dsRuntime.getForeignKeyRelation(Mockito.matches(ds.getId()),Mockito.anyString()))
                .thenAnswer( (inv) -> {
                    String dsId = inv.getArgument(0);
                    assert dsId.equals(ds.getId());

                    String fieldName = inv.getArgument(1);
                    DSField dsf = ds.getField(fieldName);
                    return RelationSupport.describeForeignKey(dsRuntime, ds, dsf);
                });

        return handler;
    }

    protected static <H extends DSHandler> H withExtraFields(H h, String... extraFields) {
        for (String ef : extraFields) {
            final List<DSField> ofs = new LinkedList<>(h.dataSource().getFields());
            final List<DSField> efs = JsonTestSupport.fromJSON(new TypeReference<>() {}, ef);
            ofs.addAll(efs);

            h.dataSource().setFields(ofs);
        }

        return h;
    }

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

package org.srg.smartclient;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSField;

class RelationSupportTest extends AbstractHandlerTest<DSHandler> {

    protected enum IncludeFrom_TestCases {
        Direct_Without_IncludeVia(){
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                test.withHandlers(Handler.Location);

                return test.withExtraFields("""
                    [
                        {
                            name:'location',
                            foreignKey:'LocationDS.id',
                            dbName:'location_id',
                            displayField: 'location_city'
                        },
                        {
                            name:'location_city',
                            type:'TEXT',
                            includeFrom:'LocationDS.city'
                        }
                    ]"""
                );
            }
        },

        Two_Direct_Same_The_Same_DisplayField_With_IncludeVia(){
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H projectHandler =  test.withHandlers(Handler.Project);

                withExtraFields(projectHandler,
                        """
                        [
                             {
                                 name:"manager"
                                 ,foreignKey:"EmployeeDS.id"
                                 ,displayField:"managerFullName"
                                 ,foreignDisplayField:"fullName"
                                 ,dbName:"manager_id"
                             },
                             {
                                 name:"managerFullName"
                                 ,type:"TEXT"
                                 ,includeFrom:"EmployeeDS.name"
                                 ,includeVia:"manager"
                             },
                             
                             {
                                 name:"supervisor"
                                 ,foreignKey:"EmployeeDS.id"
                                 ,displayField:"supervisorFullName"
                                 ,foreignDisplayField:"fullName"
                                 ,dbName:"supervisor_id"
                             },
                             {
                                 name:"supervisorFullName"
                                 ,type:"TEXT"
                                 ,includeFrom:"EmployeeDS.name"
                                 ,includeVia:"supervisor"
                             }
        
                        ]""");
                return projectHandler;
            }
        },

        Direct_With_IncludeVia() {
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H projectHandler =  test.withHandlers(Handler.Project);
                test.withExtraFields(AbstractJDBCHandlerTest.ExtraField.SqlCalculated);
                withExtraFields(projectHandler, AbstractJDBCHandlerTest.ExtraField.Project_IncludeManagerFromEmployee);

                return projectHandler;
            }
        },

        Direct_Multiple_OneToMany_With_IncludeVia() {
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H h = test.withHandlers(Handler.EmployeeRole, Handler.Employee);
                withExtraFields(h,
                        """
                                   [{
                                       name:'concatRoles',
                                       type:'TEXT',
                                       includeFrom:'EmployeeRoleDS.role',
                                       includeSummaryFunction:'CONCAT',
                                       multiple:true
                                   }]
                                """,
                        ExtraFieldBase.Employee_RolesFromEmployeeRole
                );

                return h;
            }
        },

        Direct_Multiple_ManyToMany_With_IncludeVia() {
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H h = test.withHandlers(Handler.Project);
                withExtraFields(h,
                        """
                                   [{
                                       name:'employeeName',
                                       type:'TEXT',
                                       includeFrom:'EmployeeDS.name',
                                       includeVia:'teamMembers',
                                       includeSummaryFunction:'CONCAT',
                                       dbName:"Employee.name",
                                       multiple:true,
                                       includeSummaryFunction: 'CONCAT',
                                       customSQL:false
                                   }]
                                """,
                        ExtraFieldBase.Project_IncludeTeamMembersFromFromEmployee
                );

                return h;
            }
        },

        Indirect_Without_IncludeVia() {
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H locationHandler = test.withHandlers(Handler.Country, Handler.Location);

                withExtraFields(locationHandler, """
                    [
                        {
                          name: 'country',
                          dbName: 'country_id',
                          type: 'integer',
                          foreignKey:'CountryDS.id'
                        }
                    ]"""
                );

                return test.withExtraFields("""
                    [
                        {
                            name:'location',
                            foreignKey:'LocationDS.id',
                            dbName:'location_id',
                            displayField: 'location_country'
                        },
                        {
                            name:'location_country',
                            type:'TEXT',
                            includeFrom:'LocationDS.country.CountryDS.name'
                        }
                    ]"""
                );
            }
        },
        Indirect_With_IncludeVia() {
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H locationHandler = test.withHandlers(Handler.Country, Handler.Location);
                withExtraFields(locationHandler, """
                    [
                        {
                          name: "country",
                          dbName: "country_id",
                          type: "integer",
                          foreignKey:"CountryDS.id"
                        }
                    ]"""
                );

                return test.withExtraFields("""
                    [
                        {
                            name:'location'
                            , foreignKey:'LocationDS.id'
                            , dbName:'location_id'
                            , displayField: 'location_country'
                        },
                        {
                            name:'location_country'
                            , type:'TEXT'
                            , includeFrom:'LocationDS.country.CountryDS.name'
                            , includeVia:'location'
                        }
                    ]"""
                );
            }
        };

        public abstract <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception;
    }
    @Test
    public void importFromRelation_directIncludeFrom_without_includeVia() throws Exception {
        final DSHandler h = IncludeFrom_TestCases.Direct_Without_IncludeVia.apply(this);

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                dsRuntime,
                h.dataSource(),
                h.dataSource().getField("location_city")
        );

        JsonTestSupport.assertJsonEquals("""
                {
                   dataSource:'EmployeeDS',
                   sourceField: 'location_city',
                   foreignKeyRelations: [
                      {
                         dataSource:'EmployeeDS',
                         sourceField: 'location',
                         isInverse:false,
                         foreign:{
                            dataSource:'LocationDS',
                            field: 'id',
                            sqlFieldAlias:null,
                            relatedTableAlias:'location_locations'
                         }
                      }
                   ],
                   foreignDisplay: 'city'
                }""",
                ifr
        );
    }

    @Test
    public void importFromRelation_indirectIncludeFrom_without_includeVia() throws Exception {
        final DSHandler h = IncludeFrom_TestCases.Indirect_Without_IncludeVia.apply(this);

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                dsRuntime,
                h.dataSource(),
                h.dataSource().getField("location_country")
        );

        JsonTestSupport.assertJsonEquals("""
            {
               dataSource:'EmployeeDS',
               sourceField: 'location_country',
               foreignDisplay: 'name',
               foreignKeyRelations:[
                  {
                     dataSource:'EmployeeDS',
                     sourceField: 'location',
                     foreign:{
                        dataSource:'LocationDS',
                        field:'id',
                        sqlFieldAlias:null,
                        relatedTableAlias:'location_locations'
                     },
                     isInverse:false
                  },
                  {
                     dataSource:'LocationDS',
                     sourceField: 'country',
                     foreign:{
                        dataSource:'CountryDS',
                        field: 'id',
                        sqlFieldAlias:null,
                        relatedTableAlias:'country_countries'
                     },
                     isInverse:false
                  }
               ]
            }""",
            ifr
        );
    }

    @Test
    public void importFromRelation_indirectIncludeFrom_with_includeVia() throws Exception {
        final DSHandler h = IncludeFrom_TestCases.Indirect_With_IncludeVia.apply(this);

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                dsRuntime,
                h.dataSource(),
                h.dataSource().getField("location_country")
        );

        JsonTestSupport.assertJsonEquals("""
            {
               dataSource:'EmployeeDS',
               sourceField: 'location_country',
               foreignDisplay: 'name',
               foreignKeyRelations:[
                  {
                     dataSource:'EmployeeDS',
                     sourceField:'location',
                     foreign:{
                        dataSource:'LocationDS',
                        field: 'id',
                        sqlFieldAlias:null,
                        relatedTableAlias:'location_locations'
                     },
                     isInverse:false
                  },
                  {
                     dataSource:'LocationDS',
                     sourceField: 'country',
                     foreign:{
                        dataSource:'CountryDS',
                        field: 'id',
                        sqlFieldAlias:null,
                        relatedTableAlias:'country_countries'
                     },
                     isInverse:false
                  }
               ]
            }""",
            ifr
        );
    }

    @Test
    public void importFromRelation_directIncludeFrom_with_includeVia() throws Exception {
        final DSHandler h = IncludeFrom_TestCases.Direct_With_IncludeVia.apply(this);

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                dsRuntime,
                h.dataSource(),
                h.dataSource().getField("employeeFullName")
        );

        JsonTestSupport.assertJsonEquals("""
            {
                dataSource: 'ProjectDS',
                sourceField: 'employeeFullName',
                foreignDisplay: 'calculated',
                foreignKeyRelations: [
                    {
                        dataSource: 'ProjectDS',
                        sourceField: 'manager',
                        isInverse: false,
                        foreign: {
                            dataSource: 'EmployeeDS',
                            field: 'id',
                            sqlFieldAlias: null,
                            relatedTableAlias: 'manager_employee'
                        }
                    }
                ]
            }""", ifr
        );
    }

    @Test
    public void importFromRelation_directIncludeFrom_multiple_with_includeVia() throws Exception {
        final DSHandler h = IncludeFrom_TestCases.Direct_Multiple_ManyToMany_With_IncludeVia.apply(this);

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                dsRuntime,
            h.dataSource(),
            h.dataSource().getField("employeeName")
        );

        JsonTestSupport.assertJsonEquals("""
            {
                dataSource: 'ProjectDS',
                sourceField: 'employeeName',
                foreignDisplay: 'name',
                foreignKeyRelations: [
                    {
                        dataSource: 'ProjectDS',
                        sourceField: 'teamMembers',
                        isInverse: false,
                        foreign: {
                            dataSource: 'EmployeeDS',
                            field: 'id',
                            sqlFieldAlias: null,
                            relatedTableAlias: 'teamMembers_employee'
                        }
                    }
                ]
            }""", ifr
        );

        final RelationSupport.ForeignKeyRelation  fkr = ifr.toForeignKeyRelation();
        JsonTestSupport.assertJsonEquals("""
            {
                dataSource:'ProjectDS',
                sourceField:'teamMembers',
                isInverse:false,
                foreign: {
                    dataSource:'EmployeeDS',
                    field:'id',
                    sqlFieldAlias:null,
                    relatedTableAlias: null
                }
            }""", fkr
        );

        final RelationSupport.ForeignKeyRelation fkrDisplay = ifr.toForeignDisplayKeyRelation();
        JsonTestSupport.assertJsonEquals("""
            {
                dataSource:'ProjectDS',
                sourceField:'teamMembers',
                isInverse:false,
                foreign: {
                    dataSource:'EmployeeDS',
                    field:'name',
                    sqlFieldAlias:null,
                    relatedTableAlias: null
                }
            }""", fkrDisplay
        );
    }

    @Test
    public void importFromRelation_multipleDirectIncludeFromTheSameTable_with_includeVia() throws Exception {
        final DSHandler h = IncludeFrom_TestCases.Two_Direct_Same_The_Same_DisplayField_With_IncludeVia.apply(this);
        {
            final DSField includeFrom = h.dataSource().getField("managerFullName");
            JsonTestSupport.assertJsonEquals("""
                    {
                        name: 'managerFullName',
                        includeFrom: 'EmployeeDS.name',
                        includeVia: 'manager'
                    }""", includeFrom, Option.IGNORING_EXTRA_FIELDS);


            final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                    dsRuntime,
                    h.dataSource(),
                    includeFrom
            );

            JsonTestSupport.assertJsonEquals("""
                    {
                        dataSource: 'ProjectDS',
                        sourceField: 'managerFullName',
                        foreignDisplay: 'name',
                        foreignKeyRelations: [
                            {
                                dataSource: 'ProjectDS',
                                sourceField: 'manager',
                                isInverse: false,
                                foreign: {
                                    dataSource: 'EmployeeDS',
                                    field: 'id',
                                    sqlFieldAlias: null,
                                    relatedTableAlias: 'manager_employee'
                                }
                            }
                        ]
                    }""", ifr
            );
        }

        final DSField includeFrom = h.dataSource().getField("supervisorFullName");
        JsonTestSupport.assertJsonEquals("""
                    {
                        name: 'supervisorFullName',
                        includeFrom: 'EmployeeDS.name',
                        includeVia: 'supervisor'
                    }""", includeFrom, Option.IGNORING_EXTRA_FIELDS);


        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                dsRuntime,
                h.dataSource(),
                includeFrom
        );

        JsonTestSupport.assertJsonEquals("""
                    {
                        dataSource: 'ProjectDS',
                        sourceField: 'supervisorFullName',
                        foreignDisplay: 'name',
                        foreignKeyRelations: [
                            {
                                dataSource: 'ProjectDS',
                                sourceField: 'supervisor',
                                isInverse: false,
                                foreign: {
                                    dataSource: 'EmployeeDS',
                                    field: 'id',
                                    sqlFieldAlias: null,
                                    relatedTableAlias: 'supervisor_employee'
                                }
                            }
                        ]
                    }""", ifr
        );
    }

    @Override
    protected Class<DSHandler> getHandlerClass() {
        return DSHandler.class;
    }
}

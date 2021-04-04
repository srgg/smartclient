package org.srg.smartclient;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
        Direct_With_IncludeVia() {
            @Override
            public <H extends DSHandler> H apply(AbstractHandlerTest<H> test) throws Exception {
                final H projectHandler =  test.withHandlers(Handler.Project);
                test.withExtraFields(AbstractJDBCHandlerTest.ExtraField.SqlCalculated);
                withExtraFields(projectHandler, AbstractJDBCHandlerTest.ExtraField.Project_IncludeManagerFromEmployee);

                return projectHandler;
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
                dsRegistry,
                h.dataSource(),
                h.dataSource().getField("location_city")
        );

        JsonTestSupport.assertJsonEquals("""
                {
                   dataSource:'EmployeeDS',
                   sourceField: 'location',
                   foreignKeyRelations: [
                      {
                         dataSource:'EmployeeDS',
                         sourceField: 'location',
                         isInverse:false,
                         foreign:{
                            dataSource:'LocationDS',
                            field: 'id',
                            sqlFieldAlias:null
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
                dsRegistry,
                h.dataSource(),
                h.dataSource().getField("location_country")
        );

        JsonTestSupport.assertJsonEquals("""
            {
               dataSource:'EmployeeDS',
               sourceField: 'location',
               foreignDisplay: 'name',
               foreignKeyRelations:[
                  {
                     dataSource:'EmployeeDS',
                     sourceField: 'location',
                     foreign:{
                        dataSource:'LocationDS',
                        field:'id',
                        sqlFieldAlias:null
                     },
                     isInverse:false
                  },
                  {
                     dataSource:'LocationDS',
                     sourceField: 'country',
                     foreign:{
                        dataSource:'CountryDS',
                        field: 'id',
                        sqlFieldAlias:null
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
                dsRegistry,
                h.dataSource(),
                h.dataSource().getField("location_country")
        );

        JsonTestSupport.assertJsonEquals("""
            {
               dataSource:'EmployeeDS',
               sourceField: 'location',
               foreignDisplay: 'name',
               foreignKeyRelations:[
                  {
                     dataSource:'EmployeeDS',
                     sourceField:'location',
                     foreign:{
                        dataSource:'LocationDS',
                        field: 'id',
                        sqlFieldAlias:null
                     },
                     isInverse:false
                  },
                  {
                     dataSource:'LocationDS',
                     sourceField: 'country',
                     foreign:{
                        dataSource:'CountryDS',
                        field: 'id',
                        sqlFieldAlias:null
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
                dsRegistry,
                h.dataSource(),
                h.dataSource().getField("employeeFullName")
        );

        JsonTestSupport.assertJsonEquals("""
            {
                dataSource: 'ProjectDS',
                sourceField: 'manager',
                foreignDisplay: 'calculated',
                foreignKeyRelations: [
                    {
                        dataSource: 'ProjectDS',
                        sourceField: 'manager',
                        isInverse: false,
                        foreign: {
                            dataSource: 'EmployeeDS',
                            field: 'id',
                            sqlFieldAlias: null
                        }
                    }
                ]
            }""", ifr);
    }

    @Disabled
    @Test
    public void importFromRelation_on_include_field_with_multi() throws Exception {
        final DSHandler projectHandler =  withHandlers(Handler.Project);
        withExtraFields(projectHandler,
            """
               [{
                   name:'employeeName',
                   type:'TEXT',
                   includeFrom:'EmployeeDS.name',
                   includeVia:'teamMembers',
                   includeSummaryFunction:'CONCAT',
                   dbName:"Employee.name",
                   multiple:true,
                   customSQL:false
               }]
            """);

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
            dsRegistry,
            projectHandler.dataSource(),
            projectHandler.dataSource().getField("employeeName")
        );

        JsonTestSupport.assertJsonEquals("{}", ifr);
    }

    @Override
    protected Class<DSHandler> getHandlerClass() {
        return DSHandler.class;
    }
}

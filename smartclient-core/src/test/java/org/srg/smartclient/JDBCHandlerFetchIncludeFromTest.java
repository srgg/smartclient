package org.srg.smartclient;

import net.javacrumbs.jsonunit.core.Option;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

public class JDBCHandlerFetchIncludeFromTest extends AbstractJDBCHandlerTest<JDBCHandler> {

    @Override
    protected Class<JDBCHandler> getHandlerClass() {
        return JDBCHandler.class;
    }

    @Test
    public void directIncludeFrom_without_includeVia() throws Exception {
        final JDBCHandler h = RelationSupportTest.IncludeFrom_TestCases.Direct_Without_IncludeVia.apply(this);

        final DSField includeFrom = h.getField("location_city");

        final RelationSupport.ImportFromRelation ifr = RelationSupport.describeImportFrom(
                h::getDataSourceHandlerById,
                h.dataSource(),
                includeFrom
        );

        // --
        final RelationSupport.ForeignRelation frl = h.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'LocationDS',
                    field: 'city',
                    sqlFieldAlias:null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifr.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        LEFT JOIN locations ON employee.location_id = locations.id        
                    """
                )
        );

        // --
        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);

        final DSResponse response = h.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               status:0,
               startRow:0,
               endRow:3,
               totalRows:6,
               data:[
                    {
                       id:1,
                       name:'admin',
                       location:1,
                       location_city:'Kharkiv'
                    },
                    {
                       id:2,
                       name:'developer',
                       location:2,
                       location_city:'Lviv'
                    },
                    {
                       id:3,
                       name:'UseR3',
                       location:3,
                       location_city:'USA'
                    }
               ]
            }""", response);
    }

    @Test
    public void indirectIncludeFrom_without_includeVia() throws Exception {
        final JDBCHandler h = RelationSupportTest.IncludeFrom_TestCases.Indirect_Without_IncludeVia.apply(this);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);

        final DSField includeFrom = h.getField("location_country");

        final RelationSupport.ForeignRelation frl = h.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'CountryDS',
                    field: 'name',
                    sqlFieldAlias:null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );



        final RelationSupport.ImportFromRelation ifr = h.describeImportFrom(includeFrom);

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifr.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        LEFT JOIN locations ON employee.location_id = locations.id
                        LEFT JOIN countries ON locations.country_id = countries.id        
                    """
                )
        );

        final DSResponse response = h.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               status:0,
               startRow:0,
               endRow:3,
               totalRows:6,
               data:[
                   {
                      id:1,
                      location:1,
                      location_country:'Ukraine',
                      name:'admin'
                   },
                   {
                      id:2,
                      location:2,
                      location_country:'Ukraine',
                      name:'developer'
                   },
                   {
                      id:3,
                      location:3,
                      location_country:'USA',
                      name:'UseR3'
                   }               
               ]
            }""", response);
    }

    @Test
    public void indirectIncludeFrom_with_includeVia() throws Exception {
        final JDBCHandler h = RelationSupportTest.IncludeFrom_TestCases.Indirect_With_IncludeVia.apply(this);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);

        final DSField includeFrom = h.getField("location_country");

        final RelationSupport.ForeignRelation frl = h.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'CountryDS',
                    field: 'name',
                    sqlFieldAlias:null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );



        final RelationSupport.ImportFromRelation ifr = h.describeImportFrom(includeFrom);

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifr.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        LEFT JOIN locations ON employee.location_id = locations.id
                        LEFT JOIN countries ON locations.country_id = countries.id        
                    """
                )
        );

        final DSResponse response = h.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               status:0,
               startRow:0,
               endRow:3,
               totalRows:6,
               data:[
                   {
                      id:1,
                      location:1,
                      location_country:'Ukraine',
                      name:'admin'
                   },
                   {
                      id:2,
                      location:2,
                      location_country:'Ukraine',
                      name:'developer'
                   },
                   {
                      id:3,
                      location:3,
                      location_country:'USA',
                      name:'UseR3'
                   }               
               ]
            }""", response);
    }

    @Regression("org.h2.jdbc.JdbcSQLSyntaxErrorException: Column 'countries.name' not found. Caused by improper SQL JOIN  Clause generation")
    @Test
    public void multipleIndirectIncludeFrom_with_the_same_includeVia() throws Exception {
        final JDBCHandler locationHandler = withHandlers(Handler.Country, Handler.Location);
        withExtraFields(locationHandler, """
            [
                {
                  name: "country",
                  dbName: "country_id",
                  type: "integer",
                  foreignKey:"CountryDS.id"
                }
            ]""");

        final JDBCHandler employeeHandler = withExtraFields(
                """
                    [
                        {
                            name:'location'
                            , foreignKey:'LocationDS.id'
                            , dbName:'location_id'
                            , displayField: 'location_country'
                        },
                        {
                            name:'location_city'
                            , type:'TEXT'
                            , includeFrom:'LocationDS.city'
                            , includeVia:'location'
                        },
                        {
                            name:'location_country'
                            , type:'TEXT'
                            , includeFrom:'LocationDS.country.CountryDS.name'
                            , includeVia:'location'
                        }
                    ]
                """);
        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);

        final DSResponse response = employeeHandler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               status:0,
               startRow:0,
               endRow:3,
               totalRows:6,
               data:[
                   {
                      id:1,
                      location:1,
                      location_country:'Ukraine',
                      location_city:'Kharkiv',
                      name:'admin'
                   },
                   {
                      id:2,
                      location:2,
                      location_country:'Ukraine',
                      location_city:'Lviv',                      
                      name:'developer'
                   },
                   {
                      id:3,
                      location:3,
                      location_country:'USA',
                      location_city:'USA',                      
                      name:'UseR3'
                   }               
               ]
            }""", response);
    }

    @Test
    public void fetchManyToMany_EntireEntity_WithForeignDisplayField() throws Exception {
        final JDBCHandler h = RelationSupportTest.IncludeFrom_TestCases.Direct_Multiple_With_IncludeVia.apply(this);

        final DSRequest request = new DSRequest();
        request.setOutputs("id, name, teamMembers, employeeName");


        final DSField includeFrom = h.getField("employeeName");

        final RelationSupport.ForeignRelation frl = h.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'EmployeeDS',
                    field: 'name',
                    sqlFieldAlias:null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );



        final RelationSupport.ImportFromRelation ifr = h.describeImportFrom(includeFrom);

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifr.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        JOIN project_team  ON employee.id = project_team.employee_id
                        JOIN project project ON project_team.project_id = project.id                                                                          
                        """
                )
        );


        final DSResponse response = h.handleFetch(request);

//        /*
//         * It is expected that ForeignDisplayField will be returned as part of foreign entity
//         */
        JsonTestSupport.assertJsonEquals("""
                {
                  status:0,
                  startRow:0,
                  endRow:5,
                  totalRows:5,
                  data:[
                    {
                      id:1,
                      name:'Project 1 for client 1',
                      teamMembers:[
                         {
                            id:1
                         },
                         {
                            id:2
                         }
                      ],
                      employeeName:'admin, developer'
                    },
                    {
                      id:2,
                      name:'Project 2 for client 1',
                      teamMembers:[
                         {
                            id:2
                         },
                         {
                            id:3
                         }
                      ],
                      employeeName:'developer, UseR3'
                    },
                    {
                      id:3,
                      name:'Project 1 for client 2',
                      teamMembers:[
                         {
                            id:4
                         },
                         {
                            id:5
                         }
                      ],
                      employeeName:'manager1, manager2'
                    },
                    {
                      id:4,
                      name:'Project 2 for client 2',
                      teamMembers:null,
                      employeeName:null
                    },
                    {
                      id:5,
                      name:'Project 3 for client 2',
                      teamMembers:null,
                      employeeName:null
                    }
                  ]
                }""", response);
    }
}

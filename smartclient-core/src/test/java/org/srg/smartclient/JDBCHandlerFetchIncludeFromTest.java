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
        final String extra = """
                [
                    {
                        name:"location"
                        , foreignKey:"LocationDS.id"
                        , dbName:"location_id"
                        , displayField: 'location_city'                        
                    },
                    {
                        name:"location_city"
                        , type:"TEXT"
                        , includeFrom:"LocationDS.city"
                    }
                ]""";

        withHandlers(Handler.Location);
        final JDBCHandler employeeHandler = withExtraFields(extra);

        // -- Check validity ImportFromRelation
        final DSField includeFrom = employeeHandler.getField("location_city");
        final RelationSupport.ImportFromRelation ifrl = RelationSupport.describeImportFrom(dsId -> employeeHandler.getDataSourceHandlerById(dsId), employeeHandler.dataSource(), includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                   dataSource:'EmployeeDS',
                   field:{
                      name:'location',
                      foreignKey:'LocationDS.id',
                      displayField:'location_city',
                      dbName:'location_id',
                      includeField:false
                   },
                   foreignKeyRelations: [
                      {
                         dataSource:'EmployeeDS',
                         sourceField:{
                            name:'location',
                            foreignKey:'LocationDS.id',
                            displayField:'location_city',
                            dbName:'location_id',
                            includeField:false
                         },
                         isInverse:false,
                         foreign:{
                            dataSource:'LocationDS',
                            field:{
                               name:'id',
                               required:true,
                               primaryKey:true,
                               dbName:'id'
                            },
                            sqlFieldAlias:null
                         }
                      }
                   ],
                   foreignDisplay:{
                      name:'city',
                      dbName:'city',
                      type: 'TEXT',
                      foreignKey: null,                      
                      includeField:false
                   }
                }""",
                ifrl,
                Option.IGNORING_EXTRA_FIELDS
        );

        // --
        final RelationSupport.ForeignRelation frl = employeeHandler.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'LocationDS',
                    field:{
                       name:'city',
                       type:'TEXT',
                       includeField:false
                    },
                    "sqlFieldAlias":null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifrl.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        JOIN locations ON employee.location_id = locations.id        
                    """
                )
        );

        // --
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
                            name:'location_country'
                            , type:'TEXT'
                            , includeFrom:'LocationDS.country.CountryDS.name'
                        }
                    ]
                """);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);

        final DSField includeFrom = employeeHandler.getField("location_country");

        final RelationSupport.ForeignRelation frl = employeeHandler.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'CountryDS',
                    field:{
                       name:'name',
                       type:'TEXT',
                       includeField:false
                    },
                    "sqlFieldAlias":null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );



        final RelationSupport.ImportFromRelation ifrl = employeeHandler.describeImportFrom(includeFrom);

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifrl.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        JOIN locations ON employee.location_id = locations.id
                        JOIN countries ON locations.country_id = countries.id        
                    """
                )
        );

        // -- Check validity ImportFromRelation
        JsonTestSupport.assertJsonEquals("""
                {
                   dataSource:'EmployeeDS',
                   field:{
                      name:'location',
                      foreignKey:'LocationDS.id',
                      displayField:'location_country',
                      dbName:'location_id',
                      includeField:false
                   },
                   foreignDisplay:{
                      name:'name',
                      type:'TEXT',
                      dbName:'name',
                      foreignKey:null,
                      includeField:false
                   },
                   foreignKeyRelations:[
                      {
                         dataSource:'EmployeeDS',
                         foreign:{
                            dataSource:'LocationDS',
                            field:{
                               name:'id',
                               dbName:'id',
                               includeField:false,
                               primaryKey:true
                            },
                            sqlFieldAlias:null
                         },
                         isInverse:false,
                         sourceField:{
                            name:'location',
                            dbName:'location_id',
                            displayField:'location_country',
                            foreignKey:'LocationDS.id',
                            includeField:false
                         }
                      },
                      {
                         dataSource:'LocationDS',
                         foreign:{
                            dataSource:'CountryDS',
                            field:{
                               name:'id',
                               dbName:'id',
                               includeField:false,
                               'primaryKey':true
                            },
                            sqlFieldAlias:null
                         },
                         isInverse:false,
                         sourceField:{
                            name:'country',
                            dbName:'country_id',
                            foreignKey:'CountryDS.id',
                            includeField:false,
                            type:'INTEGER'
                         }
                      }
                   ]
                }""",
                ifrl,
                Option.IGNORING_EXTRA_FIELDS
        );

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

        final DSField includeFrom = employeeHandler.getField("location_country");

        final RelationSupport.ForeignRelation frl = employeeHandler.determineEffectiveField(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                    dataSource:'CountryDS',
                    field:{
                       name:'name',
                       type:'TEXT'
                       , includeField:false
                    },
                    "sqlFieldAlias":null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );



        final RelationSupport.ImportFromRelation ifrl = employeeHandler.describeImportFrom(includeFrom);

        // -- Check generated SQL join clause
        final String sqlJoin = JDBCHandler.AbstractSQLContext.generateSQLJoin(ifrl.foreignKeyRelations());

        MatcherAssert.assertThat( sqlJoin,
                equalToCompressingWhiteSpace("""
                        JOIN locations ON employee.location_id = locations.id
                        JOIN countries ON locations.country_id = countries.id        
                    """
                )
        );

        // -- Check validity ImportFromRelation
        JsonTestSupport.assertJsonEquals("""
                {
                   dataSource:'EmployeeDS',
                   field:{
                      name:'location',
                      foreignKey:'LocationDS.id',
                      displayField:'location_country',
                      dbName:'location_id',
                      includeField:false
                   },
                   foreignDisplay:{
                      name:'name',
                      type:'TEXT',
                      dbName:'name',
                      foreignKey:null,
                      includeField:false
                   },
                   foreignKeyRelations:[
                      {
                         dataSource:'EmployeeDS',
                         foreign:{
                            dataSource:'LocationDS',
                            field:{
                               name:'id',
                               dbName:'id',
                               includeField:false,
                               primaryKey:true
                            },
                            sqlFieldAlias:null
                         },
                         isInverse:false,
                         sourceField:{
                            name:'location',
                            dbName:'location_id',
                            displayField:'location_country',
                            foreignKey:'LocationDS.id',
                            includeField:false
                         }
                      },
                      {
                         dataSource:'LocationDS',
                         foreign:{
                            dataSource:'CountryDS',
                            field:{
                               name:'id',
                               dbName:'id',
                               includeField:false,
                               'primaryKey':true
                            },
                            sqlFieldAlias:null
                         },
                         isInverse:false,
                         sourceField:{
                            name:'country',
                            dbName:'country_id',
                            foreignKey:'CountryDS.id',
                            includeField:false,
                            type:'INTEGER'
                         }
                      }
                   ]
                }""",
                ifrl,
                Option.IGNORING_EXTRA_FIELDS
        );

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
}

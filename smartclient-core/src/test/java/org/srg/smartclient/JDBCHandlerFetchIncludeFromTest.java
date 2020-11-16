package org.srg.smartclient;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

public class JDBCHandlerFetchIncludeFromTest extends AbstractJDBCHandlerTest<JDBCHandler> {

    @Override
    protected Class<JDBCHandler> getHandlerClass() {
        return JDBCHandler.class;
    }

    @Test
    public void directIncludeFrom_Without_Via() throws Exception {
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
                   foreignDataSource:'LocationDS',
                   foreignKey:{
                      name:'id',
                      required:true,
                      primaryKey:true,
                      dbName:'id'
                   },
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

    @Disabled
    @Test
    public void indirectIncludeFrom() throws Exception {
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
                    dataSource:'LocationDS',
                    field:{
                       name:'country',
                       foreignKey:'CountryDS.id',
                       includeField:false
                    },
                    "sqlFieldAlias":null
                }""",
                frl,
                Option.IGNORING_EXTRA_FIELDS
        );


        final RelationSupport.ImportFromRelation ifrl = employeeHandler.describeImportFrom(includeFrom);
        JsonTestSupport.assertJsonEquals("""
                {
                   dataSource:'EmployeeDS',
                   field:{
                      name:'location',
                      foreignKey:'LocationDS.id',
                      displayField:'location_country',
                      includeField:false
                   },
                   foreignDataSource:'LocationDS',
                   foreignKey:{
                      name:'id',
                      required:true,
                      primaryKey:true
                   },
                   foreignDisplay:{
                      name:'country',
                      type:'INTEGER',
                      foreignKey:'CountryDS.id',
                      dbName:'country_id',
                      includeField:false
                   }
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
               ]
            }""", response);
    }

}

package org.srg.smartclient;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

public class JDBCHandlerFetchIncludeFromTest extends AbstractJDBCHandlerTest<JDBCHandler> {

    @Override
    protected Class<JDBCHandler> getHandlerClass() {
        return JDBCHandler.class;
    }

    @Test
    public void includeFrom_Without_Via() throws Exception {
        final String extra = """                
                [
                    {
                        name:"location"
                        , foreignKey:"LocationDS.id"
                        , dbName:"location_id"
                    },
                    {
                        name:"location_city"
                        , type:"TEXT"
                        , includeFrom:"LocationDS.city"
                    }
                ]""";

        withHandlers(Handler.Location);
        final JDBCHandler employeeHandler = withExtraFields(extra);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);
//        request.setSortBy(Arrays.asList("+id"));

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
        final JDBCHandler locationHandler = withHandlers(Handler.Location);
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
                            name:"location"
                            , foreignKey:"LocationDS.id"
                            , dbName:"location_id"
                        },
                        {
                            name:"location_country"
                            , type:"TEXT"
                            , includeFrom:"LocationDS.country.CountryDS.name"
                        }
                    ]
                """);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(3);
//        request.setSortBy(Arrays.asList("+id"));

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

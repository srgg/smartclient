package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JDBCHandlerTest {
    private static String LocationDSDescription = """
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
        }""";

    private static String UserDSDescription = """
        {
           id: 'usersDS',
           serverType: 'sql',
           tableName: 'users',
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

    private static String IncludeFromFields = """
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

    private static JdbcDataSource jdbcDataSource = new JdbcDataSource();
    private DataSource locationDS;
    private DataSource userDS;
    private JDBCHandler handler;

    @BeforeAll
    public static void setupDB() {
        JsonTestSupport.defaultMapper = JsonSerde.createMapper();

        jdbcDataSource.setURL("jdbc:h2:mem:test:˜/test;INIT=RUNSCRIPT FROM 'classpath:test-data.sql'");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("sa");
    }

    @AfterAll
    public static void shutdownDB() {
    }

    @BeforeEach
    public void setup() {
        locationDS = JsonTestSupport.fromJSON(DataSource.class, LocationDSDescription);
        userDS = JsonTestSupport.fromJSON(DataSource.class, UserDSDescription);

        handler = Mockito.spy(
                new JDBCHandler((database, callback) -> {
                    try (Connection connection = jdbcDataSource.getConnection()) {
                        callback.apply(connection);
                    }
                }, null, userDS)
        );
    }

    protected void addExtraFields(String jsonFieldsDescription) throws IOException {
        final List<DSField> extraFields = JsonTestSupport.fromJSON(new TypeReference<>() {}, jsonFieldsDescription);

        handler.getDatasource()
                .getFields()
                .addAll(extraFields);


    }

    protected void addExtraFields(String...jsonFieldDescription) throws IOException {
        final String asJson = JsonTestSupport.asStrictJSON(jsonFieldDescription);
        addExtraFields(asJson);
    }

    @Test
    public void fetchAll() throws Exception {
        DSRequest request = new DSRequest();
        final DSResponse response = handler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
                 {
                     response: {
                         status: 0,
                         startRow: 0,
                         endRow: 5,
                         totalRows: 5,
                         data:[
                             {
                                 id:1,
                                 name: 'user1'
                             },
                             {
                                 id:2,
                                 name: 'user2'
                             },
                             {
                                 id:3,
                                 name: 'user3'
                             },
                             {
                                 id:4,
                                 name: 'user4'
                             },
                             {
                                 id:5,
                                 name: 'user5'
                             }
                         ]
                     }
                }""", response);
    }

    @Test
    public void fetchPaginated() throws Exception {
        // -- the 1'st page
        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(2);

        final DSResponse response1 = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
                {
                    response: {
                        status: 0,
                        startRow: 0,
                        endRow: 2,
                        totalRows: 5,
                        data:[
                            {
                                id:1,
                                name: 'user1'
                            },
                            {
                                id:2,
                                name: 'user2'
                            }
                        ]    
                    }
                }""", response1);

        // -- the 2-nd page
        request.setStartRow(2);
        request.setEndRow(4);
        final DSResponse response2 = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
                {
                    response: {
                        status: 0,
                        startRow: 2,
                        endRow: 4,
                        totalRows: 5,
                        data:[
                            {
                                id:3,
                                name: 'user3'
                            },
                            {
                                id:4,
                                name: 'user4'
                            }
                        ]    
                    }
                }""", response2);

        // the 3-rd page (The last one)
        request.setStartRow(4);
        request.setEndRow(6);
        final DSResponse response3 = handler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
                {
                    response: {
                        status: 0,
                        startRow: 4,
                        endRow: 5,
                        totalRows: 5,
                        data:[
                            {
                                id:5,
                                name: 'user5'
                            }
                        ]    
                    }
                }""", response3);
    }

    @Test
    public void fetchIncludeFromField() throws Exception {
        addExtraFields( IncludeFromFields);

        Mockito.doReturn(locationDS)
                .when(handler)
                .getDataSource(Mockito.anyString());

        final DSRequest request = new DSRequest();


        final DSResponse response = handler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
                 {
                     response: {
                         status: 0,
                         startRow: 0,
                         endRow: 5,
                         totalRows: 5,
                         data:[
                             {
                                 id:1,
                                 name: 'user1',
                                 location: 1,
                                 locationCity: 'Kharkiv'
                             },
                             {
                                 id:2,
                                 name: 'user2',
                                 location: 2,
                                 locationCity: 'Lviv'
                             },
                             {
                                 id:3,
                                 name: 'user3',
                                 location: 3,
                                 locationCity: 'USA'
                             },
                             {
                                 id:4,
                                 name: 'user4',
                                 location: 1,
                                 locationCity: 'Kharkiv'
                             },
                             {
                                 id:5,
                                 name: 'user5',
                                 location: 2,
                                 locationCity: 'Lviv'
                             }
                         ]
                     }
                }""", response);
    }

    @Test
    public void fetchPaginatedWithSort() throws Exception {

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(2);

        // -- check default order (should be ascending)
        request.setSortBy(Arrays.asList("name"));
        final DSResponse response1 = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
                response: {
                    status: 0,
                    startRow: 0,
                    endRow: 2,
                    totalRows: 5,
                    data:[
                        {
                            id:1,
                            name: 'user1'
                        },
                        {
                            id:2,
                            name: 'user2'
                        }
                    ]    
                }
            }""", response1);

        // -- check descending
        request.setSortBy(Arrays.asList("-name"));

        final DSResponse response2 = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
                response: {
                    status: 0,
                    startRow: 0,
                    endRow: 2,
                    totalRows: 5,
                    data:[
                        {
                            id:5,
                            name: 'user5'
                        },
                        {
                            id:4,
                            name: 'user4'
                        }
                    ]    
                }
            }""", response2);

        // -- check ascending
        request.setSortBy(Arrays.asList("+name"));
        final DSResponse response3 = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
                response: {
                    status: 0,
                    startRow: 0,
                    endRow: 2,
                    totalRows: 5,
                    data:[
                        {
                            id:1,
                            name: 'user1'
                        },
                        {
                            id:2,
                            name: 'user2'
                        }
                    ]    
                }
            }""", response3);
    }

    @Test
    public void fetchWithTextFilter() throws Exception {
        addExtraFields("""
                [{
                  name: "email",
                  type: "text"
                }]
            """);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(2);
        request.setTextMatchStyle(DSRequest.TextMatchStyle.SUBSTRING);
        request.wrapAndSetData(Map.of("email", "u5"));

        // -- check default order (should be ascending)
        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
                response: {
                    status: 0,
                    startRow: 0,
                    endRow: 1,
                    totalRows: 1,
                    data:[
                        {
                            id:5,
                            name: 'user5',
                            email: 'u5@acme.org'
                        }
                    ]    
                }
            }""", response);
    }

    @Test
    public void fetchWithIncludeFromFilter() throws Exception {
        addExtraFields(IncludeFromFields);

        Mockito.doReturn(locationDS)
                .when(handler)
                .getDataSource(Mockito.anyString());


        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(2);
        request.setTextMatchStyle(DSRequest.TextMatchStyle.SUBSTRING);
        request.wrapAndSetData(Map.of("location", 1));

        // -- check default order (should be ascending)
        final DSResponse response1 = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
                response: {
                    status: 0,
                    startRow: 0,
                    endRow: 2,
                    totalRows: 2,
                    data:[
                        {
                            id: 1,
                            name: 'user1',
                            location: 1,
                            locationCity: 'Kharkiv'
                        },
                        {
                            id: 4,
                            name: 'user4',
                            location: 1,
                            locationCity: 'Kharkiv'
                        }
                    ]    
                }
            }""", response1);
    }

//    @Test
//    public void fetchWithAdvancedCriteria() throws Exception {
//        DSRequest request = new DSRequest();
//
//                """
//
//
//    }

//    "data" : {
//        "operator" : "and",
//                "_constructor" : "AdvancedCriteria",
//                "criteria" : [ {
//            "fieldName" : "firedAt",
//                    "operator" : "notBlank",
//                    "_constructor" : "AdvancedCriteria"
//        } ]
}

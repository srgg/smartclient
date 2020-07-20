package org.srg.smartclient;

import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import java.util.Arrays;
import java.util.Map;

public class JDBCHandlerTest extends AbstractJDBCHandlerTest<JDBCHandler> {

    @Override
    protected Class<JDBCHandler> getHandlerClass() {
        return JDBCHandler.class;
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
                                 name: 'admin'
                             },
                             {
                                 id:2,
                                 name: 'developer'
                             },
                             {
                                 id:3,
                                 name: 'UseR3'
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
    public void fetchSpecifiedFields() throws Exception {
        withExtraFields(ExtraField.Email);

        DSRequest request = new DSRequest();
        request.setOutputs("id, email");
        final DSResponse response = handler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               response:{
                  status:0,
                  startRow:0,
                  endRow:5,
                  totalRows:5,
                  data:[
                     {
                        id:1,
                        email:'admin@acmE.org'
                     },
                     {
                        id:2,
                        email:'developer@acme.org'
                     },
                     {
                        id:3,
                        email:'u3@emca.org'
                     },
                     {
                        id:4,
                        email:'u4@acmE.org'
                     },
                     {
                        id:5,
                        email:'u5@acme.org'
                     }
                  ]
               }
            }""", response
        );
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
                                name: 'admin'
                            },
                            {
                                id:2,
                                name: 'developer'
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
                                name: 'UseR3'
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
        withExtraFields(ExtraField.IncludeFrom);
        withHandlers(Handler.Location);

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
                                 name: 'admin',
                                 location: 1,
                                 locationCity: 'Kharkiv'
                             },
                             {
                                 id:2,
                                 name: 'developer',
                                 location: 2,
                                 locationCity: 'Lviv'
                             },
                             {
                                 id:3,
                                 name: 'UseR3',
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
                            id:3,
                            name: 'UseR3'
                        },
                        {
                            id:1,
                            name: 'admin'
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
                            id:3,
                            name: 'UseR3'
                        },
                        {
                            id:1,
                            name: 'admin'
                        }
                    ]    
                }
            }""", response3);
    }

    @Test
    public void fetchWithTextFilter() throws Exception {
        withExtraFields(ExtraField.Email);

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
    public void fetchWithIncludeFromField() throws Exception {
        withExtraFields(ExtraField.IncludeFrom);
        withHandlers(Handler.Location);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(2);
        request.setTextMatchStyle(DSRequest.TextMatchStyle.SUBSTRING);
        request.wrapAndSetData(Map.of("location", 1));

        // -- check default order (should be ascending)
        final DSResponse response = handler.handleFetch(request);
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
                            name: 'admin',
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
            }""", response);
    }

    @Test
    public void fetchWithSQLCalculatedField() throws Exception {
        withExtraFields(ExtraField.SqlCalculated);

        DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setEndRow(2);

        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
                response:{
                    status:0,
                    startRow:0,
                    endRow:2,
                    totalRows:5,
                    data: [
                        {
                            id:1,
                            name:"admin",
                            calculated:"1_admin"
                        },
                        {
                            id:2,
                            name:"developer",
                            calculated:"2_developer"
                        }
                    ]
                }
            }""",
                response);

    }

    @Regression("Fails to fetch SQL Calculated Field via foreignDisplayField for a PK ")
    @Test
    public void fetchWithSQLCalculatedAsDisplayFieldForForeignRelation() throws Exception {
        final JDBCHandler h = withHandlers(Handler.EmployeeRole);
        withExtraFields(ExtraField.SqlCalculated);

        final DSRequest request = new DSRequest();
        final DSResponse response = h.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               response:{
                  status:0,
                  startRow:0,
                  endRow:3,
                  totalRows:3,
                  data:[
                     {
                        role:'Admin',
                        employee:1,
                        employeeCalculated:'1_admin'
                     },
                     {
                        role:'Developer',
                        employee:1,
                        employeeCalculated:'1_admin'
                     },
                     {
                        role:'Developer',
                        employee:2,
                        employeeCalculated:'2_developer'
                     }
                  ]
               }
            }""", response);
    }

    /**
     * Check case when a source Data Source does not have a correspondent FK field[s] and values
     * are mapped by so called "mappedBy" JPA mechanism using the source PK to Join On the concrete records
     * in foreign Data Source by  foreign Data Source FKs to the source Data Source (backward direction).
     */
    @Test
    public void fetchOneToMany_EntireEntity() throws Exception {
        withHandlers(Handler.EmployeeRole);
        withExtraFields(ExtraField.OneToMany_FetchEntireEntities, ExtraField.SqlCalculated);

        final DSRequest request = new DSRequest();
        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
               "response":{
                  "status":0,
                  "startRow":0,
                  "endRow":5,
                  "totalRows":5,
                  "data":[
                     {
                        "id":1,
                        "name":"admin",
                        "calculated":"1_admin", 
                        "roles":[
                           {
                              "role":"Admin",
                              "employee":1,
                              "employeeCalculated":"1_admin"
                           },
                           {
                              "role":"Developer",
                              "employee":1,
                              "employeeCalculated":"1_admin"
                           }
                        ]
                     },
                     {
                        "id":2,
                        "name":"developer",
                        "calculated":"2_developer", 
                        "roles":[
                           {
                              "role":"Developer",
                              "employee":2,
                              "employeeCalculated":"2_developer"
                           }
                        ]
                     },
                     {
                        "id":3,
                        "name":"UseR3",
                        "calculated":"3_UseR3", 
                        "roles":[]
                     },
                     {
                        "id":4,
                        "name":"user4",
                        "calculated":"4_user4", 
                        "roles":[]
                     },
                     {
                        "id":5,
                        "name":"user5",
                        "calculated":"5_user5", 
                        "roles":[]
                     }
                  ]
               }
            }""", response);
    }

    @Test
    public void fetchOneToMany_OnlyIds() throws Exception {
        withHandlers(Handler.EmployeeRole);
        withExtraFields(ExtraField.OneToMany_FetchOnlyIds, ExtraField.SqlCalculated);

        final DSRequest request = new DSRequest();
        request.setOutputs("id, name, roles");
        final DSResponse response = handler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
               "response":{
                  "status":0,
                  "startRow":0,
                  "endRow":5,
                  "totalRows":5,
                  "data":[
                     {
                        "id":1,
                        "name":"admin",
                        "roles":[
                           {
                              "role":"Admin",
                              "employee":1
                           },
                           {
                              "role":"Developer",
                              "employee":1
                           }
                        ]
                     },
                     {
                        "id":2,
                        "name":"developer",
                        "roles":[
                           {
                              "role":"Developer",
                              "employee":2
                           }
                        ]
                     },
                     {
                        "id":3,
                        "name":"UseR3",
                        "roles":[
            
                        ]
                     },
                     {
                        "id":4,
                        "name":"user4",
                        "roles":[
            
                        ]
                     },
                     {
                        "id":5,
                        "name":"user5",
                        "roles":[
            
                        ]
                     }
                  ]
               }
            }""", response);
    }
}

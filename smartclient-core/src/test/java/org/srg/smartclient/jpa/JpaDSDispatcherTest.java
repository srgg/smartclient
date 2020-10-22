package org.srg.smartclient.jpa;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.*;
import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.JpaDSDispatcher;
import org.srg.smartclient.utils.JsonSerde;
import org.srg.smartclient.JsonTestSupport;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import javax.persistence.*;
import javax.sql.DataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/JpaHibernateRelations.html
 * https://en.wikibooks.org/wiki/Java_Persistence/ManyToMany#Mapping_a_Join_Table_with_Additional_Columns
 */
public class JpaDSDispatcherTest {
    private JpaDSDispatcher dispatcher;
    private EntityManagerFactory emf;
    private DataSource dataSource = new DataSource() {
        @Override
        public Connection getConnection() throws SQLException {
            final SessionFactoryImpl sf = (SessionFactoryImpl) emf;
            final Connection con = sf.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
            return con;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {

        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {

        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }
    };


    @BeforeAll
    public static void setupMapper() {
        JsonTestSupport.defaultMapper = JsonSerde.createMapper();
    }

    private Void initDB( Connection connection ) {
        Flyway flyway = new Flyway(
                new FluentConfiguration()
                        .dataSource( dataSource )
                        .locations("classpath:db")
        );

        flyway.clean();
        flyway.migrate();

        return null;
    }

    @BeforeEach
    public void setupJPADispatcher() throws Exception {
        emf = JpaTestSupport.createEntityManagerFactory(
                "testPU",
                SimpleEntity.class,
                Employee.class,
                EmployeeStatus.class,
                EmployeeRole.class,
                ClientData.class,
                Client.class,
                Project.class
        );

        final JDBCHandler.JDBCPolicy jdbcPolicy = (database, callback) -> {
            final SessionFactoryImpl sf = (SessionFactoryImpl) emf;
            final Connection con = sf.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
            try {
                callback.apply(con);
            } finally {
                if (!con.isClosed()) {
                    sf.getJdbcServices().getBootstrapJdbcConnectionAccess().releaseConnection(con);
                }
            }
        };

        dispatcher = new JpaDSDispatcher(emf, jdbcPolicy);

        jdbcPolicy.withConnectionDo( "test", this::initDB);
    }

    @AfterEach
    public void destroyEntityManagerFactory(){
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    public void simpleCheckToEnsureThatJpaTestSupportIsWorkingProperly() {
        EntityManager em = emf.createEntityManager();
        try {
            em.find(SimpleEntity.class, 42);
            assert emf != null;


            final SimpleEntity te1 = em.find(SimpleEntity.class, 42);
            assertNull(te1);

            final SimpleEntity ethalon = new SimpleEntity(42, "Answer to the Ultimate Question of Life, The Universe, and Everything");
            em.persist(ethalon);

            final SimpleEntity te2 = em.find(SimpleEntity.class, 42);
            assertEquals(ethalon.name(), te2.name());
        } finally {
            if(em != null) {
                em.close();
            }
        }
    }

    @Test
    public void checkThatJpaMappingsAreCorrectAndRelevant_Projects() {

        // "Employee.projects" serialization
        final BeanSerializerModifier beanSerializerModifier = new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                if (beanDesc.getBeanClass().equals(Employee.class)){
                    return beanProperties.stream()
                            .filter( bpw -> !bpw.getName().equals("projects") )
                            .collect(Collectors.toList());
                } else {
                    return super.changeProperties(config, beanDesc, beanProperties);
                }
            }
        };

        final List<Project> projects;
        final EntityManager em = emf.createEntityManager();
        try {
            projects = em.createQuery("""
                    SELECT p FROM Project p
                    """)
                    .getResultList();
        }finally {
            em.close();
        }

        JsonTestSupport.assertJsonEquals(
                """
                [
                   {
                      id:1,
                      name:'Project 1 for client 1',
                      client:{
                         id:1,
                         name:'client 1',
                         data:{
                            id:1,
                            data:'Data1: client 1'
                         }
                      },
                      manager: {
                        id:4,
                        name:'manager1',
                        roles:[
                            {
                                role:'PM'
                            }
                        ],
                        statuses:[]
                      }
                   },
                   {
                      id:2,
                      name:'Project 2 for client 1',
                      client:{
                         id:1,
                         name:'client 1',
                         data:{
                            id:1,
                            data:'Data1: client 1'
                         }
                      },
                      manager: {
                        id:4,
                        name:'manager1',
                        roles:[
                            {
                                role:'PM'
                            }
                        ],
                        statuses:[]
                      }
                   },
                   {
                      id:3,
                      name:'Project 1 for client 2',
                      client:{
                         id:2,
                         name:'client 2',
                         data:{
                            id:2,
                            data:'Data2: client 2'
                         }
                      },
                      manager: {
                        id:5,
                        name:'manager2',
                        roles:[
                            {
                                role:'PM'
                            }
                        ],
                        statuses:[]
                      }
                   },
                   {
                      id:4,
                      name:'Project 2 for client 2',
                      client:{
                         id:2,
                         name:'client 2',
                         data:{
                            id:2,
                            data:'Data2: client 2'
                         }
                      },
                      manager: {
                        id:5,
                        name:'manager2',
                        roles:[
                            {
                                role:'PM'
                            }
                        ],
                        statuses:[]
                      }
                   },
                   {
                      id:5,
                      name:'Project 3 for client 2',
                      client:{
                         id:2,
                         name:'client 2',
                         data:{
                            id:2,
                            data:'Data2: client 2'
                         }
                      },
                      manager: {
                        id:5,
                        name:'manager2',
                        roles:[
                            {
                                role:'PM'
                            }
                        ],
                        statuses:[]
                      }
                   }
                ]""", projects, beanSerializerModifier);
    }

    @Test
    public void checkThatJpaMappingsAreCorrectAndRelevant_Employees() {
        /* Ignore "Project.manager" serialization to avoid:
         * java.lang.RuntimeException: com.fasterxml.jackson.databind.JsonMappingException: Infinite recursion (StackOverflowError) (through reference chain: org.srg.smartclient.jpa.Project["manager"]->org.srg.smartclient.jpa.Employee["projects"]
         */
        final BeanSerializerModifier beanSerializerModifier = new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                if (beanDesc.getBeanClass().equals(Project.class)){
                    return beanProperties.stream()
                            .filter( bpw -> !bpw.getName().equals("manager") )
                            .collect(Collectors.toList());
                } else {
                    return super.changeProperties(config, beanDesc, beanProperties);
                }
            }
        };


        final List<Employee> employees;
        final EntityManager em = emf.createEntityManager();
        try {
            employees = em.createQuery("""
                    SELECT e FROM Employee e
                    """)
                    .getResultList();
        }finally {
            em.close();
        }

        JsonTestSupport.assertJsonEquals(
                """
                [
                   {
                      id:1,
                      name:"admin",
                      roles:[
                        {
                            role:'Developer'
                        }, 
                        {
                            role:'Admin'
                        }   
                      ],
                      projects:[
                         {
                            client:{
                               data:{
                                  data:'Data1: client 1',
                                  id:1
                               },
                               id:1,
                               name:"client 1"
                            },
                            id:1,
                            name:'Project 1 for client 1'
                         }
                      ],
                      statuses:[
                          {
                            endDate:null,
                            id:3,
                            startDate: '2000-07-06',
                            status:'status 3'
                          },
                          {
                            endDate:'2000-07-05',
                            id:2,
                            startDate:'2000-06-05',
                            status:'status 2'
                          },
                          {
                            endDate:'2000-06-04',
                            id:1,
                            startDate:'2000-05-04',
                            status:'status 1'
                          }                      
                      ]
                   },
                   {
                      id:2,
                      name:'developer',
                      roles:[
                        {
                            role:'Developer'
                        }
                      ],
                      projects:[
                         {
                            client:{
                               data:{
                                  data:'Data1: client 1',
                                  id:1
                               },
                               id:1,
                               name:'client 1'
                            },
                            id:2,
                            name:'Project 2 for client 1'
                         },
                         {
                            client:{
                               data:{
                                  data:'Data1: client 1',
                                  id:1
                               },
                               id:1,
                               name:'client 1'
                            },
                            id:1,
                            name:'Project 1 for client 1'
                         }
                      ],
                      statuses:[]                      
                   },
                   {
                      id:3,
                      name:'UseR3',
                      roles:[],
                      projects:[
                         {
                            client:{
                               data:{
                                  data:'Data1: client 1',
                                  id:1
                               },
                               id:1,
                               name:'client 1'
                            },
                            id:2,
                            name:'Project 2 for client 1'
                         }
                      ],
                      statuses:[]
                   },
                   {
                      id:4,
                      name:'manager1',
                      roles:[
                        {
                            role:'PM'
                        }
                      ],
                      projects:[
                         {
                            client:{
                               data:{
                                  data:'Data2: client 2',
                                  id:2
                               },
                               id:2,
                               name:'client 2'
                            },
                            id:3,
                            name:'Project 1 for client 2'
                         }
                      ],
                      statuses:[]
                   },
                   {
                      id:5,
                      name:'manager2',
                      roles:[
                        {
                            role:'PM'
                        }
                      ],
                      projects:[
                         {
                            client:{
                               data:{
                                  data:'Data2: client 2',
                                  id:2
                               },
                               id:2,
                               name:'client 2'
                            },
                            id:3,
                            name:'Project 1 for client 2'
                         }
                      ],
                      statuses:[]
                   },
                   {
                      id:6,
                      name:'user2',
                      roles:[],
                      projects:[],
                      statuses:[]
                   }
                ]""", employees, beanSerializerModifier);
    }


    @Test
    public void oneToOneRelation() {
        dispatcher.registerJPAEntity(Client.class);
        final String clientDataDsId = dispatcher.registerJPAEntity(ClientData.class);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setDataSource( clientDataDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);

        JsonTestSupport.assertJsonEquals(
            """
            [
                {
                     status:0,
                     startRow:0,
                     endRow:2,
                     totalRows:2,
                     data:[
                        {
                           id:1,
                           data: 'Data1: client 1',
                           client: 1,
                           clientName: 'client 1' 
                        },
                        {
                           id:2,
                           data: 'Data2: client 2',
                           client: 2,
                           clientName: 'client 2' 
                        }
                     ]
                }
            ]""",
            responses);
    }

    @Test
    public void oneToOneRelationMappedBy() {
        final String clientDsId = dispatcher.registerJPAEntity(Client.class);
        dispatcher.registerJPAEntity(ClientData.class);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setOutputs("id, data");
        request.setDataSource( clientDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
                [
                    {
                        status:0,
                        startRow:0,
                        endRow:2,
                        totalRows:2,
                        data:[
                            {
                                id:1,
                                data:[
                                    {
                                         id:1,
                                         data:'Data1: client 1',
                                         client:1,
                                         clientName:'client 1'
                                    }
                                ]
                            },
                            {
                                id:2,
                                data:[
                                    {
                                        id:2,
                                        data:'Data2: client 2',
                                        client:2,
                                        clientName:'client 2'
                                  }
                               ]
                            }
                         ]
                    }
                ]""", responses
        );
    }

    @Test
    public void manyToOneRelation() {
        dispatcher.registerJPAEntity(Employee.class);
        dispatcher.registerJPAEntity(Client.class);
        final String projectDsId = dispatcher.registerJPAEntity(Project.class);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setDataSource( projectDsId);
        request.setOutputs("id, name, client, clientName");

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
            {
                status:0,
                startRow:0,
                endRow:5,
                totalRows:5,
                data:[
                 {
                    id:1,
                    name:'Project 1 for client 1',
                    client:1,
                    clientName: 'client 1'                         
                 },
                 {
                    id:2,
                    name:'Project 2 for client 1',
                    client:1,
                    clientName: 'client 1'                         
                 },
                 {
                    id:3,
                    name:'Project 1 for client 2',
                    client:2,
                    clientName: 'client 2'                         
                 },
                 {
                    id:4,
                    name:'Project 2 for client 2',
                    client:2,
                    clientName: 'client 2'                         
                 },
                 {
                    id:5,
                    name:'Project 3 for client 2',
                    client:2,
                    clientName: 'client 2'                                                     
                 }
                ]
            }"""
            , responses.iterator().next());
    }

    @Test
    public void oneToManyRelation() {
        final String clientDsId = dispatcher.registerJPAEntity(Client.class);
        dispatcher.registerJPAEntity(Employee.class);
        dispatcher.registerJPAEntity(Project.class);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setOutputs("id, projects");
        request.setAdditionalOutputs("projects!ProjectDS.name, projects!ProjectDS.client, projects!ProjectDS.clientName, projects!ProjectDS.id");
        request.setDataSource( clientDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
                [
                    {
                        status:0,
                        startRow:0,
                        endRow:2,
                        totalRows:2,
                        data:[
                        {
                            id:1,
                            projects:[
                                {
                                    id:1,
                                    client:1,
                                    clientName:'client 1',
                                    name:'Project 1 for client 1'
                                },
                                {
                                    id:2,
                                    client:1,
                                    clientName:'client 1',
                                    name:'Project 2 for client 1'
                                }
                            ]
                        },
                        {
                            id:2,
                            projects:[
                                {
                                    id:3,
                                    client:2,
                                    clientName:'client 2',
                                    name:'Project 1 for client 2'
                                },
                                {
                                    id:4,
                                    client:2,
                                    clientName:'client 2',
                                    name:'Project 2 for client 2'
                                },
                                {
                                    id:5,
                                    client:2,
                                    clientName:'client 2',
                                    name:'Project 3 for client 2'
                                }
                            ]
                        }
                     ]
                   }
                ]""",
                responses
        );
    }

    @Test
    public void oneToManyRelationWithCompositeForeignKey() {
        final String employeeDsId = dispatcher.registerJPAEntity(Employee.class);
        dispatcher.registerJPAEntity(EmployeeRole.class);

        // --
        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setOutputs("id, roles");
        request.setDataSource( employeeDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
                [
                   {
                        status:0,
                        startRow:0,
                        endRow:6,
                        totalRows:6,
                        data:[
                        {
                           id:1,
                           roles:[
                              {
                                 role:'Admin',
                                 employee:1
                              },
                              {
                                 role:'Developer',
                                 employee:1
                              }
                           ]
                        },
                        {
                           id:2,
                           roles:[
                              {
                                 role:'Developer',
                                 employee:2
                              }
                           ]
                        },
                        {
                           id:3,
                           roles:[]
                        },
                        {
                           id:4,
                           roles:[
                              {
                                 role:'PM',
                                 employee:4
                              }
                           ]
                        },
                        {
                           id:5,
                           roles:[
                              {
                                 role:'PM',
                                 employee:5
                              }
                           ]
                        },
                        {
                           id:6,
                           roles:[]
                        }                        
                    ]
                   }
                ]""",
                responses
        );
    }


    @Test
    public void loadSqlDataSourceFromResource() throws Exception {
        dispatcher.registerJPAEntity(Employee.class);
        dispatcher.registerJPAEntity(Client.class);
        dispatcher.registerJPAEntity(ClientData.class);

        final String hprjDSId = "HProjectDS";
        dispatcher.loadFromResource("HProject.ds.json");
        final org.srg.smartclient.isomorphic.DataSource ds = dispatcher.getDataSourceById(hprjDSId);
        assertNotNull(ds);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setDataSource( hprjDSId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
            """
            [
               {
                 "data":[
                    {
                       "client":1,
                       "clientName":"client 1",
                       "id":1,
                       "name":"Project 1 for client 1"
                    },
                    {
                       "client":1,
                       "clientName":"client 1",
                       "id":2,
                       "name":"Project 2 for client 1"
                    },
                    {
                       "client":2,
                       "clientName":"client 2",
                       "id":3,
                       "name":"Project 1 for client 2"
                    },
                    {
                       "client":2,
                       "clientName":"client 2",
                       "id":4,
                       "name":"Project 2 for client 2"
                    },
                    {
                       "client":2,
                       "clientName":"client 2",
                       "id":5,
                       "name":"Project 3 for client 2"
                    }
                 ],
                 "endRow":5,
                 "startRow":0,
                 "status":0,
                 "totalRows":5
               }                
            ]""", responses);
    }

    @Test
    public void oneToMany_WithAssociationOverride() {
        final String employeeDsId = dispatcher.registerJPAEntity(Employee.class);
        dispatcher.registerJPAEntity(EmployeeStatus.class);

        // --
        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setOutputs("id, statuses");
        request.setDataSource(employeeDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
    """
            [
                   {
                         status:0,
                         startRow:0,
                         endRow:6,
                         totalRows:6,
                         data:[
                             {
                                 id:1,
                                 statuses:[
                                    {
                                       id:1,
                                       owner:1,
                                       employeeName: 'admin',
                                       status:'status 1',
                                       startDate:'2000-05-04',
                                       endDate:'2000-06-04'
                                    },
                                    {
                                       id:2,
                                       owner:1,
                                       employeeName: 'admin',
                                       status:'status 2',
                                       startDate:'2000-06-05',
                                       endDate:'2000-07-05'
                                    },
                                    {
                                       id:3,
                                       owner:1,
                                       employeeName: 'admin',
                                       status:'status 3',
                                       startDate:'2000-07-06'
                                    }
                                 ]
                              },
                              {
                                 "id":2,
                                 "statuses":[]
                              },
                              {
                                 "id":3,
                                 "statuses":[]
                              },
                              {
                                 "id":4,
                                 "statuses":[]
                              },
                              {
                                 "id":5,
                                 "statuses":[]
                              },                     
                              {
                                 "id":6,
                                 "statuses":[]
                              }                     
                            ]
                       }
                   ]
                }
            ]""", responses);
    }

    @Disabled("Disabled until @ManyToMany will be supported")
    @Test
    public void manyToManyRelation() {
        dispatcher.registerJPAEntity(Employee.class);
        dispatcher.registerJPAEntity(Client.class);
        dispatcher.registerJPAEntity(ClientData.class);

        final String projectDs = dispatcher.registerJPAEntity(Project.class);

        // --
        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setOutputs("id, name, teamMembers");
        request.setAdditionalOutputs("teamMembers!EmployeeDS.id, teamMembers!EmployeeDS.name");
        request.setDataSource( projectDs);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
                [
                ]""", responses);
    }
}

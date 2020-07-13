package org.srg.smartclient.jpa;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.JpaDSDispatcher;
import org.srg.smartclient.JsonSerde;
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
    public void checkThatJpaMappingsAreCorrectAndRelevant() {
//        // -- Ensure that Employee is properly annotated and JPA can fetch it
//        final List<Employee> employees;
//        final EntityManager em = emf.createEntityManager();
//        try {
//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Employee> query = cb.createQuery(Employee.class);
//            Root<Employee> root = query.from(Employee.class);
//            root.fetch("roles", JoinType.LEFT);
////            query.select(cb.construct(Employee.class, root.get("id"), root.get("name"), root.get("roles")));
//            query.select(root)
//            .distinct(true);
//
//            TypedQuery<Employee> typedQuery = em.createQuery(query);
//            employees = typedQuery.getResultList();
//        }finally {
//            em.close();
//        }

//        final List<Object[]> employees;
//        final EntityManager em = emf.createEntityManager();
//        try {
//            employees = em.createQuery("""
//                    SELECT new Employee(e.id, e.name, e.roles)
//                           FROM Employee e
//                    """)
//                    .getResultList();
//        }finally {
//            em.close();
//        }


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
                      ]
                   },
                   {
                      id:3,
                      name:'user3',
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
                      ]
                   },
                   {
                      id:4,
                      name:'user4',
                      roles:[],
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
                      ]
                   },
                   {
                      id:5,
                      name:'user5',
                      roles:[],
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
                      ]
                   }
                ]""", employees );
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
                  response:{
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
                        response:{
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
                    }
                ]""", responses
        );
    }

    @Test
    public void manyToOneRelation() {
        dispatcher.registerJPAEntity(Client.class);
        final String projectDsId = dispatcher.registerJPAEntity(Project.class);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setDataSource( projectDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
            {
               response:{
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
               }
            }"""
            , responses.iterator().next());
    }

    @Test
    public void oneToManyRelation() {
        final String clientDsId = dispatcher.registerJPAEntity(Client.class);
        dispatcher.registerJPAEntity(Project.class);

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
        request.setOutputs("id, projects");
        request.setDataSource( clientDsId);

        final Collection<DSResponse> responses = dispatcher.dispatch(request);
        JsonTestSupport.assertJsonEquals(
                """
                [
                    {
                        response:{
                            status:0,
                            startRow:0,
                            endRow:2,
                            totalRows:2,
                            data:[
                            {
                                id:1,
                                projects:[
                                    {
                                        name:'Project 1 for client 1',
                                        client:1,
                                        clientName:'client 1',
                                        id:1
                                    }
                                ]
                            },
                            {
                                id:2,
                                projects:[
                                    {
                                        name:'Project 2 for client 1',
                                        client:1,
                                        clientName:'client 1',
                                        id:2
                                    }
                                ]
                            }
                         ]
                      }
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
                      response:{
                         status:0,
                         startRow:0,
                         endRow:5,
                         totalRows:5,
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
                               roles:[]
                            },
                            {
                               id:5,
                               roles:[]
                            }
                         ]
                      }
                   }
                ]""",
                responses
        );
    }


    @Test
    public void loadSqlDataSourceFromResource() {
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
                  "response":{
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
               }                
            ]""", responses);
    }
}

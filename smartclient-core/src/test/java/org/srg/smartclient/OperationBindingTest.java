package org.srg.smartclient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.OperationBinding;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;

import java.sql.*;
import java.util.*;

public class OperationBindingTest extends AbstractJDBCHandlerTest {

    final private static String TIME_REPORT = """
            {                        
                id: 'TimeReportDS',
                tableName: 'tr',
                operationBindings:[
                    {
                      operationId: 'fetchFromFunctionWithAdvancedCriteria',
                      operationType: 'fetch',
                      excludeCriteriaFields: 'periodStart, periodEnd',
                      tableClause: 'get_time_report(
                            <#if criteria.periodStart??>\\'${criteria.periodStart}\\'<#else>null</#if>,
                            <#if criteria.periodEnd??>\\'${criteria.periodEnd}\\'<#else>null</#if>) tr'
                    },
                    {
                      operationId: 'fetchCustomSQLWithAdvancedCriteria',
                      operationType: 'fetch',
                      customSQL: '
                        SELECT ${defaultSelectClause}
                        FROM ( 
                            SELECT
                                CAST(NULL as DATE) as work_date,
                                (p.name) as project_name,
                                (p.id) as project_id,
                                (e.name) as employee,
                                total.employee_id_tr as employee_id,
                                (total.minutes) as total_minutes               
                            FROM (
                                        SELECT  
                                            opaque.project_id_tr,
                                            opaque.employee_id_tr,                                            
                                            SUM(opaque.minutes_tr) as minutes
                                        FROM (
                                                 SELECT 
                                                    project_id as project_id_tr,
                                                    employee_id as employee_id_tr,
                                                    work_date as work_date_tr,
                                                    minutes as minutes_tr                                                    
                                                 FROM time_log_entry
                                           ) opaque
                                        WHERE ${defaultWhereClause}
                                        GROUP BY opaque.project_id_tr, opaque.employee_id_tr
                                ) total
                            LEFT JOIN project p on total.project_id_tr = p.id
                            LEFT JOIN employee e on total.employee_id_tr = e.id
                        ) tr'
                    }
                ],          
                fields:[
                    {
                        name:"work_date"
                        ,type:"DATE"
                        ,hidden:true
                    },                
                    {
                        name:"projectId"
                        ,dbName: "project_id"
                        ,type:"INTEGER"
                        ,foreignKey:"ProjectDS.id"
                        ,primaryKey:true
                        ,canEdit:false
                        ,hidden:true
                    },
                    {
                        name: 'project'
                        ,dbName: 'project_name'
                        ,type: 'TEXT'
                        ,canEdit: false
                    },
                    {
                        name:"employeeId"
                        ,dbName: "employee_id"
                        ,type:"INTEGER"
                        ,foreignKey:"EmployeeDS.id"
                        ,primaryKey:true
                        ,canEdit:false
                        ,hidden:true
                    },
                    {
                        name: 'employee'
                        ,dbName: 'employee'
                        ,type: 'TEXT'
                        ,canEdit: false
                    },
                    {
                        name:"minutes"
                        ,dbName: "total_minutes"
                        ,type:"INTEGER"
                    }
                ]
            }""";


    @Override
    protected Class getHandlerClass() {
        return AdvancedJDBCHandler.class;
    }

    @SuppressWarnings("SqlResolve")
    public static ResultSet getTimeReport(Connection conn, String o1, String o2) throws SQLException {
        final String query = """
                SELECT * FROM (
                SELECT
                    CAST(NULL as DATE) as work_date,
                    (p.name) as project_name,
                    (p.id) as project_id,
                    (e.name) as employee,
                    total.employee_id,
                    (total.minutes) as total_minutes               
                    FROM (
                             SELECT project_id, employee_id, SUM(minutes) as minutes
                             FROM time_log_entry
                             WHERE TRUE AND (?1 IS NULL OR work_date >= ?1) AND (?2 IS NULL OR work_date <= ?2)
                             GROUP BY project_id, employee_id
                         ) total
                             LEFT JOIN project p on total.project_id = p.id
                             LEFT JOIN employee e on total.employee_id = e.id
                    )
                """;

        PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, o1);
            statement.setString(2, o2);
            return statement.executeQuery();
    }

    @BeforeEach
    @Override
    public void setupDataSources() throws Exception {
        super.setupDataSources();

        try ( final Connection con = jdbcDataSource.getConnection()) {
            try (final Statement statement = con.createStatement()) {
                statement.execute("CREATE ALIAS IF NOT EXISTS get_time_report FOR \"org.srg.smartclient.OperationBindingTest.getTimeReport\"");
            }
            con.commit();
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static record TimeLogData(
            String project,
            Integer projectId,
            String employee,
            Integer employeeId,
            Long minutes
    ){};

    @Test
    public void ensureThatTestIsProperlySet() throws Exception  {
        final List<TimeLogData> data = new LinkedList<>();

        try ( final Connection con = jdbcDataSource.getConnection()) {
            try (final Statement statement = con.createStatement()) {
                try(ResultSet rs =statement.executeQuery("SELECT * FROM get_time_report(null, null)");){
                    while (rs.next()) {
                        final Object period = rs.getObject(1);
                        final String project = rs.getString(2);
                        final Integer projectId = rs.getInt(3);
                        final String employee = rs.getString(4);
                        final Integer employeeId = rs.getInt(5);
                        final Long minutes = rs.getLong(6);

                        assert period == null;

                        data.add(
                                new TimeLogData(project, projectId, employee, employeeId, minutes)
                            );
                    }
                }
            }
        }

        JsonTestSupport.assertJsonEquals("""
            [
                { 
                    project: 'Project 1 for client 1',
                    projectId: 1,
                    employee: 'admin',
                    employeeId:1,
                    minutes:123
                },
                {
                    project: 'Project 1 for client 1',
                    projectId: 1,
                    employee: 'developer',
                    employeeId: 2,
                    minutes:123
                },
                {
                    project: 'Project 1 for client 2',
                    projectId: 3,
                    employee: 'admin',
                    employeeId: 1,
                    minutes: 246
                },
                {
                    project: 'Project 1 for client 2',
                    projectId: 3,
                    employee: 'developer',
                    employeeId: 2,
                    minutes: 246
                }
            ]""",
            data
        );
    }

    @Test
    public void fetchFromFunctionWith_NULL_values() throws Exception {
        final JDBCHandler handler = withHandler(TIME_REPORT);
        DSRequest request = new DSRequest();
        request.setOperationType( DSRequest.OperationType.FETCH);
        request.setOperationId("fetchFromFunctionWithAdvancedCriteria");
        final Map<String, Object> criteria = new HashMap<>(){
            {
                this.put("periodStart", null);
                this.put("periodEnd", null);
            }
        };

        request.wrapAndSetData(criteria);

        final DSResponse response = handler.handleFetch(request);

        JsonTestSupport.assertJsonEquals("""
            {
              status:0,
              startRow:0,
              endRow:4,
              totalRows:4,
              data:[
                { 
                    project: 'Project 1 for client 1',
                    projectId: 1,
                    employee: 'admin',
                    employeeId:1,
                    minutes:123
                },
                {
                    project: 'Project 1 for client 1',
                    projectId: 1,
                    employee: 'developer',
                    employeeId: 2,
                    minutes:123
                },
                {
                    project: 'Project 1 for client 2',
                    projectId: 3,
                    employee: 'admin',
                    employeeId: 1,
                    minutes: 246
                },
                {
                    project: 'Project 1 for client 2',
                    projectId: 3,
                    employee: 'developer',
                    employeeId: 2,
                    minutes: 246
                }              ]
            }""", response);

    }

    @Test
    public void fetchFromFunctionWith_TableClause_AdvancedCriteria_ExcludeCriteria() throws Exception {
        final JDBCHandler handler = withHandler(TIME_REPORT);

        DSRequest request = new DSRequest();
        request.setOperationType( DSRequest.OperationType.FETCH);
        request.setOperationId("fetchFromFunctionWithAdvancedCriteria");

        final Map<String, Object> criteria = Map.of("periodStart", "2020-03-07", "periodEnd", "2020-03-12");
        request.wrapAndSetData(criteria);

        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
              status:0,
              startRow:0,
              endRow:4,
              totalRows:4,
              data:[
                {
                  "projectId":1,
                  "employeeId":1,
                  "project":"Project 1 for client 1",
                  "employee":"admin",
                  "minutes":122
                },
                {
                  "projectId":1,
                  "employeeId":2,
                  "project":"Project 1 for client 1",
                  "employee":"developer",
                  "minutes":12
                },
                {
                  "projectId":3,
                  "employeeId":1,
                  "project":"Project 1 for client 2",
                  "employee":"admin",
                  "minutes":244
                },
                {
                  "projectId":3,
                  "employeeId":2,
                  "project":"Project 1 for client 2",
                  "employee":"developer",
                  "minutes":24
                }              
              ]
            }""", response);
    }


    @Test
    public void fetchWith_CustomSQL_AdvancedCriteria() throws Exception {
        final JDBCHandler handler = withHandler(TIME_REPORT);

        DSRequest request = new DSRequest();
        request.setOperationType( DSRequest.OperationType.FETCH);
        request.setOperationId("fetchCustomSQLWithAdvancedCriteria");

        final AdvancedCriteria advancedCriteria = JsonTestSupport.fromJSON(AdvancedCriteria.class,
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [            
                                {
                                    "fieldName":"work_date",
                                    "operator":"greaterOrEqual",
                                    "value":"2020-03-07"
                                },
                                {
                                    "fieldName":"work_date",
                                    "operator":"lessOrEqual",
                                    "value":"2020-03-12"
                                }
                            ]
                        }"""
        );
        request.setData(advancedCriteria);


        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
              status:0,
              startRow:0,
              endRow:4,
              totalRows:4,
              data:[
                {
                  "projectId":1,
                  "employeeId":1,
                  "project":"Project 1 for client 1",
                  "employee":"admin",
                  "minutes":122
                },
                {
                  "projectId":1,
                  "employeeId":2,
                  "project":"Project 1 for client 1",
                  "employee":"developer",
                  "minutes":12
                },
                {
                  "projectId":3,
                  "employeeId":1,
                  "project":"Project 1 for client 2",
                  "employee":"admin",
                  "minutes":244
                },
                {
                  "projectId":3,
                  "employeeId":2,
                  "project":"Project 1 for client 2",
                  "employee":"developer",
                  "minutes":24
                }              
              ]
            }""", response);
    }

    /*
     * Test wil fails if default binding will not be used:
     * the default binding ignores non existent fields criteria sent within  request.
     */
    @Test
    public void fetchWith_defaultBinding() throws Exception {
        final String DEFAULT_BINDING = """
                    {
                      operationType: 'fetch',
                      excludeCriteriaFields: 'NonExistentField1, NonExistentField2'
                    },
                """;

        final JDBCHandler handler = withHandlers(Handler.Location);

        handler.dataSource().setOperationBindings(
                Arrays.asList(
                        JsonTestSupport.fromJSON(OperationBinding.class, DEFAULT_BINDING)
                )
        );


        DSRequest request = new DSRequest();
        request.setOperationType( DSRequest.OperationType.FETCH);
        request.wrapAndSetData(Map.of("NonExistentField1", 12, "NonExistentField2", "ha-ha"));

        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals("""
            {
              status:0,
              startRow:0,
              endRow: 3,
              totalRows: 3,
              data:[
                {
                   "id":1,
                   "country":"Ukraine",
                   "city":"Kharkiv"
                },
                {
                   "id":2,
                   "country":"Ukraine",
                   "city":"Lviv"
                },
                {
                   "id":3,
                   "country":"USA",
                   "city":"USA"
                }                
              ]
            }""", response);
    }
}

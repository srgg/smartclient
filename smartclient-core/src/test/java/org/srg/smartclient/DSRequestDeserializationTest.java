package org.srg.smartclient;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSTransaction;
import org.srg.smartclient.isomorphic.IDSRequest;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DSRequestDeserializationTest {
    private static  <T extends IDSRequest> T deserialize(String data) throws IOException {

        final T retVal = JsonSerde.deserializeRequest(data);

        if (retVal instanceof DSRequest request){
                assertEquals("userAssignmentsDS", request.getDataSource());
                assertEquals("AssignmentsList", request.getComponentId());
        }

        return retVal;
    }

    @Test
    public void parseFetch() throws IOException {
        final String FETCH_OPERATION = """
            {
                "useStrictJSON": true,
                "dataSource":"userAssignmentsDS",
                "operationType":"fetch",
                "operationId":"categories_fetch",
                "startRow":0,
                "endRow":75,
                "textMatchStyle":"substring",
                "componentId":"AssignmentsList",
                "data":{
                },
                "oldValues":null
            }""";

        final DSRequest req = deserialize(FETCH_OPERATION);

        assertEquals(DSRequest.OperationType.FETCH, req.getOperationType());
        assertEquals(0, (Object)req.getStartRow());
        assertEquals(75, (Object)req.getEndRow() );

        assertTrue(req.getData() instanceof Map m && m.isEmpty());

        assertNull(req.getOldValues());
        assertNull(req.getSortBy());
        assertEquals(DSRequest.TextMatchStyle.SUBSTRING, req.getTextMatchStyle());
    }

    @Test
    public void parseTransactionalFetch() throws IOException {
        final String FETCH_WITH_TRANSACTION_OPERATION = """
            { 
                "transaction": { 
                    "transactionNum": 42, 
                    "operations": [
                        {
                            "dataSource":"EmployeeDS",
                            "operationType":"fetch",
                            "startRow":0,
                            "endRow":75,
                            "textMatchStyle":"substring",
                            "useStrictJSON":true,
                            "componentId":"employeeGrid",
                            "data":{},
                            "oldValues":null
                        },
                        {
                            "dataSource":"LocationDS",
                            "operationType":"fetch",
                            "textMatchStyle":"startsWith",
                            "useStrictJSON":true,
                            "componentId":"isc_PickListMenu_0",
                            "data":{},
                            "oldValues":null
                        }
                    ]
                }
            }""";

        final DSTransaction transaction = deserialize(FETCH_WITH_TRANSACTION_OPERATION);
        assertNotNull(transaction);

        assertEquals(2, transaction.getOperations().size());
        assertEquals(42, transaction.getTransactionNum());
    }

    @Test
    public void parseFetchWithSort() throws IOException {
        final String FETCH_WITH_SORT_OPERATION = """
            {
                "dataSource":"userAssignmentsDS", 
                "operationType":"fetch", 
                "startRow":0, 
                "endRow":75, 
                "sortBy":[
                "priority"
                ], 
                "textMatchStyle":"substring", 
                "componentId":"AssignmentsList", 
                "data":{
                }, 
                "oldValues":null
            }""";

        final DSRequest req = deserialize(FETCH_WITH_SORT_OPERATION);

        assertEquals(DSRequest.OperationType.FETCH, req.getOperationType());
        assertEquals(0, (Object)req.getStartRow());
        assertEquals(75, (Object)req.getEndRow() );
        assertTrue(req.getData() instanceof Map m && m.isEmpty());
        assertNull(req.getOldValues());
        assertEquals(DSRequest.TextMatchStyle.SUBSTRING, req.getTextMatchStyle());
    }


    @Disabled
    @Test
    public void parseFetchWithAdvancedCriteria() throws IOException {
        final String FETCH_WITH_ADVANCED_CRITERIA_OPERATION = """
            {
                "dataSource":"userAssignmentsDS", 
                "operationType":"fetch", 
                "startRow":0, 
                "endRow":75, 
                "sortBy":[
                ], 
                "textMatchStyle":"substring", 
                "componentId":"AssignmentsList", 
                "data":{
                    "operator" : "and",
                    "_constructor" : "AdvancedCriteria",
                    "criteria": [ 
                        {
                           "fieldName" : "firedAt",
                            "operator" : "notBlank",
                            "_constructor" : "AdvancedCriteria"
                        } 
                     ]
                }, 
                "oldValues":null
            }""";

        final DSRequest req = deserialize(FETCH_WITH_ADVANCED_CRITERIA_OPERATION);

        assertEquals(DSRequest.OperationType.FETCH, req.getOperationType());
        assertEquals(0, (Object)req.getStartRow());
        assertEquals(75, (Object)req.getEndRow() );
        assertTrue(req.getData() instanceof AdvancedCriteria);
        assertNull(req.getOldValues());
        assertEquals(DSRequest.TextMatchStyle.SUBSTRING, req.getTextMatchStyle());
    }


    @Test
    public void parseAdd() throws IOException {
        final String ADD_OPERATION = """
            {
                "dataSource":"userAssignmentsDS", 
                "operationType":"add", 
                "componentId":"AssignmentsList", 
                "data":{
                    "priority":"2", 
                    "region":"USA", 
                    "disease":"Disease", 
                    "tA":"TA", 
                    "css":"new css",
                    "rml":"new rml"
                }, 
                "oldValues":null
            }""";

        final DSRequest req = deserialize( ADD_OPERATION);

        assertEquals(DSRequest.OperationType.ADD, req.getOperationType());
        assertEquals(0, 0);
        assertEquals(0, 0 );
        assertEquals(null, req.getTextMatchStyle());
        assertTrue(req.getData() instanceof Map m && m.size() == 6);
        assertNull(req.getOldValues());
    }

    @Test
    public void parseRemove() throws IOException {
        final String REMOVE_OPERATION = """
            {
                "dataSource":"userAssignmentsDS", 
                "operationType":"remove", 
                "componentId":"AssignmentsList", 
                "data":{
                    "id":144307, 
                    "priority":12, 
                    "region":"12", 
                    "disease":"12", 
                    "_selection_2":true
                }, 
                "oldValues":null
            }""";
        final DSRequest req = deserialize(REMOVE_OPERATION);

        assertEquals(DSRequest.OperationType.REMOVE, req.getOperationType());
    }
}

package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;


public class JDBCHandlerUpdateTest extends AbstractJDBCHandlerTest<JDBCHandler> {
    @Override
    protected Class<JDBCHandler> getHandlerClass() {
        return JDBCHandler.class;
    }


    @Test
    public void update() throws Exception {
        withExtraFields(ExtraField.Email);

        final DSRequest request = JsonTestSupport.fromJSON(new TypeReference<>(){}, """
               {
                 useStrictJSON : true,
                 dataSource : "EmployeeDS",
                 operationType : "UPDATE",
                 componentId : "isc_ListGrid_5",
                 startRow : 0,
                 endRow : 0,
                 textMatchStyle : "EXACT",
                 data : {                   
                   id : 2,
                   email: 'updated-developer@acme.org'
                 },
                 oldValues : {
                   id : 2,
                   name : 'developer',
                   email: 'developer@acme.org'
                 }
               }                
            """);

        final DSResponse response = handler.handleUpdate(request);

        JsonTestSupport.assertJsonEquals("""
                 {
                     status: 0,
                     queueStatus:0,
                     startRow: 0,
                     endRow: 1,
                     totalRows: 1,
                     data:[
                         {
                             id:2,
                             email:'updated-developer@acme.org'
                         }
                     ]
                }""", response);
    }

}

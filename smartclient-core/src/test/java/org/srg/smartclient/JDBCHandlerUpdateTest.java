package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.utils.ContextualRuntimeException;
import org.srg.smartclient.utils.JsonSerde;

import java.io.StringWriter;


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

        final DSResponse response;
        try {
            response = handler.handleUpdate(request);
        } catch (ContextualRuntimeException e) {
            /*
             * This exception handler can be used  to check and adjust
             * ContextualRuntimeException.dumpContext_ifAny().
             *
             * Other than that it does not have any sense
             */
            final StringWriter sw = new StringWriter();
            final ObjectMapper mapper = JsonSerde.createMapper();

            e.dumpContext_ifAny(sw, "  ", mapper.writerWithDefaultPrettyPrinter());
            System.out.println( sw.toString());
            throw new RuntimeException(e);
        }

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

package org.srg.smartclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.utils.ContextualRuntimeException;
import org.srg.smartclient.utils.Serde;

import java.io.StringWriter;


public class JDBCHandlerUpdateTest extends AbstractJDBCHandlerTest<JDBCHandler> {
    @Override
    protected Class<JDBCHandler> getHandlerClass() {
        return JDBCHandler.class;
    }


    @Test
    public void simpleUpdate() throws Exception {
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
            final ObjectMapper mapper = Serde.createMapper();

            e.dumpContext_ifAny(sw, "  ", mapper.writerWithDefaultPrettyPrinter());
            System.out.println( sw.toString());
            throw new RuntimeException(e);
        }

        JsonTestSupport.assertJsonEquals("""
                 {
                     status: 0,
                     data:[
                         {
                             id:2,
                             email:'updated-developer@acme.org',
                             name:'developer'
                         }
                     ]
                }""", response);
    }

    @Test
    public void simpleUpdateOfMultipleValues() throws Exception {
        withExtraFields(ExtraField.Email);

        final DSRequest request = JsonTestSupport.fromJSON(new TypeReference<>(){}, """
               {
                 dataSource : "EmployeeDS",
                 operationType : "UPDATE",
                 data : {
                   id : 2,
                   name: 'updated-developer',
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
            final ObjectMapper mapper = Serde.createMapper();

            e.dumpContext_ifAny(sw, "  ", mapper.writerWithDefaultPrettyPrinter());
            System.out.println( sw.toString());
            throw new RuntimeException(e);
        }

        JsonTestSupport.assertJsonEquals("""
                 {
                     status: 0,
                     data:[
                         {
                             id:2,
                             email:'updated-developer@acme.org',
                             name:'updated-developer'
                         }
                     ]
                }""", response);
    }

    @Test
    public void updateMustIgnoreMetaDataInOldValues() throws Exception {
        withExtraFields(ExtraField.Email);

        final DSRequest request = JsonTestSupport.fromJSON(new TypeReference<>(){}, """
               {
                 useStrictJSON : true,
                 dataSource : "EmployeeDS",
                 operationType : "UPDATE",
                 data : {                   
                   id : 2,
                   email: 'updated-developer@acme.org'
                 },
                 oldValues : {
                   id : 2,
                   name : 'developer',
                   email: 'developer@acme.org',
                   _theMetadata: 'yes-it-is-a-meta-data'
                 }
               }                
            """);

        final DSResponse response = handler.handleUpdate(request);
//        queueStatus:0,
//                startRow: 0,
//                endRow: 1,
//                totalRows: 1,

        JsonTestSupport.assertJsonEquals("""
                 {
                     status: 0,
                     data:[
                         {
                             id:2,
                             email:'updated-developer@acme.org',
                             name:'developer'
                         }
                     ]
                }""", response);
    }

    @Test
    public void updateMustFails_if_subsequentFetchFails() throws Exception {
        withExtraFields(ExtraField.Email);

        final DSRequest request = JsonTestSupport.fromJSON(new TypeReference<>(){}, """
               {
                 useStrictJSON : true,
                 dataSource : "EmployeeDS",
                 operationType : "UPDATE",
                 textMatchStyle : "EXACT",
                 data : {                   
                   id : 2,
                   email: 'updated-developer@acme.org'
                 },
                 oldValues : {
                   id : 2,
                   unexisted_field : 'developer',
                   email: 'developer@acme.org'
                 }
               }                
            """);


        final Exception ex = Assertions.assertThrows(Exception.class, () -> {
            handler.handleUpdate(request);
        });


        Assertions.assertTrue( ex.getMessage().contains("unexisted_field"));
    }
}

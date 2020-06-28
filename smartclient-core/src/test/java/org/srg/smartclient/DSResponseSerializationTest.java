package org.srg.smartclient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSResponse;

import java.util.Arrays;
import java.util.LinkedList;

public class DSResponseSerializationTest {

    @BeforeAll
    public static void setupJsonMapper() {
        JsonTestSupport.defaultMapper = JsonSerde.createMapper();
    }

    @Test
    public void deserializeSuccessfulEmptyResponse() {
        final DSResponse response = DSResponse.success(0, 0, 0,
                Arrays.asList(
                        new DSField()
                                .setName("name")
                                .setType(DSField.FieldType.TEXT)
                                .setDbName("name"),
                        new DSField()
                                .setName("client")
                                .setRequired(true)
                                .setType(DSField.FieldType.INTEGER)
                                .setForeignKey("ClientDS.id")
                                .setDbName("client_id"),
                        new DSField()
                                .setName("id")
                                .setRequired(true)
                                .setPrimaryKey(true)
                                .setType(DSField.FieldType.INTEGER)
                                .setDbName("id")
                ),
                new LinkedList<>()
        );


        JsonTestSupport.assertJsonEquals("""
            {
                response:{
                    status:0,
                    startRow:0,
                    endRow:0,
                    totalRows:0,
                    data:[]
            }
           }""", response);

    }

    @Test
    public void deserializeSuccessfulResponse() {
        final DSResponse response = DSResponse.success(0,1,
                Arrays.asList(
                        new DSField()
                            .setName("field1")
                            .setType(DSField.FieldType.INTEGER)
                            .setPrimaryKey(true)
                        ,
                        new DSField()
                                .setName("field2")
                                .setType(DSField.FieldType.TEXT)
                ),
                Arrays.asList(
                        new Object[]{24, "24"},
                        new Object[]{42, "42"}
                )
            );


        JsonTestSupport.assertJsonEquals("""
                {
                    "response":{
                       "status":0,
                       "startRow":0,
                       "endRow":1,
                       "totalRows":-1,
                       "data":[
                          {
                             "field1":24,
                             "field2":"24"
                          },
                          {
                             "field1":42,
                             "field2":"42"
                          }
                       ]
                    }
                }
                """, response);
    }

}

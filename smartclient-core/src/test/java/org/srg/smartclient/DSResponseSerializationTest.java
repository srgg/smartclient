package org.srg.smartclient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DSResponseDataContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    @Test
    public void deserializeSuccessfulResponseWithEntityField() {
        final List<DSField> subEntityFields =
                Arrays.asList(
                        new DSField()
                                .setName("cityId")
                                .setType(DSField.FieldType.INTEGER)
                                .setPrimaryKey(true)
                        ,
                        new DSField()
                                .setName("cityName")
                                .setType(DSField.FieldType.TEXT)
                );

        final DSResponse response = DSResponse.success(0,2,
                Arrays.asList(
                        new DSField()
                                .setName("countryId")
                                .setType(DSField.FieldType.INTEGER)
                                .setPrimaryKey(true),
                        new DSField()
                                .setName("countryName")
                                .setType(DSField.FieldType.TEXT),
                        new DSField()
                                .setName("cities")
                                .setType(DSField.FieldType.ENTITY)
                ),
                Arrays.asList(
                        new Object[]{
                                1,
                                "Lithuania",
                                new DSResponseDataContainer.RawDataResponse(
                                        subEntityFields,
                                        Arrays.asList(
                                                    new Object[]{1, "Vilnius"},
                                                    new Object[]{4, "Kaunas"}
                                                )
                                )
                        },
                        new Object[]{2, "Latvia",
                                new DSResponseDataContainer.RawDataResponse(
                                        subEntityFields,
                                        Collections.singletonList(new Object[]{2, "Riga"})
                                )
                        }
                )
        );

        JsonTestSupport.assertJsonEquals("""
            {
                response:{
                    status:0,
                    startRow:0,
                    endRow:2,
                    totalRows:-1,
                    data:[
                        {
                            countryName:'Lithuania',
                            countryId:1,
                            cities:[
                                {
                                    cityId:1,
                                    cityName:'Vilnius'
                                },
                                {
                                    cityId:4,
                                    cityName:'Kaunas'
                                }
                            ]
                        },
                        {
                            countryName:'Latvia',
                            countryId:2,
                            cities:[
                                {
                                    cityId:2,
                                    cityName:'Riga'
                                }
                            ]
                        }
                    ]
                }
            }""", response);

    }
}

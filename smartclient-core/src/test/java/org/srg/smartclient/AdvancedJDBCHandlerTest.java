package org.srg.smartclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;

import java.util.stream.Stream;

public class AdvancedJDBCHandlerTest extends AbstractJDBCHandlerTest<AdvancedJDBCHandler> {
    private static Stream<? extends Arguments> provideArgs() {
        return Stream.of(
                basicCriteria()
                ,compositeAndCaseInsensitiveCriteria()
                ,compositeOrCaseSensitiveCriteria()
                ,iNotContainsCriteria()
                ,equalsCriteria()
                ,notEqualFetch()
        );
    }

    @BeforeEach
    @Override
    public void setupDataSources() throws Exception {
        super.setupDataSources();
        withExtraFields(ExtraField.FiredAt);
    }

    @Override
    protected Class<AdvancedJDBCHandler> getHandlerClass() {
        return AdvancedJDBCHandler.class;
    }

    @ParameterizedTest(name = "run #{index}: {0}")
    @MethodSource("provideArgs")
    public void testAdvancedCriteria(ArgumentsAccessor argumentsAccessor) throws Exception {
        final String requestedCriteria = argumentsAccessor.getString(1);
        final String expected = argumentsAccessor.getString(2);
        final ExtraField[] extraFields;

        Object arg3 = null;

        if (argumentsAccessor.size() >= 4) {
            arg3 = argumentsAccessor.get(3);
        }

        if (arg3 == null) {
            extraFields = null;
        } else if (arg3.getClass().isArray()) {
            extraFields = (ExtraField[]) arg3;
        } else {
            extraFields = new ExtraField[]{(ExtraField) arg3};
        }

        if (extraFields != null && extraFields.length > 0) {
            withExtraFields(extraFields);
        }

        final DSRequest request = new DSRequest();
        request.setStartRow(0);
//        request.setEndRow(2);

        final AdvancedCriteria ac = JsonTestSupport.fromJSON(AdvancedCriteria.class, requestedCriteria);
        request.setData(ac);

        final DSResponse response = handler.handleFetch(request);
        JsonTestSupport.assertJsonEquals(expected, response);
    }

    private static Arguments basicCriteria() {
        return Arguments.of(
                "basic criteria",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [ 
                                {
                                    "fieldName" : "firedAt",
                                    "operator" : "notBlank",
                                    "_constructor" : "AdvancedCriteria"
                                }
                            ]
                        }""",
                """
                        {
                            response:{
                                data:[
                                    {
                                        id:1,
                                        name:'admin',
                                        firedAt: '2000-01-02T01:04:05.000+00:00'
                                    },
                                    {
                                        id:5,
                                        name:'user5',
                                        firedAt: '2000-05-04T00:02:01.000+00:00'
                                    }
                                ],
                                endRow:2,
                                startRow:0,
                                status:0,
                                totalRows:2
                            }
                        }""",
                null
        );
    }

    private static Arguments compositeAndCaseInsensitiveCriteria() {
        return  Arguments.of(
                "composite AND case insensitive criteria",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [ 
                                {
                                    "fieldName" : "firedAt",
                                    "operator" : "notBlank",
                                    "_constructor" : "AdvancedCriteria"
                                },
                                {
                                    "fieldName":"email",
                                    "operator":"iEndsWith",
                                    "value":"E.org"
                                 }
                            ]
                        }""",
                """
                        {
                            response:{
                                data:[
                                    {
                                        id:1,
                                        name:'admin',
                                        firedAt: '2000-01-02T01:04:05.000+00:00',
                                        email:"admin@acmE.org"
                                    },
                                    {
                                        id:5,
                                        name:'user5',
                                        firedAt: '2000-05-04T00:02:01.000+00:00',
                                        email:"u5@acme.org"
                                    }
                                ],
                                endRow:2,
                                startRow:0,
                                status:0,
                                totalRows:2
                            }
                        }""",
                ExtraField.Email
        );
    }

    private static Arguments compositeOrCaseSensitiveCriteria() {
        return Arguments.of(
                "composite OR case sensitive criteria",
                """
                        {
                            "operator" : "or",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [ 
                                {
                                    "fieldName" : "firedAt",
                                    "operator" : "notBlank",
                                    "_constructor" : "AdvancedCriteria"
                                },
                                {
                                    "fieldName":"email",
                                    "operator":"endsWith",
                                    "value":"E.org"
                                 }
                            ]
                        }""",
                """
                    {
                        response:{
                            data:[
                                {
                                    id:1,
                                    name:'admin',
                                    firedAt: '2000-01-02T01:04:05.000+00:00',
                                    email:"admin@acmE.org"
                                },
                                {
                                    id:4,
                                    name:'user4',
                                    firedAt: null,
                                    email:"u4@acmE.org"
                                },
                                {
                                    id:5,
                                    name:'user5',
                                    firedAt: '2000-05-04T00:02:01.000+00:00',
                                    email:"u5@acme.org"
                                }
                            ],
                            endRow:3,
                            startRow:0,
                            status:0,
                            totalRows:3
                        }
                    }""",
                ExtraField.Email
        );
    }


    private static Arguments iNotContainsCriteria() {
        return Arguments.of(
                "iNotContains condition",
                """
                        {
                            "operator" : "or",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [ 
                                {
                                    "fieldName":"email",
                                    "operator":"iNotContains",
                                    "value":"acme",
                                    "_constructor":"AdvancedCriteria"
                                }
                            ]
                        }""" ,
                """
                        {
                            response:{
                                data:[
                                    {
                                        id:3,
                                        name:'user3',
                                        firedAt: null,
                                        email:"u3@emca.org"
                                    }
                                ],
                                endRow:1,
                                startRow:0,
                                status:0,
                                totalRows:1
                            }
                        }""",
                ExtraField.Email
        );
    }

    private static Arguments equalsCriteria() {
        return Arguments.of(
                "Equals condition",
                """
                        {
                            "operator" : "or",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"name",
                                    "operator":"equals",
                                    "value":"user3",
                                    "_constructor":"AdvancedCriteria"
                                }
                            ]
                        }""",
                """
                        {
                            response:{
                                data:[
                                    {
                                        id:3,
                                        name:'user3',
                                        firedAt: null
                                    }
                                ],
                                endRow:1,
                                startRow:0,
                                status:0,
                                totalRows:1
                            }
                        }"""
        );
    }

    private static Arguments notEqualFetch() {
        return Arguments.of(
                "notEqual condition",
                """
                        {
                            "operator" : "or",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"name",
                                    "operator":"notEqual",
                                    "value":"user3",
                                    "_constructor":"AdvancedCriteria"
                                }
                            ]
                        }""",
                """
            {
                response:{
                    data:[
                        {
                            id:1,
                            name:'admin',
                            firedAt: '2000-01-02T01:04:05.000+00:00'
                        },
                        {
                            id:2,
                            name:'developer',
                            firedAt: null
                        },
                        {
                            id:4,
                            name:'user4',
                            firedAt: null
                        },
                        {
                            id:5,
                            name:'user5',
                            firedAt: '2000-05-04T00:02:01.000+00:00'
                        }
                    ],
                    endRow:4,
                    startRow:0,
                    status:0,
                    totalRows:4
                }
            }"""
        );
    }

//    private static Arguments compositeAndCaseInsensitiveCriteria() {
//        return Arguments.of();
//    }
}

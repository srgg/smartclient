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
                ,notEqualCriteria()
                ,iEqualsCriteria()
                ,iContains_by_SQLCalculatedField_Criteria()
                ,iStartsWithCriteria()
                ,startsWithCriteria()
                ,betweenInclusiveCriteria()
                ,betweenCriteria()
                ,greaterOrEqualCriteria()
                ,lessOrEqualCriteria()
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
                            data:[
                                {
                                    id:1,
                                    name:'admin',
                                    firedAt: '2000-01-02T01:04:05.000+00:00'
                                },
                                {
                                    id:5,
                                    name:'manager2',
                                    firedAt: '2000-05-04T00:02:01.000+00:00'
                                }
                            ],
                            endRow:2,
                            startRow:0,
                            status:0,
                            totalRows:2
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
                            data:[
                                {
                                    id:1,
                                    name:'admin',
                                    firedAt: '2000-01-02T01:04:05.000+00:00',
                                    email:"admin@acmE.org"
                                },
                                {
                                    id:5,
                                    name:'manager2',
                                    firedAt: '2000-05-04T00:02:01.000+00:00',
                                    email:"pm2@acme.org"
                                }
                            ],
                            endRow:2,
                            startRow:0,
                            status:0,
                            totalRows:2
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
                        data:[
                            {
                                id:1,
                                name:'admin',
                                firedAt: '2000-01-02T01:04:05.000+00:00',
                                email:"admin@acmE.org"
                            },
                            {
                                id:4,
                                name:'manager1',
                                firedAt: null,
                                email:"pm1@acmE.org"
                            },
                            {
                                id:5,
                                name:'manager2',
                                firedAt: '2000-05-04T00:02:01.000+00:00',
                                email:"pm2@acme.org"
                            }
                        ],
                        endRow:3,
                        startRow:0,
                        status:0,
                        totalRows:3
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
                            data:[
                                {
                                    id:3,
                                    name:'UseR3',
                                    firedAt: null,
                                    email:"u3@emca.org"
                                },
                                {
                                    id:6,
                                    name:'user2',
                                    firedAt: null,
                                    email:"u2@emca.org"
                                }
                            ],
                            endRow:2,
                            startRow:0,
                            status:0,
                            totalRows:2
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
                                    "value":"developer",
                                    "_constructor":"AdvancedCriteria"
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:2,
                                    name:'developer',
                                    firedAt: null
                                }
                            ],
                            endRow:1,
                            startRow:0,
                            status:0,
                            totalRows:1
                        }"""
        );
    }

    private static Arguments iEqualsCriteria() {
        return Arguments.of(
                "iEquals condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"name",
                                    "operator":"iEquals",
                                    "value":"uSEr3",
                                    "_constructor":null
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:3,
                                    name:'UseR3',
                                    firedAt:null
                                }
                            ],
                            endRow:1,
                            startRow:0,
                            status:0,
                            totalRows:1
                        }"""
        );
    }

    private static Arguments notEqualCriteria() {
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
                        id:3,
                        name:'UseR3',
                        firedAt: null
                    },                        
                    {
                        id:4,
                        name:'manager1',
                        firedAt: null
                    },
                    {
                        id:5,
                        name:'manager2',
                        firedAt: '2000-05-04T00:02:01.000+00:00'
                    },
                    {
                        id:6,
                        name:'user2',
                        firedAt: null
                    }              
                ],
                endRow:6,
                startRow:0,
                status:0,
                totalRows:6
            }"""
        );
    }

    private static Arguments iContains_by_SQLCalculatedField_Criteria() {
        return Arguments.of(
                "iContains by sql calculated field",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"calculated",
                                    "operator":"iContains",
                                    "value":"seR3",
                                    "_constructor":null
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    calculated:'3_UseR3',
                                    firedAt:null,
                                    id:3,
                                    name:'UseR3'
                                }
                            ],
                            endRow:1,
                            startRow:0,
                            status:0,
                            totalRows:1
                        }""",
                ExtraField.SqlCalculated
        );
    }

    private static Arguments startsWithCriteria() {
        return Arguments.of(
                "startsWith condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"name",
                                    "operator":"startsWith",
                                    "value":"Use",
                                    "_constructor":null
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:3,
                                    name:'UseR3',
                                    firedAt:null
                                }
                            ],
                            endRow:1,
                            startRow:0,
                            status:0,
                            totalRows:1
                        }"""
        );
    }


    private static Arguments iStartsWithCriteria() {
        return Arguments.of(
                "iStartsWith condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"name",
                                    "operator":"iStartsWith",
                                    "value":"Use",
                                    "_constructor":null
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:3,
                                    name:'UseR3',
                                    firedAt:null
                                },
                                {
                                    id:6,
                                    name:'user2',
                                    firedAt:null
                                }
                            ],
                            endRow:2,
                            startRow:0,
                            status:0,
                            totalRows:2
                        }"""
        );
    }



    private static Arguments betweenInclusiveCriteria() {
        return Arguments.of(
                "betweenInclusiveCriteria condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName" : "firedAt",
                                    "operator":"betweenInclusive",
                                    "start":"1970-10-01T00:00:00.000",
                                    "end":"2000-05-04T00:02:01.000+00:00",
                                    "_constructor":null
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:1,
                                    name:'admin',
                                    firedAt: "2000-01-02T01:04:05.000+00:00"
                                },
                                {
                                    id:5,
                                    name:'manager2',
                                    firedAt: "2000-05-04T00:02:01.000+00:00"
                                }
                            ],
                            endRow:2,
                            startRow:0,
                            status:0,
                            totalRows:2
                        }"""
        );
    }

    private static Arguments betweenCriteria() {
        return Arguments.of(
                "betweenCriteria condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName" : "firedAt",
                                    "operator":"between",
                                    "start":"1970-10-01T00:00:00.000",
                                    "end":"2000-05-04T00:02:01.000+00:00",
                                    "_constructor":null
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:1,
                                    name:'admin',
                                    firedAt: "2000-01-02T01:04:05.000+00:00"
                                }
                            ],
                            endRow:1,
                            startRow:0,
                            status:0,
                            totalRows:1
                        }"""
        );
    }

    private static Arguments greaterOrEqualCriteria() {
        return Arguments.of(
                "greaterOrEqual condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"firedAt",
                                    "operator":"greaterOrEqual",
                                    "value":"2000-05-04T00:02:01.000+00:00"
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:5,
                                    name:'manager2',
                                    firedAt: '2000-05-04T00:02:01.000+00:00'
                                }
                            ],
                            endRow:1,
                            startRow:0,
                            status:0,
                            totalRows:1
                        }"""
        );
    }

    private static Arguments lessOrEqualCriteria() {
        return Arguments.of(
                "lessOrEqual condition",
                """
                        {
                            "operator" : "and",
                            "_constructor" : "AdvancedCriteria",
                            "criteria" : [
                                {
                                    "fieldName":"firedAt",
                                    "operator":"lessOrEqual",
                                    "value":"2000-05-04T00:02:01.000+00:00"
                                }
                            ]
                        }""",
                """
                        {
                            data:[
                                {
                                    id:1,
                                    name:'admin',
                                    firedAt: "2000-01-02T01:04:05.000+00:00"
                                },
                                {
                                    id:5,
                                    name:'manager2',
                                    firedAt: '2000-05-04T00:02:01.000+00:00'
                                }
                            ],
                            endRow:2,
                            startRow:0,
                            status:0,
                            totalRows:2
                        }"""
        );
    }

//    private static Arguments compositeAndCaseInsensitiveCriteria() {
//        return Arguments.of();
//    }
}

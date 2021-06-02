package org.srg.smartclient.dmi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.IHandler;
import org.srg.smartclient.JsonTestSupport;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.isomorphic.ServerObject;
import org.srg.smartclient.utils.Serde;

public class JDKDMITest {
    private String TEST_DATASOURCE = """
            {
                id: 'TestDMIDS',
                tableName: 'tr',
                serverObject: {
                  lookupStyle: 'NEW',
                  className: 'org.srg.smartclient.dmi.StaticDMI',
                  methodName: 'staticDMI1arg'                  
                },
                fields:[
                    {
                        name:"data"
                        ,type:"INTEGER"
                    }
                ]
            }""";

    private JDKDMIHandlerFactory handlerFactory;

    @BeforeAll
    public static void setupDB() {
        JsonTestSupport.defaultMapper = Serde.createMapper();
    }

    @BeforeEach
    public void initializeFactory() {
        handlerFactory = new JDKDMIHandlerFactory();
    }

    @Test
    public void lookup_New_Static_1ArgMethod() throws Exception {
        final DataSource ds = JsonTestSupport.fromJSON(DataSource.class, TEST_DATASOURCE);
        final IHandler handler = handlerFactory.createDMIHandler(ds.getServerObject());

        final DSResponse response = handler.handle(null);

        JsonTestSupport.assertJsonEquals("""
            {
              status:0,
              startRow:0,
              endRow:1,
              totalRows:1,
              data:[
                {
                    data: 42
                }
              ]
            }
            """, response);
    }

    @Test
    public void lookup_0Arg_Factory_1ArgMethod() throws Exception {
        final DataSource ds = JsonTestSupport.fromJSON(DataSource.class, TEST_DATASOURCE);
        final ServerObject so = ds.getServerObject();
        so.setLookupStyle(ServerObject.LookupStyle.Factory);
        so.setClassName("org.srg.smartclient.dmi.DMIFactory_With_Zero_Arg");
        so.setMethodName("oneArg");

        final IHandler handler = handlerFactory.createDMIHandler(ds.getServerObject());

        final DSResponse response = handler.handle(null);

    }
}

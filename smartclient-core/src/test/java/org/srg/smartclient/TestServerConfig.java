package org.srg.smartclient;

import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.Config;

import java.io.IOException;


public class TestServerConfig {

    @Test
    public void load() throws IOException {
        Config cfg = Config.get();

        JsonTestSupport.assertJsonEquals(
                "{" +
                        "   sql:{" +
                        "      defaultDatabase:'test1'," +
                        "      connections:{" +
                        "         test2:{" +
                        "            name:'test2'," +
                        "            database:{" +
                        "               type:'h2'," +
                        "               ansiMode:false" +
                        "            }," +
                        "            driverClass:'org.h2.jdbcx.JdbcDataSource'," +
                        "            driverProperties:{" +
                        "               password:'sa'," +
                        "               user:'sa'," +
                        "               url:'jdbc:h2:mem:test:~/test'" +
                        "            }" +
                        "         }," +
                        "         test1:{" +
                        "            name:'test1'," +
                        "            database:{" +
                        "               type:'mysql'," +
                        "               ansiMode:false" +
                        "            }," +
                        "            driverClass:'com.mysql.jdbc.jdbc2.optional.MysqlDataSource'," +
                        "            driverProperties:{" +
                        "               url:'jdbc:mysql://host1:33060/test'" +
                        "            }" +
                        "         }" +
                        "      }" +
                        "   }" +
                        "}", cfg
        );
    }
}

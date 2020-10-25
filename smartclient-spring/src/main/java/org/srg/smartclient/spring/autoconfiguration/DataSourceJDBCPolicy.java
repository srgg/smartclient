package org.srg.smartclient.spring.autoconfiguration;

import org.slf4j.LoggerFactory;
import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.utils.Utils;

import javax.sql.DataSource;
import java.sql.Connection;

public class DataSourceJDBCPolicy implements JDBCHandler.JDBCPolicy {
    private DataSource dataSource;

    public DataSourceJDBCPolicy(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void withConnectionDo(String database, Utils.CheckedFunction<Connection, Void> callback) throws Exception {
        // https://www.postgresql.org/message-id/CAKq0gv%2BwqDv6cmF__XR1sEY3wun0V2FQ4HenG%2BEc073xOU394Q%40mail.gmail.com
        try (final Connection connection = dataSource.getConnection() ){
            if (connection.getAutoCommit()) {
                LoggerFactory.getLogger(DataSourceJDBCPolicy.class).warn(
                        "Setting DB Connection autocommit to false. Consider to configure this in db connection."
                );
                connection.setAutoCommit(false);
            }
            callback.apply(connection);
        }
    }
}

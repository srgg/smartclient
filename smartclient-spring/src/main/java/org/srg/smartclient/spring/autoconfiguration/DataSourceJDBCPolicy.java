package org.srg.smartclient.spring.autoconfiguration;

import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.Utils;

import javax.sql.DataSource;
import java.sql.Connection;

public class DataSourceJDBCPolicy implements JDBCHandler.JDBCPolicy {
    private DataSource dataSource;

    public DataSourceJDBCPolicy(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void withConnectionDo(String database, Utils.CheckedFunction<Connection, Void> callback) throws Exception {
        try (final Connection connection = dataSource.getConnection() ){
            callback.apply(connection);
        }
    }
}

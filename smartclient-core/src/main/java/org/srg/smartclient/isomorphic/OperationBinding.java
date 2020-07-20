package org.srg.smartclient.isomorphic;

/**
 * @see <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..OperationBinding.ansiJoinClause">OperationBinding</a>
 * @see <a href="</>https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/OperationBinding.html">OperationBinding</a>
 *
 * @see <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=group..customQuerying">Custom Querying Overview</a>
 * @see <a href="https://www.smartclient.com/smartgwtee-5.1/javadoc/com/smartgwt/client/docs/CustomQuerying.html">Custom Querying</a>
 * @see <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=type..DefaultQueryClause"></a>
 *
 */
public class OperationBinding {
    private DSRequest.OperationType operationType;
    private String operationId = "";

    /**
     * For a dataSource of serverType "sql", this property can be specified on an operationBinding to provide the
     * server with a bespoke table clause to use when constructing the SQL query to perform this operation.
     *
     * The property should be a comma-separated list of tables and views, and you can use any special
     * language constructs supported by the underlying database. The server will insert the text of this property
     * immediately after the "FROM" token.
     *
     * See the documentation for <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..OperationBinding.customSQL">customSQL</a> for usage examples
     */
    private String tableClause = "";

    /**
     * For a dataSource of serverType "sql", this property can be specified on an operationBinding to provide the
     * server with a bespoke WHERE clause to use when constructing the SQL query to perform this operation.
     *
     * The property should be a valid expression in the syntax of the underlying database. The server will insert
     * the text of this property immediately after the "WHERE" token.
     *
     * You may find the SmartClient-provided $criteria variable of particular use with this property.
     *
     * See the documentation for <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..OperationBinding.customSQL">customSQL</a> for usage examples
     */
    private String whereClause = "";

    /**
     * For a dataSource of serverType "sql", this property can be specified on an operationBinding to provide the server with a bespoke ANSI-style joins clause to use when constructing the SQL query to perform this operation. The property should be a set of joins implemented with JOIN directives (as opposed to additional join expressions in the where clause), joining related tables to the main table or view defined in tableClause. The server will insert the text of this property immediately after the tableClause.
     *
     * See the documentation for <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..OperationBinding.customSQL">customSQL</a> for usage examples
     */
    private String ansiJoinClause = "";

    public DSRequest.OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(DSRequest.OperationType operationType) {
        this.operationType = operationType;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getTableClause() {
        return tableClause;
    }

    public void setTableClause(String tableClause) {
        this.tableClause = tableClause;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public String getAnsiJoinClause() {
        return ansiJoinClause;
    }

    public void setAnsiJoinClause(String ansiJoinClause) {
        this.ansiJoinClause = ansiJoinClause;
    }
}

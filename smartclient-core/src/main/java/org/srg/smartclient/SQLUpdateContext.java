package org.srg.smartclient;

import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.OperationBinding;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SQLUpdateContext<H extends JDBCHandler> extends JDBCHandler.AbstractSQLContext {
    private String updateSQL;

    private List<JDBCHandler.IFilterData> modifiedData;
    private List<JDBCHandler.IFilterData> pkFieldData;

    public SQLUpdateContext(H dsHandler, DSRequest request, OperationBinding operationBinding) throws Exception {
        super(dsHandler, request, operationBinding);
        init();
    }

    protected void init() {
        final List<JDBCHandler.IFilterData> filterData = dsHandler().generateFilterData(DSRequest.OperationType.UPDATE, DSRequest.TextMatchStyle.EXACT, request().getData());
        final Map<Boolean, List<JDBCHandler.IFilterData>> m = filterData.stream()
                .collect(Collectors.groupingBy( (JDBCHandler.IFilterData fd) -> ((JDBCHandler.FilterData)fd).field().isPrimaryKey()));

        this.modifiedData = m.get(Boolean.FALSE);
        this.pkFieldData = m.get(Boolean.TRUE);


        final String whereSQL = pkFieldData.stream()
                .map(fd -> fd.sql())
                .collect(Collectors.joining("\n\t\t AND "));

        /*
         * Column names within the SET clause must not be prefixed with either table alias nor table name.
         * Otherwise it will cause an error.
         */
        final String setSQL = modifiedData.stream()
                .map(fd -> fd.sql(null))
                .collect(Collectors.joining("\n\t\t AND "));

        this.updateSQL = """
                        UPDATE  %s 
                            SET %s
                        WHERE %s;                            
                    """.formatted(
                dataSource().getTableName(),
                setSQL,
                whereSQL
        );
    }

    public String getUpdateSQL() {
        return updateSQL;
    }

    public Map<String, Object> getModifiedFields() {
        return this.convertFilterDataToMap(getModifiedData());
    }

    public Map<String, Object> getPkValues() {
        return this.convertFilterDataToMap(pkFieldData);
    }

    public List<JDBCHandler.IFilterData> getModifiedData() {
        assert modifiedData != null;
        return modifiedData;
    }

    public List<JDBCHandler.IFilterData> getPkFieldData() {
        assert pkFieldData != null;
        return pkFieldData;
    }
}

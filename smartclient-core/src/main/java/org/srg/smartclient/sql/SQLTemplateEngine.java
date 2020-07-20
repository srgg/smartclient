package org.srg.smartclient.sql;

import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.OperationBinding;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;

import java.util.HashMap;
import java.util.Map;

/**
 * @see <a hretf="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=group..customQuerying">Custom Querying Overview</a>
 * @see <ahref="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=type..DefaultQueryClause">DefaultQueryClause</a>
 */
public class SQLTemplateEngine {

    public static boolean isTemplateEngineRequired(JDBCHandler handler, DSRequest request) {
        final OperationBinding operationBinding = handler.getEffectiveOperationBinding(request.getOperationType());

        final boolean potentiallyRequiresTemplateEngine = operationBinding != null && !(
                operationBinding.getAnsiJoinClause().isBlank()
                        || operationBinding.getTableClause().isBlank()
                        || operationBinding.getWhereClause().isBlank()
        );

        return potentiallyRequiresTemplateEngine;
    }

    public static Map<String, Object > createContext(
            DSRequest request,
            String selectClause,
            String fromClause,
            String whereClause,
            String orderClause
    ) {
        final Map<String, Object> ctx = new HashMap<>();

        ctx.put("defaultSelectClause",selectClause);
        ctx.put("defaultTableClause", fromClause);
        ctx.put("defaultWhereClause", whereClause);
        ctx.put("defaultOrderClause", orderClause);

        //                "defaultAnsiJoinClause", null,
//                "defaultValuesClause", null,


        if (DSRequest.OperationType.FETCH.equals(request.getOperationType())) {
            if (request.getData() instanceof Map criteria) {
                ctx.put("criteria", criteria);
                ctx.put("advancedCriteria", criteria);
            } else if (request.getData() instanceof AdvancedCriteria ac) {
                ctx.put("advancedCriteria", ac);
            }
        }

        return ctx;
    }
}

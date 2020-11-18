package org.srg.smartclient;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;
import org.srg.smartclient.isomorphic.criteria.Criteria;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**

 * @see <a hretf="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=group..customQuerying">Custom Querying Overview</a>
 * @see <ahref="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=type..DefaultQueryClause">DefaultQueryClause</a>
 */
public class SQLTemplateEngine {

    public static String processSQL(Map<String,Object> context, String sql) throws IOException, TemplateException {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        final Template template = new Template("t", new StringReader(sql), cfg);
        final Writer out = new StringWriter();
        template.process(context, out);
        return out.toString();
    }

    private static Map<String, Object> populateAdvancedCriteriaMap(Map<String, Object> values, Criteria ac) {
        if (ac.getFieldName() != null && !ac.getFieldName().isBlank()) {
            if (ac.getCriteria() != null && !ac.getCriteria().isEmpty()) {
                throw new IllegalStateException(
                        "Hm, I was sure that this case is impossible, but if you got this error, I need to rethink this"
                );
            }

            values.put(ac.getFieldName(), ac.getValue());
        } else {
            for (Criteria c :ac.getCriteria()){
                values = populateAdvancedCriteriaMap(values, c);
            }
        }

        return values;
    }

    public static Map<String, Object > createContext(
            DSRequest request,
            String selectClause,
            String fromClause,
            String joinClause,
            String whereClause,
            String orderClause
    ) {
        final Map<String, Object> ctx = new HashMap<>();

        ctx.put("defaultSelectClause",selectClause);
        ctx.put("defaultTableClause", fromClause);
        ctx.put("defaultWhereClause", whereClause);
        ctx.put("defaultAnsiJoinClause", joinClause);
        ctx.put("defaultOrderClause", orderClause);

        if (DSRequest.OperationType.FETCH.equals(request.getOperationType())) {
            if (request.getData() instanceof Map criteria) {
                ctx.put("criteria", criteria);
                ctx.put("advancedCriteria", criteria);
            } else if (request.getData() instanceof AdvancedCriteria ac) {
                final Map<String, Object> values = populateAdvancedCriteriaMap(new HashMap<>(), ac);
                ctx.put("advancedCriteria", values);
            }
        }

        return ctx;
    }
}

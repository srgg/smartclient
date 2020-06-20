package org.srg.smartclient;

//https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/data/RestDataSource.html
// https://forums.smartclient.com/forum/smart-gwt-technical-q-a/8944-loading-datasource-using-client-side-ds-xml-for-demo-without-a-server

// server_properties
// https://www.smartclient.com/smartclient-10.0/isomorphic/system/reference/SmartClient_Reference.html?ref=group:iscInstall#group..server_properties
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.annotations.SmartClientField;
import org.srg.smartclient.annotations.SmartClientHandler;
import org.srg.smartclient.isomorphic.*;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class DSDispatcher implements IDSDispatcher {
    private static Logger logger = LoggerFactory.getLogger(DSDispatcher.class);

    private Map<String, DSHandler> datasourceMap = new LinkedHashMap<>();

    public DSDispatcher() {
    }

    protected JDBCHandler.JDBCPolicy getJdbcPolicy() {
        return DBConnectionManager.get();
    }

    protected DSHandler getHandlerByName(String dsId) {
        final DSHandler ds = datasourceMap.get(dsId);

        if (ds != null) {
            return ds;
        }

        return Utils.throw_it("Unknown datasource '%s'", dsId);
    }

    @Override
    public DataSource dataSource(String dsId) {
        return getHandlerByName(dsId).dataSource();
    }

    protected DSResponse handleRequest(DSRequest request) {
        try {
            final DSHandler ds = getHandlerByName(request.getDataSource());
            final DSResponse response = ds.handle(request);

            if (logger.isDebugEnabled()){
                final ObjectWriter maper = JsonSerde.createMapper()
                        .writerWithDefaultPrettyPrinter();

                String strRequest, strResponse;

                try {
                    strRequest = maper.writeValueAsString(request);
                } catch (JsonProcessingException ex) {
                    strRequest = "{Cant't serialize request: %s}".formatted(ex.getMessage());
                }

                try {
                    strResponse = maper.writeValueAsString(response);
                } catch (JsonProcessingException ex) {
                    strResponse = "{Cant't serialize response: %s}".formatted(ex.getMessage());
                }

                logger.debug("""
                        \n
                        -------------------------------------------------
                        - [DSDispatcher - %s]  %s
                        -------
                        Request:
                        %s
                        -------
                        Response:
                        %s\\n" +
                        -------------------------------------------------
                    """.formatted(
                        request.getOperationType(),
                        request.getDataSource(),
                        strRequest,
                        strResponse

                ));
            }
            return response;
        } catch (Throwable t) {
            String strRequest;
            try {
                strRequest = JsonSerde
                        .createMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request);
            } catch (JsonProcessingException ex) {
                strRequest = "{Cant't serialize request: %s}".formatted(ex.getMessage());
            }

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);

            logger.error("""
                \n
                -------------------------------------------------
                - [DSDispatcher]   Unhandled Exception
                -------
                Request:
                %s\n
                  --
                %s
                -------------------------------------------------                                                        
                """.formatted(
                    strRequest,
                    sw
                    )
            );
            return DSResponse.failure(t.getMessage());
        }
    }

    @Override
    public Collection<DSResponse> dispatch(IDSRequest request) {
        final LinkedList<DSResponse> responses = new LinkedList<>();

        for (DSRequest r: request) {
            final DSResponse response = handleRequest(r);
            responses.add(response);
        }

        return responses;
    }

    public <A extends Appendable> A generateDSJavaScript(A out, String dispatcherUrl, String... dsId) throws Exception {
        if (dsId.length == 0) {
            dsId = datasourceMap.keySet().toArray(new String[0]);
        }

        final AbstractDSDeclarationBuilder builder = new AbstractDSDeclarationBuilder() {
//            @Override
//            protected String getDataSourceNameByEntityType(Class javaClass) {
//                final IDataSourceOperationHandler ds = dsByClass.get(javaClass);
//                return ds == null ? null : ds.getName();
//            }
        };

        for (String name : dsId) {
            final DSHandler ds = getHandlerByName(name);

            out.append(builder.build(dispatcherUrl, ds));

//            if (ds instanceof AbstractDSHandler) {
//                Collection<DSField> allFields = ((AbstractDSHandler)ds).getFields();
//                out.append(builder.build(name, dispatcherUrl, allFields));
//            } else {
//                throw new RuntimeException(String.format("Can't generate DS Script: Data source with name '%s' is not an instance of AbstractDSHandler",
//                        name));
//            }
        }
        return out;
    }

    private DSHandler loadDsFromResource(File file) throws IOException {
//        final XmlMapper xmlMapper = new XmlMapper();
//        xmlMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//        xmlMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
//        final Datasource ds = xmlMapper.readValue(file, Datasource.class);

        final ObjectMapper mapper = new ObjectMapper()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        final DataSource ds = mapper.readValue(file, DataSource.class);
        switch (ds.getServerType()) {
            case SQL:
                return createJDBCHandler(ds);

            default:
                return Utils.throw_it("Can't load Data source '%s': serverType '%s' is not supported.",
                        ds.getId(), ds.getServerType());
        }
    }

    @Override
    public void loadFromResource(String path) {
        final URL url = Utils.getResource(path);
        if (url == null) {
            logger.debug("Datasource path does not set properly, loading datasource will be skipped.");
            return;
        }

        final String urlPath = url.getPath();
        File f = new File(urlPath);

        final String files[];

        if (f.isDirectory()) {
            files = f.list( (dir, name) -> name.endsWith(".ds.json"));
        } else {
            files = new String[]{f.getName()};
            f = new File(f.getParent());
        }

        for (String s: files ) {
            try {
                final DSHandler ds = loadDsFromResource(new File(f, s));
                registerDatasource(ds);
            } catch (Exception ex) {
                logger.warn("Can't load Smart Client DSHandler from file '%s'".formatted(s), ex);
            }
        }
    }


    protected JDBCHandler createJDBCHandler(DataSource ds) {
        return new AdvancedJDBCHandler( getJdbcPolicy(), this, ds);
    }

    @Override
    public void registerDatasource(DSHandler handler) {
        datasourceMap.put(handler.id(), handler);
        logger.info("A new DSHandler has been registered as '%s' ".formatted(handler.id()));
    }

    protected static String getDsId(Class<?> clazz) {
        final SmartClientHandler[] annotations = clazz.getAnnotationsByType(SmartClientHandler.class);

        if (annotations.length > 0) {
            final SmartClientHandler smartClientHandler = annotations[0];

            if (!smartClientHandler.value().isBlank()) {
                return smartClientHandler.value();
            }
        }

        return clazz.getSimpleName() +"DS";
    }

    protected <T> DSField describeField(String dsId, Field field) {
        final DSField f = new DSField();

        f.setName( field.getName() );

        try {
            f.setType(fieldType(field.getType()));

            if (f.getType() != null) {
                switch (f.getType()) {
                    case ENUM:
                    case INTENUM:
                        final Class<?> type = field.getType();
                        if (!type.isEnum()) {
                            throw new IllegalStateException();
                        }

                        f.setValueMapEnum(type.getCanonicalName());

                        Enum constants[] = (Enum[]) type.getEnumConstants();

                        final Map<Integer, String> vm = new HashMap<>(constants.length);

                        for (int i = 0; i < constants.length; ++i) {
                            vm.put(i, constants[i].name());
                        }

                        f.setValueMap(vm);
                        break;
                }
            }
        } catch (Throwable t) {
            logger.warn("DataSource '%s': Can't determine field type for field '%s'"
                    .formatted(dsId, f.getName()), t);
            f.setType(null);
        }

        final SmartClientField sfa = field.getAnnotation(SmartClientField.class);
        if (sfa != null) {

            if (!sfa.name().isBlank()) {
                f.setName( sfa.name());
            }

            if (!sfa.foreignDisplayField().isBlank()) {
                f.setForeignDisplayField(sfa.foreignDisplayField());
            }

            if (!sfa.displayField().isBlank()) {
                f.setDisplayField(sfa.displayField());
            }

            if (!sfa.type().equals(DSField.FieldType.ANY)) {
                f.setType(sfa.type());
            }

            if (sfa.hidden()) {
                f.setHidden(true);
            }

            if (!sfa.customSelectExpression().isBlank()) {
                f.setCustomSelectExpression(sfa.customSelectExpression());
            }
        }

        // -- set DB field name
        f.setDbName( f.getName());


        // Convert field name to snake case
        f.setDbName(
                f.getDbName().replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z])([A-Z])", "$1_$2")
        );

        return f;
    }

    protected <T> DataSource describeEntity(Class<T> entityClass) {
        final DataSource ds = new DataSource();
        final String id = getDsId(entityClass);
        ds.setId(id);
        ds.setServerType(DataSource.DSServerType.GENERIC);
        ds.setBeanClassName(entityClass.getCanonicalName());

        final List<Field> fields = FieldUtils.getAllFieldsList(entityClass);

        final List<DSField> dsFields = fields.stream()
                .map( f -> describeField(ds.getId(), f))
                .collect(Collectors.toList());

        ds.setFields(dsFields);
        return ds;
    }
    protected <X> DSField.FieldType fieldType(Class<X> clazz) {
        if (clazz.equals(Integer.class)
                || clazz.equals(Short.class)
                || clazz.equals(int.class)
                || clazz.equals(short.class)) {
            return DSField.FieldType.INTEGER;
        }

        if(clazz.equals(Long.class)
                || clazz.equals(long.class) ){
            return DSField.FieldType.INTEGER;
        }

        if (clazz.equals(String.class)) {
            return DSField.FieldType.TEXT;
        }

        if (clazz.equals(java.sql.Date.class)) {
            return DSField.FieldType.DATE;
        }

        if (clazz.equals(java.sql.Timestamp.class)) {
            return DSField.FieldType.DATETIME;
        }

        if( clazz.equals(Boolean.class)
                || clazz.equals(boolean.class) ){
            return DSField.FieldType.BOOLEAN;
        }

        if (clazz.isEnum()) {
            return DSField.FieldType.ENUM;
        }

        if (clazz.equals(java.sql.Time.class)) {
            return DSField.FieldType.TIME;
        }

        //throw new RuntimeException(String.format("Smart Client -- Unmapped field type %s.", clazz.getName()));
        return null;
    }

}

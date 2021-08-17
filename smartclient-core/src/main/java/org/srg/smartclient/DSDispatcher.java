package org.srg.smartclient;

//https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/data/RestDataSource.html
// https://forums.smartclient.com/forum/smart-gwt-technical-q-a/8944-loading-datasource-using-client-side-ds-xml-for-demo-without-a-server

// server_properties
// https://www.smartclient.com/smartclient-10.0/isomorphic/system/reference/SmartClient_Reference.html?ref=group:iscInstall#group..server_properties
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.dmi.JDKDMIHandlerFactory;
import org.srg.smartclient.isomorphic.*;
import org.srg.smartclient.runtime.DSRuntime;
import org.srg.smartclient.runtime.IDSRuntime;
import org.srg.smartclient.utils.ContextualRuntimeException;
import org.srg.smartclient.utils.Serde;
import org.srg.smartclient.utils.Utils;


import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DSDispatcher implements IDSDispatcher, IDSRuntime {
    private static Logger logger = LoggerFactory.getLogger(DSDispatcher.class);
    private DSRuntime dsRuntime = DSRuntime.create(Collections.emptyList());
    private JDBCHandlerFactory  jdbcHandlerFactory = new JDBCHandlerFactory();
    private JDBCHandler.JDBCPolicy jdbcPolicy;
    private JDKDMIHandlerFactory dmiHandlerFactory;

    public DSDispatcher() {
        this(DBConnectionManager.get());
    }

    public DSDispatcher(JDBCHandler.JDBCPolicy jdbcPolicy) {
        this.jdbcPolicy = jdbcPolicy;
        this.dmiHandlerFactory = new JDKDMIHandlerFactory();
    }

    protected JDBCHandler.JDBCPolicy getJdbcPolicy() {
        assert jdbcPolicy != null;
        return jdbcPolicy;
    }

    @Override
    public Iterator<IHandler> iterator() {
        return dsRuntime.iterator();
    }

    @Override
    public RelationSupport.ForeignKeyRelation getForeignKeyRelation(String dsId, String fieldName) {
        return dsRuntime.getForeignKeyRelation(dsId, fieldName);
    }

    @Override
    public RelationSupport.ImportFromRelation getImportFromRelation(String dsId, String fieldName) {
        return dsRuntime.getImportFromRelation(dsId, fieldName);
    }

    @Override
    public IHandler getHandlerById(String dsId) {
        return dsRuntime.getHandlerById(dsId);

//        if (ds != null) {
//            return ds;
//        }
//
//        return Utils.throw_it("Unknown datasource '%s'", dsId);
    }

    private ObjectWriter createObjectWriter() {
        final SimpleFilterProvider filterProvider = new SimpleFilterProvider();

        final DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        final DefaultIndenter indenter = new DefaultIndenter() {
            @Override
            public void writeIndentation(JsonGenerator jg, int level) throws IOException {
                super.writeIndentation(jg, level + 2);
            }
        };

        pp.indentArraysWith(indenter);
        pp.indentObjectsWith(indenter);

        ObjectMapper om = Serde.createMapper();

        final ContextAttributes attrs = om.getSerializationConfig().getAttributes()
                .withSharedAttribute(Serde.RawDataResponseSerializer.SERIALIZE_FIELDS_ONLY, logger.isTraceEnabled() ? null : Boolean.TRUE);

        SerializationConfig sc =  om.getSerializationConfig().with(attrs);
        om.setConfig(sc);

        ObjectWriter objectWriter = om
                .enable(SerializationFeature.INDENT_OUTPUT)

                .disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES)

                .addMixIn(DSResponse.class, PropertyFilterMixIn.class)
                .setFilterProvider(filterProvider)
                .writer(pp)
                .withAttribute(Serde.RawDataResponseSerializer.SERIALIZE_FIELDS_ONLY, logger.isTraceEnabled() ? null : Boolean.TRUE);


//        if (!logger.isTraceEnabled()) {
//            filterProvider.addFilter("PropertyFilter", SimpleBeanPropertyFilter.serializeAllExcept("data"));
//        } else {
            filterProvider.addFilter("PropertyFilter", SimpleBeanPropertyFilter.serializeAll());
//        }

        return objectWriter;
    }

    protected DSResponse handleRequest(DSRequest request) {
        try {
            final IHandler ds = getHandlerById(request.getDataSource());
            final DSResponse response = ds.handle(request);

            response.setOperationId( request.getOperationId());
//            response.setOperationType( request.getOperationType());

            if (logger.isDebugEnabled()){
                final ObjectWriter objectWriter = createObjectWriter();
                String strRequest, strResponse;

                try {
                    strRequest = objectWriter.writeValueAsString(request);
                } catch (JsonProcessingException ex) {
                    strRequest = "{Can't serialize request: %s}".formatted(ex.getMessage());
                }

                try {
                    strResponse = objectWriter.writeValueAsString(response);
                } catch (JsonProcessingException ex) {
                    strResponse = "{Can't serialize response: %s}".formatted(ex.getMessage());
                }

                logger.debug("""
                    
                    -------------------------------------------------
                    - [DSDispatcher - %s]  %s
                    -------------------------------------------------
                      Request:
                        %s
                    
                      Response:   
                        %s
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
            final ObjectWriter objectWriter = createObjectWriter();
            StringWriter contextWriter = null;

            if ( t instanceof ContextualRuntimeException crte) {
                contextWriter = new StringWriter();

                contextWriter.write("""                      
                      Context:                        
                    """);

                crte.dumpContext_ifAny(contextWriter, "    ", objectWriter);

                contextWriter.append("\n\n  -------");
            }

            String strRequest;
            try {
                strRequest = objectWriter.writeValueAsString(request);
            } catch (JsonProcessingException ex) {
                strRequest = "{Can't serialize request: %s}".formatted(ex.getMessage());
            }

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);

            logger.error("""
                                
                -------------------------------------------------
                - [DSDispatcher]  Unhandled Exception
                -------------------------------------------------
                  Request:
                    %s
                
                  -------%s  Stack Trace:                         
                    %s
                -------------------------------------------------
                """.formatted(
                        strRequest,
                        contextWriter == null ? "" : "%s".formatted(contextWriter),
                        sw
                    )
            );
            return DSResponse.failure(t.getMessage() == null ? t.getClass().getCanonicalName() : t.getMessage());
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
        out.append("const DISPATCHER_URL = \"%s\";\n"
                .formatted(dispatcherUrl));

        /*
         * If dsIds is not provided, -- generate all registered handlers
         */
        if (dsId.length == 0) {
            final List<String> ids = StreamSupport.stream(dsRuntime.spliterator(), false)
                    .map(IHandler::id)
                    .collect(Collectors.toList());

            dsId = ids.toArray(ids.toArray(new String[0]));
        }

        for (String name : dsId) {
            final IHandler handler = getHandlerById(name);

            if (handler instanceof DSHandler ds) {
                out.append(DSDeclarationBuilder.build(this, dispatcherUrl, ds));
            }
        }
        return out;
    }

    protected IHandler createHandler(DataSource ds) {
        switch (ds.getServerType()) {
            case SQL:
                return jdbcHandlerFactory.createJDBCHandler(getJdbcPolicy(), this, ds);

            case GENERIC:
                if (ds.getServerObject() == null) {
                    throw new IllegalStateException();
                }

                try {
                    return dmiHandlerFactory.createDMIHandler(ds.getServerObject());
                } catch (Exception ex) {
                    throw new RuntimeException("Data source '%s': Can't instantiate DMI handler: %s."
                            .formatted(ds.getId(), ds.getServerObject()));
                }

            default:
                return Utils.throw_it("Can't load Data source '%s': serverType '%s' is not supported.",
                        ds.getId(), ds.getServerType());
        }
    }

    private IHandler loadDsFromResource(File file) throws IOException {
//        final XmlMapper xmlMapper = new XmlMapper();
//        xmlMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//        xmlMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
//        final Datasource ds = xmlMapper.readValue(file, Datasource.class);

        final ObjectMapper mapper = new ObjectMapper()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());

        logger.debug("trying to load DataSource from '%s'...".formatted(file));

        final DataSource ds;

        final InputStream is = Utils.getResourceAsStream(file.getPath());
        if (is == null) {
            logger.debug("Can't load datasource from InputStream, trying to load from File object");
            ds = mapper.readValue(file, DataSource.class);
        } else {
            ds = mapper.readValue(is, DataSource.class);
        }

        if (DataSource.DSServerType.SQL.equals(ds.getServerType())
            && ( ds.getTableName() == null || ds.getTableName().isBlank()) ) {

            throw new RuntimeException("Can't load Data source '%s': Table name must be specified for a sql-based datasource."
                .formatted(ds.getId())
            );
        }

        return createHandler(ds);
    }

    @Override
    public void loadFromResource(String path) throws Exception {
        final URL url = Utils.getResource(path);

        if (url == null) {
            logger.debug("Datasource path does not set properly, loading datasource will be skipped.");
            return;
        }

        logger.debug("\n\nSCAN BUILTIN RESOURCES in '%s', url: %s\n".formatted(path, url));

        final Set<String> files = new HashSet<>();

        final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path + '/');
        while (resources.hasMoreElements()) {
            URL url1 = resources.nextElement();
            logger.info("Resource URL: %s".formatted( url1));

            if (url.getProtocol().equals("jar")) {
                logger.debug("Scanning a jar file via URL: %s...".formatted( url1));


                /* A JAR path */
                final String jarPath = url1.getPath().substring(5, url1.getPath().indexOf("!")); //strip out only the JAR file
                final String relativePath = url1.getPath().substring(5 + jarPath.length() + 2 /* '/!' */).replace("!", "");

                logger.debug("jarPath: '%s'".formatted(jarPath));
                logger.debug("relativePath: '%s'".formatted(relativePath));

                final Set<String> result = new HashSet<>();

                try (final JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8")) ) {
                    final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar

                    while (entries.hasMoreElements()) {
                        JarEntry je = entries.nextElement();
                        String name = je.getName();
                        logger.trace("analyzing jarEntry: '%s'".formatted(name));

                        if (name.startsWith(relativePath)) { //filter according to the path
                            logger.debug("found a match, jarEntry: '%s'".formatted(name));

                            if (je.isDirectory()) {
                                logger.debug("   skipped as it is a directory");
                                continue;
                            }

                            String entry = name.substring(path.length());
                            int checkSubdir = entry.lastIndexOf("/");
                            if (checkSubdir >= 0) {
                                // if it is a subdirectory, we just return the directory name
                                entry = entry.substring(checkSubdir + 1);
                            }

                            logger.debug("found a file in jar: '%s'".formatted(entry));
                            result.add(path + '/' + entry);
                        } else {
                            logger.trace("   ignore as it does not match a relative path.");
                        }
                    }
                }

                if (result.isEmpty()) {
                    logger.debug("Jar scan was completed, nothing ound (jar: '%s')".formatted(url1));
                } else {
                    logger.debug(
                            "Jar scan was completed, %d matches were found(jar: '%s'):\n%s"
                            .formatted(
                                    result.size(),
                                    url1,
                                    result.stream()
                                            .collect(Collectors.joining(",\n    ", "    ", ""))
                            )
                    );
                    files.addAll(result);
                }
            } else {
                File f = new File(url.getFile());

                List<String> result;
                if (f.isDirectory()) {

                    logger.debug("Scanning resource directory '%s'..."
                            .formatted(f));


                    result = Arrays.stream(f.list((dir, name) -> name.endsWith(".ds.json")))
                    .map(p -> path + '/' + p)
                    .collect(Collectors.toList());
                } else {
                    result = List.of(f.getName());
                }

                if (result.isEmpty()) {
                    logger.debug("Directory scan was completed, nothing found (jar: '%s')".formatted(url1));
                } else {
                    logger.debug("Directory scan was completed, %d matches were found(dir: '%s'):\n%s"
                            .formatted(
                                    result.size(),
                                    url1,
                                    result.stream()
                                            .collect(Collectors.joining(",\n    ","    ",""))
                            )
                    );
                }

                files.addAll(result);
            }
        }

        logger.info("BUILTIN RESOURCES SCAN COMPLETED, %d matches were found(path '%s'):\n%s"
                .formatted(
                        files.size(),
                        url,
                        files.stream()
                                .collect(Collectors.joining(",\n   ", "   ", ""))
                )
        );

        final List<IHandler>    handlers = new LinkedList<>();
        for (String s: files ) {
            try {
                final IHandler ds = loadDsFromResource(new File(s));
                handlers.add(ds);
            } catch (Exception ex) {
                logger.warn("Can't load Smart Client DSHandler from file '%s'".formatted(s), ex);
            }
        }

        registerHandlers(handlers.toArray(new IHandler[0]));
    }

    @Override
    public void registerHandlers(IHandler... handlers) {
        dsRuntime = dsRuntime.registerHandlers(handlers);

        logger.info("Following Handlers have been registered:\n%s"
            .formatted(
                    Arrays.stream(handlers)
                        .map( IHandler::id)
                        .collect(Collectors.joining(",\n    ", "    ", ""))
                    )
        );
    }

    @JsonFilter("PropertyFilter")
    static class PropertyFilterMixIn
    {
    }
}

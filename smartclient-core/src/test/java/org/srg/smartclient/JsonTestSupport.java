package org.srg.smartclient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class JsonTestSupport {

    private JsonTestSupport() {
    }

    // Should be used to provide required base mapper
    public static ObjectMapper defaultMapper = new ObjectMapper();

    private static ObjectMapper tolerantMapper;

    public static ObjectMapper tolerantMapper() {
        if (tolerantMapper == null) {
            tolerantMapper = defaultMapper
                    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
//                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                    .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            final SimpleModule module = new SimpleModule("DSResponse-Serialization", Version.unknownVersion());
            module.addSerializer(RelationSupport.ForeignRelation.class, new JsonTestSupport.ForeignRelationSerializer());
            module.addSerializer(RelationSupport.ImportFromRelation.class, new JsonTestSupport.ImportFromRelationSerializer());
            module.addSerializer(RelationSupport.ForeignKeyRelation.class, new JsonTestSupport.ForeignKeyRelationSerializer());


            tolerantMapper.registerModule(module);

        }
        return tolerantMapper;
    }

    public static String asStrictJSON(Object data) throws IOException {
        data = parseIfString(data);
        return tolerantMapper().writer().writeValueAsString(data);
    }

    protected static Object parseIfString(Object data) throws IOException {
        if (data instanceof String) {
            data = tolerantMapper().readValue((String) data, Object.class);
        }

        return data;
    }

    public static Configuration defaultConfiguration = Configuration.empty().withOptions(Option.IGNORING_ARRAY_ORDER);

    public static void assertJsonEquals(Object expected, Object actual, Option... options) {
        assertEqualsUsingJSONImpl(expected, actual, defaultConfiguration.withOptions(Option.IGNORING_ARRAY_ORDER, options), null);
    }

    public static void assertJsonEquals(Object expected, Object actual) {
        assertEqualsUsingJSONImpl(expected, actual, defaultConfiguration, null);
    }

    public static void assertJsonEquals_WithOrder(Object expected, Object actual) {
        assertEqualsUsingJSONImpl(expected, actual, Configuration.empty(), null);
    }

    public static void assertJsonEquals(Object expected, Object actual, BeanSerializerModifier beanSerializerModifier) {
        assertEqualsUsingJSONImpl(expected, actual, defaultConfiguration, beanSerializerModifier);
    }

    protected static void assertEqualsUsingJSONImpl(Object expected, Object actual, Configuration configuration, BeanSerializerModifier beanSerializerModifier /*, String... ignoreProperties*/) {

        final ObjectMapper om = tolerantMapper().copy();
//                .setSerializationInclusion(JsonInclude.Include.ALWAYS);

        if (beanSerializerModifier != null) {
            om.registerModule(new SimpleModule() {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.addBeanSerializerModifier(beanSerializerModifier);
                }
            });
        }

//        /*
//         * If expected is provided as a String -- THE ALL expected fields MUST BE COUNTED(included)
//         */
//        final Set<String> includeFields;
//
//        if (expected instanceof String) {
//            try {
//                final Object oExpected = parseIfString(expected);
//                includeFields = DeepFieldFilter.buildInclusionSetByExample(oExpected);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            includeFields = Set.of();
//        }
//
//        /*
//         * Deep field filtering has been taken from  https://gist.github.com/sdorra/a59a7cb95c49afc84ed26211350c1321
//         */
//        final DeepFieldFilter f = new DeepFieldFilter(includeFields);
//        final SimpleFilterProvider sfp = new SimpleFilterProvider();
//        sfp.addFilter("customPropertyFilter", f);
//
        String strExpected = null;
        String strActual = null;
        try {
//
//            // It is expected taht DeepFieldFilter will do all teh required magic
//            om.setSerializationInclusion(JsonInclude.Include.ALWAYS);
//
            strExpected = om
//                    .addMixIn(Object.class, CustomPropertyFilterMixIn.class)
//                    .setFilterProvider(sfp)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(parseIfString(expected));


            if ( !(actual instanceof String)) {
                /*
                 * It is required to ensure that the same Serializers/Deserializers will be used for expect
                 * and actual values.
                 *
                 * Otherwise, especially in case of involving custom serializer/deserializer for either
                 * actual or expected, results can be different from each other. Typically,
                 * custom serializers/deserializers does not take care about Inclusion policy
                 * (JsonInclude.Include.NON_NULL) and that can lead to a different results:
                 *    the same business value will be
                 *        a) parsed from string (actually to Map) and parsing results will be serialized
                 *           with a default serializer
                 *        b)  serialized with a custom serializer taht does noit take Inclusion into account.
                 */
                if (actual instanceof Collection) {
                    actual = om.convertValue(actual, new TypeReference<List<Map<String, Object>>>(){});
                } else {
                    actual = om.convertValue(actual, new TypeReference<Map<String, Object>>(){});
                }
            }

            strActual = om
//                    .addMixIn(Object.class, CustomPropertyFilterMixIn.class)
//                    .setFilterProvider(sfp)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(parseIfString(actual));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

         JsonAssert.assertJsonEquals(strExpected, strActual, configuration);
//        assertThatJson(strActual)
//                .withConfiguration(с -> configuration)
//                .isEqualTo(strExpected);
    }

    public static <T> T fromJSON(TypeReference<T> typeReference, String json, Object... args) {
        try {
            return tolerantMapper().readValue(String.format(json, args), typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJSON(Class<T> clazz, String json, Object... args) {
        try {
            return tolerantMapper().readValue(String.format(json, args), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ForeignRelationSerializer extends JsonSerializer<RelationSupport.ForeignRelation> {
        @Override
        public void serialize(RelationSupport.ForeignRelation value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeStartObject();

            if (value.dataSource() != null) {
                gen.writeStringField("dataSource", value.dataSource().getId());
            } else {
                gen.writeStringField("dataSourceId", value.dataSourceId());
            }

            if (value.field() != null) {
                gen.writeObjectField("field", value.field().getName());
            } else {
                gen.writeStringField("fieldName", value.fieldName());
            }

            gen.writeObjectField("relatedTableAlias", value.getRelatedTableAlias());
            gen.writeStringField("sqlFieldAlias", value.getSqlFieldAlias());

            gen.writeEndObject();
        }
    }

    private static class ImportFromRelationSerializer extends JsonSerializer<RelationSupport.ImportFromRelation> {

        @Override
        public void serialize(RelationSupport.ImportFromRelation value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            gen.writeStringField("dataSource", value.dataSource().getId());
            gen.writeObjectField("sourceField", value.sourceField().getName());
//            gen.writeObjectField("relatedTableAlias", value.relatedTableAlias());

            gen.writeObjectField("foreignKeyRelations", value.foreignKeyRelations());
            gen.writeObjectField("foreignDisplay", value.foreignDisplay().getName());

            gen.writeEndObject();
        }
    }

    private static class ForeignKeyRelationSerializer extends JsonSerializer<RelationSupport.ForeignKeyRelation> {
        @Override
        public void serialize(RelationSupport.ForeignKeyRelation value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            if (value.dataSource() != null) {
                gen.writeStringField("dataSource", value.dataSource().getId());
            } else {
                gen.writeNullField("dataSource");
            }


            gen.writeObjectField("sourceField", value.sourceField().getName());
            gen.writeBooleanField("isInverse", value.isInverse());

            gen.writeObjectField("foreign", value.foreign());

            gen.writeEndObject();
        }
    }
//
//    @JsonFilter("customPropertyFilter")
//    public static class CustomPropertyFilterMixIn {
//
//    }
//
//
//    private static class DeepFieldFilter extends SimpleBeanPropertyFilter {
//        private final Set<String> includes;
//
//        private DeepFieldFilter(Set<String> includes) {
//            this.includes = includes;
//        }
//
//        private String createPath(PropertyWriter writer, JsonGenerator jgen) {
//            final StringBuilder path = new StringBuilder();
//            path.append(writer.getName());
//
//            JsonStreamContext sc = jgen.getOutputContext();
//            if (sc != null) {
//                sc = sc.getParent();
//            }
//
//            while (sc != null) {
//                if (sc.getCurrentName() != null) {
//                    if (path.length() > 0) {
//                        path.insert(0, ".");
//                    }
//                    path.insert(0, sc.getCurrentName());
//                }
//                sc = sc.getParent();
//            }
//            return path.toString();
//        }
//
//        @Override
//        public void serializeAsField(Object pojo, JsonGenerator gen, SerializerProvider provider, PropertyWriter writer)
//                throws Exception {
//            String path = createPath(writer, gen);
//            if (includes.contains(path)) {
//                writer.serializeAsField(pojo, gen, provider);
//            } else {
//                writer.serializeAsOmittedField(pojo, gen, provider);
//            }
//        }
//
//        private static Set<String> buildInclusionSetByExampleImpl(String parentPath, Object example) {
//            final HashSet<String> r = new HashSet<>();
//
//            if (!parentPath.isBlank()) {
//                r.add(parentPath);
//            }
//
//            if (example instanceof Collection c) {
//                for (Object o: c) {
//                    final Set<String> childFields = buildInclusionSetByExampleImpl(parentPath, o);
//                    r.addAll(childFields);
//                }
//            } else if (example instanceof Map m) {
//                for (Map.Entry<String, Object> e: ((Map<String,Object>)m).entrySet()) {
//                    final String path = parentPath.isBlank() ? e.getKey() : parentPath + "." + e.getKey();
//                    final Set<String> childFields = buildInclusionSetByExampleImpl(path, e.getValue());
//                    r.addAll(childFields);
//                }
//            }
//
//            return r;
//        }
//
//        public static Set<String> buildInclusionSetByExample(Object example) {
//            return buildInclusionSetByExampleImpl("", example);
//        }
//    }
}

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

    protected static void assertEqualsUsingJSONImpl(Object expected, Object actual, Configuration configuration, BeanSerializerModifier beanSerializerModifier) {

        final ObjectMapper om = tolerantMapper().copy();

        if (beanSerializerModifier != null) {
            om.registerModule(new SimpleModule() {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.addBeanSerializerModifier(beanSerializerModifier);
                }
            });
        }

        String strExpected = null;
        String strActual = null;
        try {
            strExpected = om
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(parseIfString(expected));

            strActual = om
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(parseIfString(actual));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JsonAssert.assertJsonEquals(strExpected, strActual, configuration);
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
                gen.writeObjectField("field", value.field());
            } else {
                gen.writeStringField("fieldName", value.fieldName());
            }

            gen.writeStringField("sqlFieldAlias", value.getSqlFieldAlias());

            gen.writeEndObject();
        }
    }

    private static class ImportFromRelationSerializer extends JsonSerializer<RelationSupport.ImportFromRelation> {

        @Override
        public void serialize(RelationSupport.ImportFromRelation value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            gen.writeStringField("dataSource", value.dataSource().getId());
            gen.writeObjectField("field", value.sourceField());

            gen.writeObjectField("foreignKeyRelations", value.foreignKeyRelations());

            gen.writeObjectField("foreignDisplay", value.foreignDisplay());

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


            gen.writeObjectField("sourceField", value.sourceField());
            gen.writeBooleanField("isInverse", value.isInverse());

            gen.writeObjectField("foreign", value.foreign());

            gen.writeEndObject();
        }
    }
}

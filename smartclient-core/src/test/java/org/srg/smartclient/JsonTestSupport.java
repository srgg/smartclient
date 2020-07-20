package org.srg.smartclient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

    public static void assertJsonEquals(Object expected, Object actual, Option...options) {
        assertEqualsUsingJSONImpl(expected, actual, Configuration.empty().withOptions(Option.IGNORING_ARRAY_ORDER, options));
    }

    public static void assertJsonEquals(Object expected, Object actual) {
        assertEqualsUsingJSONImpl(expected, actual, Configuration.empty().withOptions(Option.IGNORING_ARRAY_ORDER));
    }

    public static void assertJsonEquals_WithOrder(Object expected, Object actual) {
        assertEqualsUsingJSONImpl(expected, actual, Configuration.empty());
    }

    protected static void assertEqualsUsingJSONImpl(Object expected, Object actual, Configuration configuration) {
        String strExpected = null;
        String strActual = null;
        try {
            strExpected = tolerantMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(parseIfString(expected));

            strActual = tolerantMapper()
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
}

package org.srg.smartclient;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import netscape.javascript.JSObject;
import org.apache.commons.collections.map.LinkedMap;
import org.srg.smartclient.isomorphic.*;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class JsonSerde {

    public static <T extends IDSRequest> T deserializeRequest(String data) throws IOException {
        final ObjectMapper mapper = createMapper();

        try (JsonParser p = mapper.createParser(data)) {
            if (p.nextToken() == JsonToken.START_OBJECT
                && "transaction".equals(p.nextFieldName())
                    && p.nextToken() == JsonToken.START_OBJECT
            ) {
                return (T)mapper.readValue(p, DSTransaction.class);
            } else {
                return (T)mapper.readValue(data, DSRequest.class);
            }
        }
    }

    public static void serializeResponse(Writer writer, Collection<DSResponse> response) throws IOException {
        createMapper()
            .writerWithDefaultPrettyPrinter()
        .   writeValue(writer, response);
    }

    private static class DSRequestDeserializer extends JsonDeserializer<IDSRequestData> {
        @Override
        public IDSRequestData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            final ObjectNode node = p.readValueAsTree();

            if (node.has("operator")) {
                return  p.getCodec().treeToValue(node, AdvancedCriteria.class);
            } else {
                final  DSRequest.MapData map = p.getCodec().treeToValue(node, DSRequest.MapData.class);
                return map;
            }
        }
    }

    private static class DSResponseSerialize extends  JsonSerializer<DSResponse> {
        @Override
        public void serialize(DSResponse dsResponse, JsonGenerator jg, SerializerProvider serializerProvider) throws IOException {
            jg.writeStartObject();
            jg.writeFieldName("response");
            jg.writeStartObject();
            jg.writeNumberField("status", dsResponse.getStatus());
            jg.writeNumberField("startRow", dsResponse.getStartRow());
            jg.writeNumberField("endRow", dsResponse.getEndRow());
            jg.writeNumberField("totalRows", dsResponse.getTotalRows());

            final DSResponseDataContainer rc = dsResponse.getData();
            switch (dsResponse.getData().getResponseType()) {
                case GENERAL_ERROR:
                    jg.writeStringField("data", rc.getGeneralFailureMessage());
                    break;

                case RAW:
                    jg.writeFieldName("data");
                    serializeRawResponse(rc, jg);
                    break;

            }
            jg.writeEndObject();
            jg.writeEndObject();
        }

        private void serializeRawResponse(DSResponseDataContainer rc, JsonGenerator jg) throws IOException {
            final DSResponseDataContainer.RawDataResponse rr = rc.getRawDataResponse();

            jg.writeStartArray();
            for (Object r[]: rr.getData()) {
                jg.writeStartObject();
                int i=0;
                for (DSField f: rr.getFields()) {
                    jg.writeFieldName(f.getName());

                    final Object value = r[i++];
                    writeValue(jg, f, value);
                }
                jg.writeEndObject();
            }
            jg.writeEndArray();
        }

        private static void writeValue(JsonGenerator jsonGenerator, DSField field, Object value) throws IOException {
            switch (field.getType()) {
                case TEXT:
//                    jsonGenerator.writeString((String) value);
//                    break;
//
                case INTEGER:
//                    jsonGenerator.writeNumber((Long)value);
//                    break;

                case DATETIME:

                case INTENUM:

                    // TODO: write ENUM name instead of  writing ordinal
                case ENUM:
//                    jsonGenerator.writeString(value.toString());
                    jsonGenerator.writeObject(value);
                    break;


                case DATE:
                    final String s = "%tF".formatted(value);
                    jsonGenerator.writeString(s);
                    break;

                case TIME:
                    final String s2 = "%tT".formatted(value);
                    jsonGenerator.writeString(s2);
                    break;

                default:
                    throw new IllegalStateException("Unsupported DSField type '%s'.".formatted(field.getType()));
            }
        }
    }

    public static ObjectMapper createMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule("DSResponse-Serialization", Version.unknownVersion());
        module.addSerializer(DSResponse.class, new DSResponseSerialize() );
        module.addDeserializer(IDSRequestData.class, new DSRequestDeserializer());

        mapper.registerModule(module);
        mapper
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
//                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
//                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
//                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        return mapper;
    }
}

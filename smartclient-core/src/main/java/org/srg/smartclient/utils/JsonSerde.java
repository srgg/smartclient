package org.srg.smartclient.utils;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.srg.smartclient.JpaRelation;
import org.srg.smartclient.isomorphic.*;
import org.srg.smartclient.isomorphic.criteria.AdvancedCriteria;

import javax.persistence.JoinColumn;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.IncompleteAnnotationException;
import java.util.Collection;

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

    /**
     * This specific deserializer is required due to the requirement for the foreignDisplayField
     * mechanism that is described here:
     *   - https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/JpaHibernateRelations.html (One-to-Many Relations);
     *   - https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..DataSourceField.foreignDisplayField.
     *
     * Issue solved by this deserializer: In some use cases (subsequent entity fetch) <code>type</code> represents
     * a Data Source Id, whereas, usually, -- it represents a concrete field type,
     * one of the values from the enumeration {@link DSField.FieldType}. Therefore in thisrare case Jackson fails
     * to map <code>type</code>, since there are no correspondent enumeration value.
     *
     * https://stackoverflow.com/questions/18313323/how-do-i-call-the-default-deserializer-from-a-custom-deserializer-in-jackson
     */
    private static class DSFieldDeserializer extends StdDeserializer<DSField> {

        protected DSFieldDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public DSField deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            final ObjectNode node = p.readValueAsTree();

            if (node.has("type")) {
                final TextNode nodeV = (TextNode) node.get("type");
                final String typeV = nodeV.asText(null);

                try {
                    final DSField.FieldType ft = DSField.FieldType.valueOf(typeV.toUpperCase());
                    assert ft != null;
                }  catch (Throwable e) {
                    /**
                     *  Since field type doe not represent particular DSField.FieldType: it must have been
                     *  a Data Source ID, and the right field type for a suuch field is DSField.FieldType.Entity.
                     */
                    node.set("type", new TextNode("entity"));
                }
            }

            final DeserializationConfig config = ctxt.getConfig();
            JavaType type = TypeFactory.defaultInstance().constructType(DSField.class);
            JsonDeserializer<Object> defaultDeserializer = BeanDeserializerFactory.instance.buildBeanDeserializer(ctxt, type, config.introspect(type));

            if (defaultDeserializer instanceof ResolvableDeserializer) {
                ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
            }

            final ObjectCodec oc = p.getCodec();
            JsonParser treeParser = oc.treeAsTokens(node);
            config.initialize(treeParser);

            if (treeParser.getCurrentToken() == null) {
                treeParser.nextToken();
            }

            final DSField dsf = (DSField) defaultDeserializer.deserialize(treeParser, ctxt);
            return dsf;
        }
    }

    private static class RawDataResponseSerializer extends JsonSerializer<DSResponseDataContainer.RawDataResponse> {

        @Override
        public void serialize(DSResponseDataContainer.RawDataResponse rr, JsonGenerator jg, SerializerProvider serializers) throws IOException {
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
            if (/*field.getIncludeFrom() != null && !field.getIncludeFrom().isBlank() &&*/ field.getType() ==  null) {
                jsonGenerator.writeObject(value);
                return;
            }

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

                    // TODO: write ENUM name instead of  writing ordina
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

                case ENTITY:
                    jsonGenerator.writeObject(value);
                    break;

                default:
                    throw new IllegalStateException("Unsupported DSField type '%s'.".formatted(field.getType()));
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
                    jg.writeObject(rc.getRawDataResponse());
                    break;

            }
            jg.writeEndObject();
            jg.writeEndObject();
        }
    }

    private static class JpaRelationSerializer extends JsonSerializer<JpaRelation> {

        @Override
        public void serialize(JpaRelation value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            gen.writeStringField("type", value.type().name());
            gen.writeStringField("sourceType", value.sourceType().getJavaType().getSimpleName());
            gen.writeStringField("sourceAttribute", value.sourceAttribute().getName());

            gen.writeStringField("targetType", value.targetType().getJavaType().getSimpleName());

            if (value.targetAttribute() != null) {
                gen.writeStringField("targetAttribute", value.targetAttribute().getName());
            } else {
                gen.writeNullField("targetAttribute");
            }

            gen.writeBooleanField("isInverse", value.isInverse());

            gen.writeObjectField("joinColumns", value.joinColumns());

            gen.writeEndObject();
        }
    }

    private static class JoinColumnSerializer extends JsonSerializer<JoinColumn> {
        @Override
        public void serialize(JoinColumn value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            String name;
            try {
                name = value.name();
            } catch (IncompleteAnnotationException e) {
                name = "";
            }
            gen.writeStringField("name", name);

            boolean nullable;
            try {
                nullable = value.nullable();
            } catch (IncompleteAnnotationException e){
                nullable = true;
            }
            gen.writeBooleanField("nullable", nullable);

            boolean insertable;
            try {
                insertable = value.insertable();
            } catch (IncompleteAnnotationException e){
                insertable = true;
            }
            gen.writeBooleanField("insertable", insertable);

            boolean unique;
            try {
                unique = value.unique();
            } catch (IncompleteAnnotationException e){
                unique = false;
            }
            gen.writeBooleanField("unique", unique);

            boolean updatable;
            try {
                updatable = value.updatable();
            } catch (IncompleteAnnotationException e){
                updatable = true;
            }
            gen.writeBooleanField("updatable", updatable);

//            gen.writeObjectField("foreignKey", value.foreignKey());
//            gen.writeStringField("columnDefinition", value.columnDefinition());

            String referencedColumnName;
            try {
                referencedColumnName = value.referencedColumnName();
            } catch (IncompleteAnnotationException e){
                referencedColumnName = "";
            }
            gen.writeStringField("referencedColumnName", referencedColumnName);

            String table;
            try {
                table = value.table();
            } catch (IncompleteAnnotationException e){
                table = "";
            }
            gen.writeStringField("table", table);


            gen.writeEndObject();
        }
    }

    public static ObjectMapper createMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule("DSResponse-Serialization", Version.unknownVersion());
        module.addSerializer(DSResponse.class, new DSResponseSerialize() );
        module.addSerializer(DSResponseDataContainer.RawDataResponse.class, new RawDataResponseSerializer());
        module.addSerializer(JpaRelation.class, new JpaRelationSerializer());
        module.addSerializer(JoinColumn.class, new JoinColumnSerializer());

        module.addDeserializer(IDSRequestData.class, new DSRequestDeserializer());
        module.addDeserializer(DSField.class, new DSFieldDeserializer(DSField.class));

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

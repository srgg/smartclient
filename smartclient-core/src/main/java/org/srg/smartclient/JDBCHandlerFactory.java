package org.srg.smartclient;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.annotations.SmartClientField;
import org.srg.smartclient.annotations.SmartClientHandler;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JDBCHandlerFactory {
    private static Logger logger = LoggerFactory.getLogger(JDBCHandlerFactory.class);


    public JDBCHandler createJDBCHandler(JDBCHandler.JDBCPolicy jdbcPolicy, IDSRegistry dsRegistry, DataSource ds) {
        return new AdvancedJDBCHandler( jdbcPolicy, dsRegistry, ds);
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

}

package org.srg.smartclient;

import org.apache.commons.lang3.StringUtils;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.metamodel.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class JpaDSDispatcher extends DSDispatcher {
    private EntityManagerFactory emf;

    public JpaDSDispatcher(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public <T> void registerJPAEntity(Class<T> clazz) {
        final Entity[] annotations =  clazz.getAnnotationsByType(Entity.class);

        if (annotations.length == 0) {
            throw new IllegalStateException("Class '%s' does not marked  by @Entity");
        }

        final DataSource ds = describeEntity(clazz);
        JDBCHandler jdbcDS = createJDBCHandler(ds);
        registerDatasource(jdbcDS);
    }

    protected <T> DataSource describeEntity(Class<T> entityClass) {
        final DataSource datasource = new DataSource();

        final String id = getDsId(entityClass);

        datasource.setId(id);
        datasource.setServerType(DataSource.DSServerType.GENERIC);
        datasource.setBeanClassName(entityClass.getCanonicalName());

        datasource.setTableName(
            sqlTableName(entityClass)
        );

        final Metamodel mm = emf.getMetamodel();
        final EntityType<T> et = mm.entity(entityClass);
        final Set<Attribute<? super T, ?>> attrs = et.getAttributes();

        final List<DSField> fields = new ArrayList<>(attrs.size());

        for (Attribute<? super T, ?> a :attrs) {
            DSField f = describeField(a);
            if (f == null) {
                // TODO: add proper logging
                continue;
            }
            fields.add(f);

            // --  includeFrom
            /**
             *
             * Indicates this field should be fetched from another, related DataSource.
             * The incluedFrom attribute should be of the form "dataSourceId.fieldName", for example:
             *
             *     <field includeFrom="supplyItem.itemName"/>
             *
             * https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..DataSourceField.includeFrom
             */
            if ( (f.getForeignKey() != null && !f.getForeignKey().isBlank())
                    && (f.getForeignDisplayField() != null && !f.getForeignDisplayField().isBlank() )) {

                final EntityType<T> targetEntity;

                if (a instanceof SingularAttribute) {
                    SingularAttribute sa = (SingularAttribute) a;
                    targetEntity = (EntityType<T>) sa.getType();
                } else if (a instanceof PluralAttribute) {
                    PluralAttribute pa = (PluralAttribute) a;
                    targetEntity = (EntityType<T>) pa.getElementType();
                } else  {
                    throw new IllegalStateException("Unsupported Attribute class '%s'".formatted(a.getClass()));
                }

                final Attribute<?,?> targetDisplayFieldAttr =  targetEntity.getAttribute(f.getForeignDisplayField());
                assert targetDisplayFieldAttr != null;

                final Class<?> targetClass = targetEntity.getJavaType();
                final String targetDsId = getDsId(targetClass);
                final DSField targetField = describeField(targetDisplayFieldAttr);

                String displayField = f.getDisplayField();
                if (displayField == null || displayField.isBlank()) {
                    displayField = "%s%s".formatted(
                            targetClass.getSimpleName().toLowerCase(),
                            StringUtils.capitalize(f.getForeignDisplayField())
                    );

                    f.setDisplayField( displayField );
                }

                targetField.setName(displayField);
                targetField.setIncludeFrom("%s.%s"
                    .formatted(targetDsId, f.getForeignDisplayField())
                );

                targetField.setIncludeVia(f.getName());


                targetField.setDbName("%s.%s"
                    .formatted(targetClass.getSimpleName(), f.getForeignDisplayField())
                );

                // by default  includeFrom field is not editable
                targetField.setCanEdit(false);

                // by default  includeFrom field is not visible
                targetField.setHidden(true);

                fields.add(targetField);
            }
        }

        datasource.setFields(fields);

        return datasource;
    }

    protected <T> DSField describeField(Attribute<? super T, ?> attr) {
        final Field field = (Field) attr.getJavaMember();

        // -- Generic
        final DSField f = describeField(field);

        // Convert field name to snake case
        f.setDbName(
                f.getDbName().replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z])([A-Z])", "$1_$2")
        );

        // -- JPA
        final JoinColumn jca = field.getAnnotation(JoinColumn.class);

        if (jca != null
            && !jca.name().isBlank()) {
            f.setDbName(jca.name());
        }


        // --

        final DSField.FieldType preserveType = f.getType();
        if (attr instanceof SingularAttribute) {
            SingularAttribute sa = (SingularAttribute) attr;
            f.setPrimaryKey(sa.isId());
            f.setRequired(!sa.isOptional());

            final Type<T> type = sa.getType();
            final Attribute.PersistentAttributeType pat = attr.getPersistentAttributeType();

            switch (type.getPersistenceType()) {
                case BASIC:
                    f.setType(
                            fieldType(sa.getType())
                    );
                    break;

                case ENTITY:
                    switch (pat){
                        case ONE_TO_ONE:
                            final DSField fff =  getDSIDField(type.getJavaType());
                            f.setForeignKey(
                                "%s.%s"
                                    .formatted(
                                        getDsId(type.getJavaType()),
                                            fff.getName()
                                    )
                            );

                            f.setType(fff.getType());
                            break;

                        default:
                            throw new IllegalStateException("Unsupported PersistentAttributeType '%s'.".formatted(
                                    pat
                            ));
                    }
                   break;

                default:
                    Utils.throw_it("Unsupported Persistence Type %s.", type.getPersistenceType());
                    break;

            }
        } else {
            return null;
//            Utils.throw_it("Unsupported Attribute Type %s.", attr.getClass());
        }

        if (preserveType != null) {
            f.setType(preserveType);
        }

        return f;
    }

    protected static String sqlTableName(Class<?> entityClass) {
        String tableName = entityClass.getSimpleName().toLowerCase();

        // -- check entity name
        final Entity[] entities =  entityClass.getAnnotationsByType(Entity.class);

        if (entities.length != 1){
            throw new IllegalStateException("Class '%s' does not marked  by @Entity");
        }

        final Entity entity = entities[0];
        if (!entity.name().isBlank()){
            tableName = entity.name();
        }

        // -- check @Table
        final Table[] tables = entityClass.getAnnotationsByType(Table.class);
        if (tables.length == 1
                && !tables[0].name().isBlank()) {
            final Table table = tables[0];
            tableName = table.name();
        }

        return tableName;
    }

    protected <X> DSField.FieldType fieldType(Type<X> type) {
        final Class<X> clazz = type.getJavaType();

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
                || type.equals(boolean.class) ){
            return DSField.FieldType.BOOLEAN;
        }

        throw new RuntimeException(String.format("Smart Client -- Unmapped field type %s.", clazz.getName()));
    }

    protected DSField getDSIDField(Class<?> clazz) {
        final DataSource ds = describeEntity(clazz);

        final List<DSField> idFields = ds.getFields()
            .stream()
            .filter( dsField -> dsField.isPrimaryKey())
                .collect(Collectors.toList());

        if (idFields.size() != 1) {
            throw new IllegalStateException("Unexpected situation");
        }

        return idFields.get(0);
    }


}

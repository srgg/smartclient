package org.srg.smartclient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import javax.persistence.*;
import javax.persistence.metamodel.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class JpaDSDispatcher extends DSDispatcher {
    private static Logger logger = LoggerFactory.getLogger(JpaDSDispatcher.class);
    private EntityManagerFactory emf;

    public JpaDSDispatcher(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public <T> void registerJPAEntity(Class<T> entityClass) {
        logger.trace("Building DataSource definition for entity '%s'..."
                .formatted(
                        entityClass.getCanonicalName()
                )
        );

        final Entity[] annotations =  entityClass.getAnnotationsByType(Entity.class);

        if (annotations.length == 0) {
            throw new IllegalStateException("Class '%s' does not marked  by @Entity");
        }

        final DataSource ds = describeEntity(entityClass);
        JDBCHandler jdbcDS = createJDBCHandler(ds);
        registerDatasource(jdbcDS);

        if (logger.isDebugEnabled()) {

            final StringBuilder sbld = new StringBuilder();
            try {
                generateDSJavaScript(sbld, "<URL-PLACE-HOLDER>", ds.getId());
            } catch (Exception e) {
                sbld.append("Can't serialize Data Source definition, unexpected error occurred: %s"
                        .formatted(
                                e.getMessage()
                        )
                );
            }

            logger.debug("DataSource definition for entity '%s' has been built:\n%s"
                    .formatted(
                            entityClass.getCanonicalName(),
                            sbld
                    )
            );
        }
    }

    protected <T> DataSource describeEntity(Class<T> entityClass) {
        final DataSource ds  = super.describeEntity(entityClass);

        ds.setTableName(
            sqlTableName(entityClass)
        );

        final Metamodel mm = emf.getMetamodel();
        final EntityType<T> et = mm.entity(entityClass);
        final Set<Attribute<? super T, ?>> attrs = et.getAttributes();

        final List<DSField> fields = new ArrayList<>(attrs.size());
        final Map<String, Attribute<? super T, ?>> skippedAttrs = new HashMap<>(attrs.size());

        for (Attribute<? super T, ?> a :attrs) {
            DSField f = describeField(ds.getId(), a);
            if (f == null) {
                // TODO: add proper logging
                skippedAttrs.put(a.getName(), a);
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

                final Class<?> targetClass = targetEntity.getJavaType();
                final DSField targetField;

                // -- handle case when ForeignDisplayField is @Transient
                final Field javaTargetField = FieldUtils.getField(targetClass, f.getForeignDisplayField(), true);
                if (javaTargetField == null) {
                    throw new IllegalStateException(
                            "Datasource '%s', field '%s': Nothing known about foreignDisplayField '%s' in the target java class '%s'"
                            .formatted( ds.getId(),
                                    f.getName(),
                                    f.getForeignDisplayField(),
                                    targetClass.getCanonicalName()
                            )
                    );
                }

                if (javaTargetField.isAnnotationPresent(Transient.class) ) {
                    targetField = describeField(ds.getId(), javaTargetField);
                } else {
                    final Attribute<?,?> targetDisplayFieldAttr =  targetEntity.getAttribute(f.getForeignDisplayField());
                    assert targetDisplayFieldAttr != null;

                    targetField = describeField(ds.getId(), targetDisplayFieldAttr);
                }

                // --
                final String targetDsId = getDsId(targetClass);

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

        // -- populate data source with JPA @Transient Fields
        final List<DSField> oldFields = ds.getFields();
        for (DSField dsf :oldFields) {
            if (!fields.contains(dsf)
                && !skippedAttrs.containsKey(dsf.getName())) {
                // since it is @Transient field and JPA does not have any clue about it's processing, -
                // the field will be exclueded from SQL generation
                fields.add(dsf);
            }
        }
        ds.setFields(fields);
        return ds;
    }

    protected <T> DSField describeField(String dsId, Attribute<? super T, ?> attr) {
        final Field field = (Field) attr.getJavaMember();

        // -- Generic
        final DSField f = describeField(dsId, field);


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
        String tableName = entityClass.getSimpleName();

        // Convert table name to snake case
        tableName = tableName
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();

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
        return fieldType(clazz);
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

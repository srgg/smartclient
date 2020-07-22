package org.srg.smartclient.jpa;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srg.smartclient.*;
import org.srg.smartclient.annotations.SmartClientField;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;
import org.srg.smartclient.isomorphic.OperationBinding;

import javax.persistence.*;
import javax.persistence.metamodel.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class JPAAwareHandlerFactory extends JDBCHandlerFactory {
    private static Logger logger = LoggerFactory.getLogger(JPAAwareHandlerFactory.class);

    public JDBCHandler createHandler(EntityManagerFactory emf, JDBCHandler.JDBCPolicy jdbcPolicy,
                                     IDSRegistry dsRegistry, Class<?> entityClass) {

        logger.trace("Building DataSource definition for JPA entity '%s'..."
                .formatted(
                        entityClass.getCanonicalName()
                )
        );

        final Metamodel mm = emf.getMetamodel();
        final DataSource ds = this.describeEntity(mm, entityClass);

        if (logger.isDebugEnabled()) {

            String dsDefinition;
            try {
                dsDefinition = DSDeclarationBuilder.build(dsRegistry, "<URL-PLACE-HOLDER>", ds, true);
            } catch (Exception e) {
                dsDefinition = "Can't serialize Data Source definition, unexpected error occurred: %s"
                        .formatted(
                                e.getMessage()
                        );

                logger.warn(dsDefinition, e);
            }

            logger.debug("DataSource definition for entity '%s' has been built:\n%s"
                    .formatted(
                            entityClass.getCanonicalName(),
                            dsDefinition
                    )
            );
        }

        logger.trace("Creating JDBCHandler Handler for JPA entity '%s'..."
                .formatted(
                        entityClass.getCanonicalName()
                )
        );

        JDBCHandler handler = createJDBCHandler(jdbcPolicy, dsRegistry, ds);
        return handler;
    }

    protected <T> DataSource describeEntity(Metamodel mm, Class<T> entityClass) {

        final Entity[] annotations =  entityClass.getAnnotationsByType(Entity.class);

        if (annotations.length == 0) {
            throw new IllegalStateException("Class '%s' does not marked  by @Entity");
        }

        final DataSource ds  = super.describeEntity(entityClass);

        ds.setTableName(
                sqlTableName(entityClass)
        );

        final EntityType<T> et = mm.entity(entityClass);
        final Set<Attribute<? super T, ?>> attrs = et.getAttributes();

        final List<DSField> fields = new ArrayList<>(attrs.size());
        final Map<String, Attribute<? super T, ?>> skippedAttrs = new HashMap<>(attrs.size());

        for (Attribute<? super T, ?> a :attrs) {
            DSField f = describeField(mm, ds.getId(), et,  a);
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
             * The includeFrom attribute should be of the form "dataSourceId.fieldName", for example:
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
                final DSField targetField = new DSField();

                // -- detect target field type
                {
                    final DSField tf;

                    /**
                     *  Handles a case when ForeignDisplayField is @Transient, that can happen if it is a calculated field:
                     *  <pre>
                     *     @SmartClientField(hidden = true, customSelectExpression = "CONCAT(employee.last_Name, ' ', employee.first_Name)")
                     *     @Transient
                     *     private String fullName;
                     *  </pre>
                     *
                     */
                    final Field javaTargetField = FieldUtils.getField(targetClass, f.getForeignDisplayField(), true);
                    if (javaTargetField == null) {
                        throw new IllegalStateException(
                                "Datasource '%s', field '%s': Nothing known about foreignDisplayField '%s' in the target java class '%s'"
                                        .formatted(ds.getId(),
                                                f.getName(),
                                                f.getForeignDisplayField(),
                                                targetClass.getCanonicalName()
                                        )
                        );
                    }

                    if (javaTargetField.isAnnotationPresent(Transient.class)) {
                        tf = describeField(ds.getId(), javaTargetField);
                    } else {
                        final Attribute<? super T, ?> targetDisplayFieldAttr = targetEntity.getAttribute(f.getForeignDisplayField());
                        assert targetDisplayFieldAttr != null;

                        tf = describeField(mm, ds.getId(), targetEntity, targetDisplayFieldAttr);
                    }

                    targetField.setType(tf.getType());
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

    protected <T> JpaDSField describeField( Metamodel mm, String dsId, EntityType<? super T> entityType, Attribute<? super T, ?> attr) {
        final Field field = (Field) attr.getJavaMember();

        // -- Generic
        final JpaDSField f = new JpaDSField();
        describeField(f, dsId, field);

        // -- JPA
        final boolean attributeBelongsToCompositeId = !entityType.hasSingleIdAttribute()
                && entityType.getIdClassAttributes().contains(attr);

        if (attributeBelongsToCompositeId) {
            /**
             * Since attribute is a part of the composite Id/PK, it is possible that @SmartClientField annotation
             * was put on the Entity field, rather than on the IdClass field, and,  in this case,
             * generic method JDBCHandlerFactory#describeField(dsId, field)  will not find the annotation and
             * do not apply it.
             *
             * Therefore, it is required to check the entity field for @SmartClientField and apply it if it exists
             */
            SmartClientField sfa = field.getAnnotation(SmartClientField.class);

            if (sfa == null) {
                /**
                 * Ok, it is clear that IdClass field is not annotated with @SmartClientField and it is HIGHLY possible that
                 * annotation was put at the correspondent entity field.
                 *
                 * I can't find any suitable JPA MetaModel API that returns attributes for the Entity,
                 * all of them returns attributes for the IdClass. The only way to get correspondent entity fields
                 * is to use Java Reflection API.
                 */
                final Class entityJavaType = entityType.getJavaType();

                try {
                    final Field entityField = entityJavaType.getDeclaredField(attr.getName());
                    sfa = entityField.getAnnotation(SmartClientField.class);
                } catch (NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }

                if (sfa != null) {
                    applySmartClientFieldAnnotation(sfa, f);
                }
            }
        }

        final JpaRelation jpaRelation = JPARelationSupport.describeRelation(mm, attr);


        if (jpaRelation != null
                && jpaRelation.joinColumns().size() == 1) {
            // Workaround, this is how it was done before and, probably, thic will be changed in the future

            final JoinColumn joinColumnAnnotation = jpaRelation.joinColumns().get(0);
            if (!joinColumnAnnotation.name().isBlank()) {
                f.setDbName(joinColumnAnnotation.name());
            }
        }

        // --

        final DSField.FieldType preserveType = f.getType();
        if (attr instanceof SingularAttribute sa) {
            f.setPrimaryKey(sa.isId());

            if (f.isHidden() == null ) {
                f.setHidden( f.isPrimaryKey());
            }

            if (f.isPrimaryKey()) {
                f.setCanEdit(false);
                f.setRequired(true);
            } else {
                f.setRequired(!sa.isOptional());
            }

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
                        case MANY_TO_ONE:
                        case ONE_TO_ONE:
                            assert jpaRelation != null;
                            final Set<DSField> dsIdFields = getDSIDField(mm, type.getJavaType());

                            final DSField fff =  dsIdFields.iterator().next();

                            // -- foreign key
                            final String foreignDataSourceId = getDsId(type.getJavaType());
                            final String foreignFieldName;
                            if (!jpaRelation.joinColumns().isEmpty()){
                                if (jpaRelation.joinColumns().size() >1) {
                                    throw new IllegalStateException("Should be implemented soon");
                                } else {
                                    foreignFieldName = fff.getName();
                                }
                            } else {
                                /**
                                 * As soon as entity does not declare any mapping for the attribute,
                                 * by default
                                 */
                                foreignFieldName = jpaRelation.mappedByFieldName();
                            }

                            if ( foreignFieldName == null
                                    || foreignFieldName.isBlank()) {
                                throw new IllegalStateException();
                            }

                            f.setForeignKey(
                                "%s.%s"
                                    .formatted(
                                            foreignDataSourceId,
                                            foreignFieldName
                                    )
                            );

                            // -- field type

                            if ((f.getForeignDisplayField() == null
                                    || f.getForeignDisplayField().isBlank()) && jpaRelation.joinColumns().isEmpty() ) {

                                /**
                                 * As soon as this entity does not declare any mapping for the attribute,
                                 * by default, the entire foreign entity  will be fetched
                                 */
                                f.setType(DSField.FieldType.ENTITY);
                            } else {
                                f.setType(fff.getType());
                            }
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
        } else if (attr instanceof PluralAttribute pa){
            final Type<T> type = pa.getElementType();
            final Attribute.PersistentAttributeType pat = attr.getPersistentAttributeType();

            switch (type.getPersistenceType()) {
                case ENTITY:

                    // Fields with Entity type should be hidden by default
                    if (f.isHidden() == null) {
                        f.setHidden(true);
                    }

                    f.setMultiple(true);

                    // -- Determine a foreign relation
                    final Class<?> javaType = type.getJavaType();
                    final Set<DSField> dsIdFields =  getDSIDField(mm, javaType);

                    DSField fkField = null;
                    if (dsIdFields.size() == 1) {
                        fkField = dsIdFields.iterator().next();

                    } else if (jpaRelation.mappedByFieldName() != null
                            && !jpaRelation.mappedByFieldName().isBlank()) {

                        for (DSField dsf: dsIdFields) {
                            if (dsf.getName().equals(jpaRelation.mappedByFieldName())) {
                                fkField = dsf;
                                break;
                            }
                        }
                    }

                    if (fkField == null) {
                        throw new IllegalStateException(
                                "Datasource '%s', field '%s': Can't determine a foreignKey field  for '%s.%s'"
                                        .formatted( dsId,
                                                f.getName(),
                                                f.getForeignDisplayField(),
                                                attr.getDeclaringType(),
                                                attr.getName()
                                        )
                        );
                    }

                    final String foreignDsId = getDsId(javaType);

//                    SmartClientRelationSupport.describeForeignKey(dsId -> get);
//
//                    final SmartClientRelationSupport.ForeignKeyRelation fkRelation = new SmartClientRelationSupport.ForeignKeyRelation(
//                            foreignDsId,
//                            null,
//                            fkField.getName(),
//                            fkField
//                    );

                    final SmartClientRelationSupport.ForeignRelation fkRelation = new SmartClientRelationSupport.ForeignRelation(
                            foreignDsId,
                            null,
                            fkField.getName(),
                            fkField
                    );

                    // --

                    switch (pat) {
                        case MANY_TO_MANY:
                            /**
                             * Many-To-Many Relations
                             *
                             * An example of Many-To-Many relation is that Students have multiple Courses and each Course has multiple Students. In Java each Student bean has a list of Courses and each Course bean has a list of Students. In database tables are linked using additional table holding references to both students and courses.
                             * To set up Many-To-Many relation between data sources you need to set up One-To-Many relation on both sides.
                             *
                             * For example students DataSourceField for CourseDS data source:
                             *
                             *       <field name="students" type="integer" foreignKey="StudentDS.id" multiple="true" />
                             *
                             * and courses DataSourceField for StudentDS data source:
                             *       <field name="courses" type="integer" foreignKey="CourseDS.id" multiple="true" />
                             *
                             * Note that type attribute can be safely omitted here.
                             * Note that alternative type declaration to be ID of related data source (as in regular One-To-Many relation case) would work as expected, but is not recommended to use, cause it would result in getting lots of copies of same data. Smartclient server will prevent infinite loops, but still lots of unnecessary data will be sent to client.
                             *
                             * @see <a href="https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/JpaHibernateRelations.html">JPA & Hibernate Relations</a>
                             * @see <a href="https://forums.smartclient.com/forum/smart-gwt-technical-q-a/19147-many-to-many-relationship-in-sql-datasource">many-to-many relationship in SQL datasource?</a>
                             * @see <a href="https://www.smartclient.com/smartgwtee/showcase/#large_valuemap_sql"> example shows the simple use of custom SQL clauses to provide a DataSource that joins multiple tables</a>
                             */
                            if (jpaRelation.joinTable() == null) {
                                throw new RuntimeException(
                                    "Datasource '%s', field '%s': Can't determine a join table name  for relation '%s'."
                                            .formatted( dsId,
                                                    f.getName(),
                                                    jpaRelation
                                            )
                                );
                            }

                            final String joinTableName;

                            if (jpaRelation.joinTable().name() == null
                                || jpaRelation.joinTable().name().isBlank()) {
//
//
//                                joinTableName = "%s_%s"
//                                        .formatted(
//                                                dsId,
//                                                fkRelation.dataSource().getTableName()
//                                        );
//
//                                logger.debug(
//                                        "Data source '%s': join table name is not provided for a relation %s, auto generated name '%s' will be used"
//                                            .formatted(dsId,
//                                                    jpaRelation,
//                                                    joinTableName
//                                                )
//                                );

                                throw new IllegalStateException("Not implemented yet");
                            } else {
                                joinTableName = jpaRelation.joinTable().name();
                            }

                            f.setTableName(joinTableName);


                            final OperationBinding b = new OperationBinding();

                            b.setTableClause("""
                                        ${defaultTableClause},
                                        %s 
                                    """
                                    .formatted(
                                        joinTableName
                                    )
                            );

                            b.setAnsiJoinClause("""
                                ${defaultAnsiJoinClause} \n
                             """);

//                            break;

                        case ONE_TO_MANY:
                            f.setForeignKey(
                                    "%s.%s"
                                            .formatted(
                                                    fkRelation.dataSourceId(),
                                                    fkRelation.fieldName()
                                            )
                            );

                            f.setType(DSField.FieldType.ENTITY);
                            break;

                        default:
                            return null;
//                            throw new IllegalStateException("Unsupported PersistentAttributeType '%s'.".formatted(
//                                    pat
//                            ));
                    }
                    break;

                default:
                    Utils.throw_it("Unsupported Persistence Type %s.", type.getPersistenceType());
            }
        } else {
            Utils.throw_it("Unsupported Attribute Type %s.", attr.getClass());
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

    protected  <E> Set<DSField> getDSIDField( Metamodel mm, Class<E> clazz) {
        final EntityType<E> et = mm.entity(clazz);
        final ManagedType<E> mt = mm.managedType(clazz);

        if ( et.hasSingleIdAttribute() ) {
            final Attribute<? super E, ?> idAttribute = et.getId(et.getIdType().getJavaType());

            if (idAttribute == null) {
                // No sure is it possible or not
                throw new IllegalStateException("It seems there is no any @Id field, JPA entity '%s'."
                        .formatted(clazz.getCanonicalName())
                );
            }


            switch (idAttribute.getPersistentAttributeType()) {
                case BASIC:
                    return Collections.singleton(
                            describeField(mm, "<>", et, idAttribute)
                    );

                default:
                    throw new IllegalStateException("Unsupported @Id type '%s', JPA entity '%s'."
                            .formatted(
                                    idAttribute.getPersistentAttributeType(),
                                    clazz.getCanonicalName())
                    );
            }
        } else {
            //Type<?> idType = et.getIdType();
            //assert idType != null;
            final Set<SingularAttribute<? super E, ?>> idAttributes = et.getIdClassAttributes();

            if (idAttributes != null && !idAttributes.isEmpty()) {
                assert idAttributes != null;

                final Set<DSField> ids = idAttributes.stream()
                        .map(sa -> describeField(mm, "<>", et, sa))
                        .collect(Collectors.toSet());

                return ids;
            }

            throw new IllegalStateException("Can't determine id attributes for JPA entity '%s'."
                    .formatted(
                            clazz.getCanonicalName()
                    )
            );
        }
    }
}

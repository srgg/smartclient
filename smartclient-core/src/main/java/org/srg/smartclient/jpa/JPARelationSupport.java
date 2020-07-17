package org.srg.smartclient.jpa;

import javax.persistence.*;
import javax.persistence.metamodel.*;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.*;
import java.util.stream.Collectors;

public class JPARelationSupport {

    // https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=group..jpaHibernateRelations
    public static <T> JpaRelation describeRelation(Metamodel mm, Attribute<? super T, ?> attribute) {
        if (!attribute.isAssociation()) {
            return null;
        }
        final Attribute.PersistentAttributeType pat = attribute.getPersistentAttributeType();
        final JpaRelationType relationType = JpaRelationType.from(pat);

        final EntityType<T> sourceEntityType = (EntityType<T>) attribute.getDeclaringType();
        final Field javaField = (Field) attribute.getJavaMember();

        // -- Mapped by
        final EntityType<T> foreignEntityType;
        final Attribute<? super T,?> mappedByAttribute;
        final Field mappedByField;
        final String mappedByFieldName = determineMappedBy(mm, relationType, javaField);
        if (mappedByFieldName != null && !mappedByFieldName.isBlank()) {
            if (attribute instanceof SingularAttribute sa) {
                foreignEntityType = (EntityType<T>) sa.getType();
            } else if (attribute instanceof PluralAttribute pa) {
                foreignEntityType = (EntityType<T>) pa.getElementType();
            } else  {
                throw new IllegalStateException("Attribute '%s.%s' has unsupported attribute implementation class '%s'."
                        .formatted(
                                attribute.getDeclaringType(),
                                attribute.getName(),
                                attribute.getClass()
                        )
                );
            }

            mappedByAttribute = foreignEntityType.getAttribute( mappedByFieldName );

            final Object o = mappedByAttribute.getJavaMember();
            if (o instanceof Field ) {
                mappedByField = (Field) o;
            } else {
                throw new IllegalStateException("");
            }
        } else {
            foreignEntityType = null;
            mappedByField = null;
            mappedByAttribute = null;
        }

        // -- Join columns
        List<JoinColumn> joinColumns = determineJoinColumns(javaField);

        if (joinColumns.isEmpty()) {
            joinColumns = determineJoinColumns(sourceEntityType);
        }

        List<JoinColumn> mappedByJoinColumns = mappedByField == null ? Collections.EMPTY_LIST : determineJoinColumns(mappedByField);

        if (mappedByJoinColumns.isEmpty()
                && mappedByFieldName != null
                && !mappedByFieldName.isBlank()) {
            mappedByJoinColumns = determineJoinColumns(foreignEntityType);

        }

        if (joinColumns.isEmpty() && mappedByJoinColumns.isEmpty()) {
            throw new IllegalStateException("Cant't build JpaRelation for '%s.%s': join column is not found."
                    .formatted(attribute.getDeclaringType(), attribute.getName()));
        }

        // -- Join Table
        final JoinTable joinTable;

        if (mappedByAttribute == null) {
            joinTable = javaField.getAnnotation(JoinTable.class);
        } else {
            joinTable = mappedByField.getAnnotation(JoinTable.class);
        }

        return new JpaRelation(relationType,sourceEntityType,  foreignEntityType, null, joinColumns, mappedByFieldName, mappedByJoinColumns,
                joinTable == null? null : joinTable);
    }

    private static String determineMappedBy(Metamodel mm, JpaRelationType type, Field field) {
        final String mappedBy = switch (type) {
            case ONE_TO_MANY -> field.getAnnotation(OneToMany.class).mappedBy();
            case ONE_TO_ONE -> field.getAnnotation(OneToOne.class).mappedBy();
            case MANY_TO_MANY -> field.getAnnotation(ManyToMany.class).mappedBy();

            // manyToOne does not support mappedBy
            case MANY_TO_ONE -> null;
            default -> null;
        };

        return mappedBy;
    }

    private static <T> List<JoinColumn> determineJoinColumns(EntityType<T> entityType) {
        final List<JoinColumn> joinColumns;

        if (!entityType.hasSingleIdAttribute()) {
            /**
             * Entity has a composite key, therefore it is also require to look for @JoinColumn annotations
             * at the MappedBy entity, if any
             */
            final Set<SingularAttribute<? super T, ?>> idAttr = entityType.getIdClassAttributes();

            joinColumns = idAttr.stream()
                    .filter(a -> a.isAssociation())
                    .map(a -> {
                        final Member jm = a.getJavaMember();
                        List<JoinColumn> jc = determineJoinColumns((Field) jm);

                        if (jc.isEmpty()) {
                            /**
                             * it seems that  Entity IdClass is not annotated, and it is highly possible that
                             * all the annotations were put at the correspondent entity fields.
                             *
                             * I can't find any JPA MetaModel API that returns attributes for the Entity,
                             * all of them returns attributes for the IdClass. Unfortunately, as a result, -
                             * the only way to get correspondent entity fields is to use a Java Reflection API.
                             */
                            final Class entityJavaType = entityType.getJavaType();

                            Field entityField = null;
                            try {
                                entityField = entityJavaType.getDeclaredField(a.getName());
                            } catch (NoSuchFieldException e) {
                            }

                            if (entityField != null) {
                                assert !jm.equals(entityField);
                                jc = determineJoinColumns(entityField);
                            }
                        }
                        assert jc != null;
                        return jc;
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            joinColumns = Collections.EMPTY_LIST;
        }

        return joinColumns;
    }

    private static final List<JoinColumn> determineJoinColumns(Field field) {
        final List<JoinColumn> joinColumns;

        final JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        if (joinColumnAnnotation != null) {
            joinColumns = Collections.singletonList(joinColumnAnnotation);
        } else {
            final JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
            if (joinColumnsAnnotation != null){
                joinColumns = Arrays.asList(joinColumnsAnnotation.value());
            } else {
                final JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);
                if (joinTableAnnotation != null) {
                    joinColumns = Arrays.asList(joinTableAnnotation.joinColumns());
                } else {
                    joinColumns = Collections.EMPTY_LIST;
                }
            }
        }

        return joinColumns;
    }

}


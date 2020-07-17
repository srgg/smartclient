package org.srg.smartclient.jpa;

import javax.persistence.metamodel.Attribute;

public enum JpaRelationType {
    BASIC,
    ONE_TO_MANY,
    ONE_TO_ONE,
    MANY_TO_ONE,
    MANY_TO_MANY;

    public static JpaRelationType from(Attribute.PersistentAttributeType pat) {
        return switch (pat) {
            case BASIC -> JpaRelationType.BASIC;

            case MANY_TO_ONE -> JpaRelationType.MANY_TO_ONE;
            case ONE_TO_MANY -> JpaRelationType.ONE_TO_MANY;
            case ONE_TO_ONE -> JpaRelationType.ONE_TO_ONE;
            case MANY_TO_MANY ->  JpaRelationType.MANY_TO_MANY;

            default -> throw new IllegalStateException();
        };
    }
}

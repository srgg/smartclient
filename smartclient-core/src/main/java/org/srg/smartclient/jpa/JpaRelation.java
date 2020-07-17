package org.srg.smartclient.jpa;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.metamodel.EntityType;
import java.util.List;

public record JpaRelation(
        JpaRelationType type,

        EntityType<?> sourceEntityType,
        EntityType<?> foreigEntityType,

        //
        String idClassName,

        List<JoinColumn> joinColumns,

        String mappedByFieldName,
        List<JoinColumn> mappedByJoinColumn,

        JoinTable joinTable
){ }

package org.srg.smartclient.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * On Java 15 Hibernate stopped working with Records, it throws
 *  Javax.persistence.PersistenceException: org.hibernate.PropertyAccessException:
 *      Could not set field value [42] value by reflection : [class org.srg.smartclient.jpa.SimpleEntity.id]
 *      setter of org.srg.smartclient.jpa.SimpleEntity.id
 */
@Entity
public class SimpleEntity{
    @Id
    private int id;
    private String name;

    public SimpleEntity() {
    }

    public SimpleEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }
}

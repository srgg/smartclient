package org.srg.smartclient.jpa;

import org.srg.smartclient.annotations.SmartClientField;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Project {
    @Id
    private int id;

    private String name;

    @SmartClientField(foreignDisplayField = "name")
    @ManyToOne
    @JoinColumn(name="client_id", nullable=false)
    private Client client;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Client getClient() {
        return client;
    }
}

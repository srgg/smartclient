package org.srg.smartclient.jpa;

import org.srg.smartclient.annotations.OperationBinding;
import org.srg.smartclient.annotations.SmartClientField;
import org.srg.smartclient.isomorphic.DSRequest;

import javax.persistence.*;
import java.util.Set;

@OperationBinding(
        operationType = DSRequest.OperationType.FETCH,
        whereClause = "($defaultWhereClause) AND"
)
@Entity
public class Project {
    @Id
    private int id;

    private String name;

    @SmartClientField(foreignDisplayField = "name")
    @ManyToOne
    @JoinColumn(name="client_id", nullable=false)
    private Client client;

    @ManyToMany
    @JoinTable(
            name = "project_team",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"))
    private Set<Employee> teamMembers;


    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Client getClient() {
        return client;
    }

    public Set<Employee> getTeamMembers() {
        return teamMembers;
    }
}

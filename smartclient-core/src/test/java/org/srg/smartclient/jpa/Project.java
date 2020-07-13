package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.srg.smartclient.annotations.SmartClientField;

import javax.persistence.*;
import java.util.Set;

//@OperationBinding(
//        operationType = DSRequest.OperationType.FETCH,
//        whereClause = "($defaultWhereClause) AND"
//)
@Entity
public class Project {
    @Id
    private int id;

    private String name;

    @SmartClientField(foreignDisplayField = "name")
    @ManyToOne
    @JoinColumn(name="client_id", nullable=false)
    @JsonManagedReference
    private Client client;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "project_team",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"))
    @JsonIgnore
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

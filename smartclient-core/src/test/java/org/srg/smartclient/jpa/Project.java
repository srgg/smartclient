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

    @SmartClientField(foreignDisplayField = "name")
    @OneToOne
    @JoinColumn(name = "manager_id", foreignKey = @ForeignKey(name = "fkProject_ManagerId"))
    private Employee manager;

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

    public Employee getManager() {
        return manager;
    }

    public Set<Employee> getTeamMembers() {
        return teamMembers;
    }
}

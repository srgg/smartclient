package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Employee {
    @Id
    private int id;
    private String name;

    @OneToMany(mappedBy = "employee", fetch = FetchType.EAGER)
    @JsonManagedReference
    private Set<EmployeeRole> roles;

    @ManyToMany(mappedBy = "teamMembers",  fetch = FetchType.EAGER)
    private Set<Project> projects;

    public Employee() {
    }

    public Employee(int id, String name, Set<EmployeeRole> roles) {
        this.id = id;
        this.name = name;
        this.roles = roles;
    }

    public int getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public Set<EmployeeRole> getRoles() {
        return roles;
    }

    public Set<Project> getProjects() {
        return projects;
    }
}

package org.srg.smartclient.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
public class Employee {
    @Id
    private int id;
    private String firstName;
    private String lastName;

    @OneToMany(mappedBy = "employee")
    private Set<EmployeeRole> roles;

    @OneToMany(mappedBy = "teamMembers")
    private Set<Project> projects;


    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Set<Project> getProjects() {
        return projects;
    }
}

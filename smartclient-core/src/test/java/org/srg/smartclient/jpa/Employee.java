package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
public class Employee  extends GenericEntity<Employee> {
    private String name;

    @OneToMany(mappedBy = "employee", fetch = FetchType.EAGER)
    @JsonManagedReference
    private Set<EmployeeRole> roles;

    @ManyToMany(mappedBy = "teamMembers",  fetch = FetchType.EAGER)
    private Set<Project> projects;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("startDate DESC")
    private List<EmployeeStatus> statuses;

    public String getName() {
        return name;
    }

    public Set<EmployeeRole> getRoles() {
        return roles;
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public List<EmployeeStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<EmployeeStatus> statuses) {
        this.statuses = statuses;
    }
}

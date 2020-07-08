package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Table(name = "employee_role")
@Entity
@IdClass(EmployeeRole.EmployeeRoleId.class)
public class EmployeeRole {
    public static class EmployeeRoleId implements Serializable {
        private int employee;

        private String role;

        // default constructor
        public EmployeeRoleId() {
        }

        public EmployeeRoleId(int employee, String role) {
            this.employee = employee;
            this.role = role;
        }

        // equals() and hashCode()


        public int getEmployeeId() {
            return employee;
        }

        public void setEmployeeId(int employeeId) {
            this.employee = employeeId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeRoleId)) return false;
            EmployeeRoleId that = (EmployeeRoleId) o;
            return employee == that.employee &&
                    getRole().equals(that.getRole());
        }

        @Override
        public int hashCode() {
            return Objects.hash(employee, getRole());
        }
    }

    @Id
    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;

    @Id
    private String role;

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeRole)) return false;
        EmployeeRole that = (EmployeeRole) o;
        return this.employee.getId() == that.employee.getId() && this.role.equals(((EmployeeRole) o).getRole());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmployee().getId(), getRole());
    }
}

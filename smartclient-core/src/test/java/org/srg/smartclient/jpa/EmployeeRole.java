package org.srg.smartclient.jpa;

import org.srg.smartclient.annotations.SmartClientField;
import org.srg.smartclient.isomorphic.DSField;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

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
    private Employee employee;

    @Id
    private String role;

    @SmartClientField(type = DSField.FieldType.DATE)
    private Timestamp createdAt;

    private Timestamp modifiedAt;

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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Timestamp modifiedAt) {
        this.modifiedAt = modifiedAt;
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

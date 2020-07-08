package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.List;

@Entity
public class Client {
        @Id
        private int id;

        private String name;

        @OneToOne(mappedBy = "client", fetch = FetchType.EAGER)
        @JsonManagedReference
        private ClientData data;

        @OneToMany(mappedBy = "client", fetch = FetchType.EAGER)
        @JsonBackReference
        private List<Project> projects;

        public int getId() {
                return id;
        }

        public String getName() {
                return name;
        }

        public ClientData getData() {
                return data;
        }

        public List<Project> getProjects() {
                return projects;
        }
}

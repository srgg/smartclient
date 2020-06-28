package org.srg.smartclient.jpa;

import javax.persistence.*;
import java.util.List;

@Entity
public class Client {
        @Id
        private int id;

        private String name;

        @OneToOne(mappedBy = "client")
        private ClientData data;

        @OneToMany(mappedBy = "client")
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

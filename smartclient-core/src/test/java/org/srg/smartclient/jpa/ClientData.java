package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.srg.smartclient.annotations.SmartClientField;

import javax.persistence.*;

@Table(name = "client_data")
@Entity
public class ClientData {
        @Id
        private int id;

        @SmartClientField(foreignDisplayField = "name")
        @OneToOne
        @JoinColumn(name = "client_id", nullable = false)
        @JsonBackReference
        private Client client;

        private String data;

        public int getId() {
                return id;
        }

        public Client getClient() {
                return client;
        }

        public String getData() {
                return data;
        }
}

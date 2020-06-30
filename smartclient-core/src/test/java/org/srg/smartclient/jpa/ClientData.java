package org.srg.smartclient.jpa;

import org.srg.smartclient.annotations.SmartClientField;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity
public class ClientData {
        @Id
        private int id;

        @SmartClientField(foreignDisplayField = "name")
        @OneToOne
        @JoinColumn(name = "client_id", nullable = false)
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

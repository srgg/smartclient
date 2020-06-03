package org.srg.smartclient.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public record SimpleEntity(@Id int id, String name) { }

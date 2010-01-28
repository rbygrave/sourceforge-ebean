package com.avaje.tests.model.basic;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.annotation.Encrypted;

@Entity
@Table(name="e_basicenc")
public class EBasicEncrypt {
    
    @Id
    Integer id;
    
    String name;
    
    //@Lob
    @Encrypted(dbLength=80)
    String description;
    
    //@Version
    Timestamp lastUpdate;
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Timestamp lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

}

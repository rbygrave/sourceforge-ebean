package com.avaje.tests.model.basic;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import com.avaje.ebean.annotation.Encrypted;

@Entity
@Table(name="e_basicenc_bin")
public class EBasicEncryptBinary {
    
    @Id
    Integer id;
    
    String name;
    
    String description;
    
    @Encrypted
    @Lob
    byte[] data;
    
    @Encrypted
    Timestamp someTime;
    
    @Version
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

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Timestamp getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Timestamp lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Timestamp getSomeTime() {
        return someTime;
    }

    public void setSomeTime(Timestamp someTime) {
        this.someTime = someTime;
    }
}

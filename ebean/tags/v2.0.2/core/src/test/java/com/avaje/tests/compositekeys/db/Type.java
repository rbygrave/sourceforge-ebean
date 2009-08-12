package com.avaje.tests.compositekeys.db;

import javax.persistence.*;
import java.util.List;

@Entity
public class Type
{
    @Id
    private TypeKey key;

    private String description;

    @Version
    private Long version;

    @OneToMany
    @JoinColumns({
        @JoinColumn(name = "customer", referencedColumnName = "customer", insertable = false, updatable = false),
        @JoinColumn(name = "type", referencedColumnName = "type", insertable = false, updatable = false)
    })
    private List<Item> items;
    
    public TypeKey getKey() {
        return key;
    }

    public void setKey(TypeKey key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<Item> getItems() {
        return items;
    }
}
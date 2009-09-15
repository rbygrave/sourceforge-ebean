package com.avaje.tests.compositekeys.db;

import javax.persistence.*;

@Entity
public class Item
{
    @Id
    private ItemKey key;

    private String description;

    private int type;

    private int region;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "customer", referencedColumnName = "customer", insertable = false, updatable = false),
        @JoinColumn(name = "type", referencedColumnName = "type", insertable = false, updatable = false)
    })
    private Type eType;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "customer", referencedColumnName = "customer", insertable = false, updatable = false),
        @JoinColumn(name = "region", referencedColumnName = "type", insertable = false, updatable = false)
    })
    private Region eRegion;          

    public ItemKey getKey() {
        return key;
    }

    public void setKey(ItemKey key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public Long getVersion() {
        return version;
    }

    public Type getEType() {
        return eType;
    }

    public Region getERegion() {
        return eRegion;
    }

    public void setVersion(Long version)
    {
        this.version = version;
    }

    public void setEType(Type eType)
    {
        this.eType = eType;
    }

    public void setERegion(Region eRegion)
    {
        this.eRegion = eRegion;
    }
}

package com.avaje.tests.model.basic;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class EVanillaCollection {

    @Id
    Integer id;
    
    @OneToMany(cascade=CascadeType.PERSIST)
    List<EVanillaCollectionDetail> details;
    
    public EVanillaCollection() {
        details = new ArrayList<EVanillaCollectionDetail>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<EVanillaCollectionDetail> getDetails() {
        return details;
    }

    public void setDetails(List<EVanillaCollectionDetail> details) {
        this.details = details;
    }
    
}

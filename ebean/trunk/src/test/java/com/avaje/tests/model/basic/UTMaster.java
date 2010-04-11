package com.avaje.tests.model.basic;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name="ut_master")
public class UTMaster {

    @Id
    Integer id;
    
    String name;
    
    @Version
    Integer version;
    
    @OneToMany(cascade=CascadeType.ALL)
    List<UTDetail> details;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<UTDetail> getDetails() {
        return details;
    }

    public void setDetails(List<UTDetail> details) {
        this.details = details;
    }
    
    public void addDetail(UTDetail detail) {
        if (details == null){
            details = new ArrayList<UTDetail>();
        }
        details.add(detail);
    }
}

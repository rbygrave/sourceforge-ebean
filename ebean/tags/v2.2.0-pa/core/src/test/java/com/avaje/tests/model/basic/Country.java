package com.avaje.tests.model.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.CacheTuning;
import com.avaje.ebean.validation.Length;

/**
 * Country entity bean.
 */
@CacheStrategy(readOnly=true,warmingQuery="order by name")
@CacheTuning(maxSize=500)
@Entity
@Table(name="o_country")
public class Country {

    @Id
    @Length(max=2)
    String code;

    @Length(max=60)
    String name;
    
    /**
     * Return code.
     */    
    public String getCode() {
  	    return code;
    }

    /**
     * Set code.
     */    
    public void setCode(String code) {
  	    this.code = code;
    }

    /**
     * Return name.
     */    
    public String getName() {
  	    return name;
    }

    /**
     * Set name.
     */    
    public void setName(String name) {
  	    this.name = name;
    }


}

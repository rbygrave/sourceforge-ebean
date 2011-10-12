package com.avaje.tests.model.basic;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.validation.Length;

/**
 * Product entity bean.
 */
@CacheStrategy(warmingQuery="order by name")
@Entity
@Table(name="o_product")
public class Product {

    @Id
    Integer id;

    @Length(max=20)
    String sku;

    String name;

    @CreatedTimestamp
    Timestamp cretime;

    @Version
    Timestamp updtime;

    /**
     * Return id.
     */    
    public Integer getId() {
  	    return id;
    }

    /**
     * Set id.
     */    
    public void setId(Integer id) {
  	    this.id = id;
    }

    /**
     * Return sku.
     */    
    public String getSku() {
  	    return sku;
    }

    /**
     * Set sku.
     */    
    public void setSku(String sku) {
  	    this.sku = sku;
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

    /**
     * Return cretime.
     */    
    public Timestamp getCretime() {
  	    return cretime;
    }

    /**
     * Set cretime.
     */    
    public void setCretime(Timestamp cretime) {
  	    this.cretime = cretime;
    }

    /**
     * Return updtime.
     */    
    public Timestamp getUpdtime() {
  	    return updtime;
    }

    /**
     * Set updtime.
     */    
    public void setUpdtime(Timestamp updtime) {
  	    this.updtime = updtime;
    }
    
}

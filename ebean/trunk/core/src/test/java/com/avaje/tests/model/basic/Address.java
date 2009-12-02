package com.avaje.tests.model.basic;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.avaje.ebean.validation.Length;

/**
 * Address entity bean.
 */
@Entity
@Table(name="o_address")
public class Address {

    @Id
    Short id;
    
    @Length(max=100)
    @Column(name="line_1")
    String line1;

    @Length(max=100)
    @Column(name="line_2")
    String line2;

    @Length(max=100)
    String city;

    Timestamp cretime;

    @Version
    Timestamp updtime;

    @ManyToOne
    Country country;

    
    public String toString() {
    	return id+" "+line1+" "+line2+" "+city+" "+country;
    }

    /**
     * Return id.
     */    
    public Short getId() {
  	    return id;
    }

    /**
     * Set id.
     */    
    public void setId(Short id) {
  	    this.id = id;
    }

    /**
     * Return line 1.
     */    
    public String getLine1() {
  	    return line1;
    }

    /**
     * Set line 1.
     */    
    public void setLine1(String line1) {
  	    this.line1 = line1;
    }

    /**
     * Return line 2.
     */    
    public String getLine2() {
  	    return line2;
    }

    /**
     * Set line 2.
     */    
    public void setLine2(String line2) {
  	    this.line2 = line2;
    }

    /**
     * Return city.
     */    
    public String getCity() {
  	    return city;
    }

    /**
     * Set city.
     */    
    public void setCity(String city) {
  	    this.city = city;
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

    /**
     * Return country.
     */    
    public Country getCountry() {
  	    return country;
    }

    /**
     * Set country.
     */    
    public void setCountry(Country country) {
  	    this.country = country;
    }

}

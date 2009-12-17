package com.avaje.tests.model.ddd;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.joda.time.Interval;

import com.avaje.ebean.annotation.EmbeddedColumns;
import com.avaje.tests.model.ivo.CMoney;
import com.avaje.tests.model.ivo.Money;
import com.avaje.tests.model.ivo.Oid;

@Entity
public class DPerson {

    @Id
    Oid<DPerson> id;
    
    String firstName;
    
    String lastName;

    Money salary;
    
    @EmbeddedColumns(columns="amount=a_amt, currency=a_curr")
    CMoney cmoney;
    
    @EmbeddedColumns(columns="startMillis=i_start, endMillis=i_end")
    Interval interval;
    
    public String toString() {
        return id+" "+firstName+" "+lastName+" "+salary;
    }
    
    public Oid<DPerson> getId() {
        return id;
    }

    public void setId(Oid<DPerson> id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Money getSalary() {
        return salary;
    }

    public void setSalary(Money salary) {
        this.salary = salary;
    }

    public CMoney getCmoney() {
        return cmoney;
    }

    public void setCmoney(CMoney cmoney) {
        this.cmoney = cmoney;
    }

    public Interval getInterval() {
        return interval;
    }

    public void setInterval(Interval interval) {
        this.interval = interval;
    }
    
}

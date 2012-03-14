package com.avaje.tests.model.basic;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.CreatedTimestamp;

@Entity
@CacheStrategy(useBeanCache=true,naturalKey="email")
public class Contact {

    private static final long serialVersionUID = 1L;

    @Id
    int id;

    String firstName;
    String lastName;

    String phone;
    String mobile;
    String email;

    @ManyToOne
    Customer customer;
    
    @ManyToOne(optional=true)
    ContactGroup group;
    
    @OneToMany
    List<ContactNote> notes;

    @CreatedTimestamp
    Timestamp cretime;

    @Version
    Timestamp updtime;


    public Contact(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Contact() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getUpdtime() {
        return updtime;
    }

    public void setUpdtime(Timestamp updtime) {
        this.updtime = updtime;
    }

    public Timestamp getCretime() {
        return cretime;
    }

    public void setCretime(Timestamp cretime) {
        this.cretime = cretime;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public ContactGroup getGroup() {
        return group;
    }

    public void setGroup(ContactGroup group) {
        this.group = group;
    }

    public List<ContactNote> getNotes() {
        return notes;
    }

    public void setNotes(List<ContactNote> notes) {
        this.notes = notes;
    }

}

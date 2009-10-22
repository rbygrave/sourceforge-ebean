package com.avaje.tests.model.basic;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Contact extends BasicDomain {

	private static final long serialVersionUID = 1L;
	
	
	String firstName;
	String lastName;
	
	String phone;
	String mobile;
	String email;
	
	@ManyToOne
	Customer customer;

	public Contact(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public Contact() {
		
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
	
}

package com.avaje.tests.model.basic;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Inheritance;

@Entity
@Inheritance
@DiscriminatorColumn(length=3)
public abstract class Vehicle extends BasicDomain {

	private static final long serialVersionUID = -3060920549470002030L;
	
	private String licenseNumber;

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

}

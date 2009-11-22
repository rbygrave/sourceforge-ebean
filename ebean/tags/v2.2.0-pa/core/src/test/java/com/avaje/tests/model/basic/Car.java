package com.avaje.tests.model.basic;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.ManyToOne;

@Entity
@Inheritance
@DiscriminatorValue("C")
public class Car extends Vehicle {

	private static final long serialVersionUID = 4716705779684333446L;

	private String driver;

	@ManyToOne
	TruckRef carRef;
	
	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public TruckRef getCarRef() {
		return carRef;
	}

	public void setCarRef(TruckRef carRef) {
		this.carRef = carRef;
	}
}

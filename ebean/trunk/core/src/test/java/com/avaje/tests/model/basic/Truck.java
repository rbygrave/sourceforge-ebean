package com.avaje.tests.model.basic;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;

@Entity
@Inheritance
@DiscriminatorValue("T")
public class Truck extends Vehicle {

	private static final long serialVersionUID = 7433386912403859900L;
	
	private Double capacity;

	public Double getCapacity() {
		return capacity;
	}

	public void setCapacity(Double capacity) {
		this.capacity = capacity;
	}

}

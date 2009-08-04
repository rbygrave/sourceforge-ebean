package com.avaje.tests.inheritance;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.VehicleDriver;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestInheritInsert extends TestCase {

	public void test() {
		
		VehicleDriver d = new VehicleDriver();
		d.setName("Rob");
		
		Ebean.save(d);
		
		VehicleDriver driver = Ebean.find(VehicleDriver.class, d.getId());
		
		Assert.assertNotNull(driver);
		
	}
	
}

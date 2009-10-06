package com.avaje.tests.inheritance;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Car;
import com.avaje.tests.model.basic.VehicleDriver;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.List;

public class TestInheritInsert extends TestCase {

	public void test() {
		
		VehicleDriver d = new VehicleDriver();
		d.setName("Rob");
		
		Ebean.save(d);
		
		VehicleDriver driver = Ebean.find(VehicleDriver.class, d.getId());
		
		Assert.assertNotNull(driver);
		
	}

	public void testQuery()
	{
		Car car = new Car();
		car.setLicenseNumber("MARIOS_CAR_LICENSE");
		Ebean.save(car);

		VehicleDriver driver = new VehicleDriver();
		driver.setName("Mario");
		driver.setVehicle(car);
		Ebean.save(driver);

		Query<VehicleDriver> query = Ebean.find(VehicleDriver.class);
		query.where().eq("vehicle.licenseNumber", "MARIOS_CAR_LICENSE");
		List<VehicleDriver> drivers = query.findList();
		
		Assert.assertNotNull(drivers);
		Assert.assertEquals(1, drivers.size());
		Assert.assertNotNull(drivers.get(0));

		Assert.assertEquals("Mario", drivers.get(0).getName());
		Assert.assertEquals("MARIOS_CAR_LICENSE", drivers.get(0).getVehicle().getLicenseNumber());
	}
}

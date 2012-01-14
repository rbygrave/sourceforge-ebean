package com.avaje.tests.basic;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.TMapSuperEntity;

public class TestDeletePartialNoVersion extends TestCase {

	public void testNoVersion() {
		
		TMapSuperEntity e = new TMapSuperEntity();
		e.setName("babanaone");
		
		Ebean.save(e);
		
		// select includes a transient property
		TMapSuperEntity e2 = Ebean.find(TMapSuperEntity.class)
			.where().idEq(e.getId())
			.select("id, name")
			.findUnique();
		
		Assert.assertNotNull(e2);
	
		e2.setName("banaban2");
		Ebean.save(e2);
		
		Ebean.delete(e2);
	}
	
	
	public void testWithVersion() {
		
		TMapSuperEntity e = new TMapSuperEntity();
		e.setName("babanatwo");
		
		Ebean.save(e);
		
		// select includes a transient property
		TMapSuperEntity e2 = Ebean.find(TMapSuperEntity.class)
			.where().idEq(e.getId())
			.select("id, name, version")
			.findUnique();
		
		Assert.assertNotNull(e2);
	
		e2.setName("banaban2two");
		Ebean.save(e2);
		
		Ebean.delete(e2);
	}
}

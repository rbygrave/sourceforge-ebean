package com.avaje.tests.basic.lazyload;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestTransient extends TestCase {

	
	public void testTransient() {
		
		ResetBasicData.reset();
		
		Customer c = Ebean.find(Customer.class)
			.setAutofetch(false)
			.setId(1)
			.findUnique();
		
		Assert.assertNotNull(c);
		
		c.getLock().tryLock();
		try {
			c.setSelected(Boolean.TRUE);			
		} finally {
			c.getLock().unlock();	
		}
		
		
		Boolean selected = c.getSelected();
		Assert.assertNotNull(selected);
		
		Ebean.save(c);
		
		selected = c.getSelected();
		Assert.assertNotNull(selected);
		
		c.setName("Modified");
		
		selected = c.getSelected();
		Assert.assertNotNull(selected);
		
	}
}

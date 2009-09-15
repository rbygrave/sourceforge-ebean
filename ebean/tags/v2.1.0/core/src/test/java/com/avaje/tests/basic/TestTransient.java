package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.BeanState;
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

		BeanState beanState = Ebean.getBeanState(c);
		Assert.assertFalse("not new or dirty as transient", beanState.isNewOrDirty());

		
		c.getLock().tryLock();
		try {
			c.setSelected(Boolean.TRUE);			
		} finally {
			c.getLock().unlock();	
		}
		
		
		Boolean selected = c.getSelected();
		Assert.assertNotNull(selected);
		
		Assert.assertFalse("not new or dirty as transient", beanState.isNewOrDirty());
		
		Ebean.save(c);
		
		selected = c.getSelected();
		Assert.assertNotNull(selected);
		
		c.setName("Modified");
		Assert.assertTrue("dirty now", beanState.isNewOrDirty());

		
		selected = c.getSelected();
		Assert.assertNotNull(selected);
		
		Ebean.save(c);
		Assert.assertFalse("Not dirty after save", beanState.isNewOrDirty());

		selected = c.getSelected();
		Assert.assertNotNull(selected);
		
		String updateStmt = "update customer set name = 'Rob' where id = :id";
		int rows = Ebean.createUpdate(Customer.class, updateStmt)
			.set("id", 1)
			.execute();
		
		Assert.assertTrue("changed name back", 1 == rows);
	}
}

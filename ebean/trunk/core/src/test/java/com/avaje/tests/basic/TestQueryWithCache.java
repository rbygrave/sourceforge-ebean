package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryWithCache extends TestCase {

	
	public void testJoinCache() {

		ResetBasicData.reset();
		
		Query<Order> query = Ebean.createQuery(Order.class)
			.setAutofetch(false)
			.join("customer","+cache +readonly")
			.setId(1);
		
		Order order = query.findUnique();
		Customer customer = order.getCustomer();
		BeanState custState = Ebean.getBeanState(customer);
		Assert.assertTrue(custState.isReference());
		
		// invoke lazy loading
		customer.getName();
		
		order = query.findUnique();
		customer = order.getCustomer();
		custState = Ebean.getBeanState(customer);
		Assert.assertFalse(custState.isReference());
		
		//custState.
		System.out.println("done");
		
	}
	
	public void testFindId() {
		
		ResetBasicData.reset();

		Order o  = Ebean.find(Order.class)
			.setUseCache(true)
			.setReadOnly(true)
			.setId(1)
			.findUnique();

		BeanState beanState = Ebean.getBeanState(o);
		Assert.assertTrue(beanState.isReadOnly());

		Order o2  = Ebean.find(Order.class)
			.setUseCache(true)
			.setReadOnly(true)
			.setId(1)
			.findUnique();

		BeanState beanState2 = Ebean.getBeanState(o2);

		// same instance as readOnly = true
		Assert.assertTrue("same instance", o == o2);
		Assert.assertTrue(beanState2.isReadOnly());
		
		Order o3  = Ebean.find(Order.class)
			.setUseCache(true)
			.setReadOnly(false)
			.setId(1)
			.findUnique();

		// NOT the same instance as readOnly = false
		Assert.assertTrue("not same instance", o != o3);


	}
}

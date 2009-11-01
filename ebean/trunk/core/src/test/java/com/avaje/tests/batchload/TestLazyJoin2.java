package com.avaje.tests.batchload;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.JoinConfig;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestLazyJoin2 extends TestCase {

	public void testLazyOnNonLoaded() {

		ResetBasicData.reset();

		List<Order> list = Ebean.find(Order.class)
			//.select("status")
			.join("customer", new JoinConfig().query(3).lazy(10))
			.findList();
			//.join("customer.contacts");

		//List<Order> list = query.findList();

		
		Order order = list.get(0);
		Customer customer = order.getCustomer();
		
		// this invokes lazy loading on a property that is
		// not one of the selected ones (name, status) ... and
		// therefore the lazy load query selects all properties
		// in the customer (not just name and status)
		Address billingAddress = customer.getBillingAddress();

		Assert.assertNotNull(billingAddress);
	}
	
}

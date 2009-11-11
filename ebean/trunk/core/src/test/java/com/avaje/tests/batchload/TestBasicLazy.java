package com.avaje.tests.batchload;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestBasicLazy extends TestCase {

	public void testQueries() {

		ResetBasicData.reset();

		Order order = Ebean.find(Order.class)
			.select("totalAmount")
			.setMaxRows(1)
			.findUnique();

		Assert.assertNotNull(order);

		Customer customer = order.getCustomer();
		Assert.assertNotNull(customer);
		Assert.assertNotNull(customer.getName());

		Address address = customer.getBillingAddress();
		Assert.assertNotNull(address);
		Assert.assertNotNull(address.getCity());
	}
}
package com.avaje.tests.batchload;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestSecondaryQueries extends TestCase {

	public void testQueries() {
		
		ResetBasicData.reset();
		
		Order testOrder = ResetBasicData.createOrderCustAndOrder("testSecQry10");
		Integer custId = testOrder.getCustomer().getId();
		
		
		Customer cust  = Ebean.find(Customer.class)
			.select("name")
			.join("contacts","+query")
			.setId(custId)
			.findUnique();
		
		Assert.assertNotNull(cust);
		
		List<Order> list = Ebean.find(Order.class)
			.select("status")
			.join("details","+query(10)")
			.join("customer","+query name, status")
			.join("customer.contacts")
			.where().eq("status", Order.Status.NEW)
			.findList();

		Assert.assertTrue(list.size() > 0);
	}
	
}

package com.avaje.tests.basic;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.AdminAutofetch;
import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.OrderDetail;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestBatchLazy extends TestCase {

	public void testMe() {
		
		ResetBasicData.reset();
		
		List<Order> list = Ebean.find(Order.class)
			.findList();
		
		Order order = list.get(0);
		
		List<OrderDetail> details = order.getDetails();
		details.get(0).getProduct().getSku();
		
		
		Customer customer = order.getCustomer();
		//Assert.assertTrue(Ebean.getBeanState(customer).isReference());
		
		customer.getName();
		//customer.getCretime();
		//Assert.assertFalse(Ebean.getBeanState(customer).isReference());
		
		AdminAutofetch adminAutofetch = Ebean.getServer(null).getAdminAutofetch();
		adminAutofetch.collectUsageViaGC();

	}
	
	
	
}

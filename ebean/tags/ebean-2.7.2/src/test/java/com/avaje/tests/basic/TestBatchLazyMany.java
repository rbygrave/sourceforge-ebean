package com.avaje.tests.basic;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestBatchLazyMany extends TestCase {

	
	public void testMe() {
		
		ResetBasicData.reset();
	
		Order order2 = Ebean.getReference(Order.class, 1);
		order2.getOrderDate();
		System.out.println("done");
		
//		List<Order> list = Ebean.find(Order.class)
//			//.join("details")
//			//.join("details", "+fetchquery")
//			.findList();
//		
//		Order order = list.get(0);
//		//List<OrderDetail> details = order.getDetails();
//		//details.size();
//		
//		Customer customer = order.getCustomer();
//		customer.getName();
//		System.out.println("done");
		
	}
	
	
}

package com.avaje.tests.batchload;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryJoin extends TestCase {

	
	public void test() {
		
		ResetBasicData.reset();
		
//		Query<Order> query = Ebean.find(Order.class)
//			.select("status, orderDate, shipDate")//, customer
//			.join("customer","+query +lazy(20) name")//, billingAddress
//			.join("customer.billingAddress","+query");
//		
//		List<Order> list = query.findList();

		
Query<Order> query = Ebean.find(Order.class)
	.select("status")
	//.join("details","+query(10)")
	.join("customer","+lazy(10) name, status")
	.join("customer.contacts");
	//.join("customer.billingAddress");

List<Order> list = query.findList();
	
		//list.get(0).getShipDate();
		
		Order order = list.get(0);
		BeanState beanStateOrder = Ebean.getBeanState(order);
		Assert.assertNotNull(beanStateOrder.getLoadedProps());
		//Assert.assertTrue(beanStateOrder.getLoadedProps().contains("id"));
		Assert.assertTrue(beanStateOrder.getLoadedProps().contains("status"));
		Assert.assertTrue(beanStateOrder.getLoadedProps().contains("shipments"));
		Assert.assertTrue(beanStateOrder.getLoadedProps().contains("customer"));
		
		Customer customer = order.getCustomer();
		BeanState beanStateCustomer = Ebean.getBeanState(customer);
		Assert.assertNull(beanStateCustomer.getLoadedProps());
		
		customer.getName();
		Assert.assertNotNull(beanStateCustomer.getLoadedProps());
		Assert.assertTrue(beanStateCustomer.getLoadedProps().contains("name"));
		Assert.assertTrue(beanStateCustomer.getLoadedProps().contains("status"));
		Assert.assertFalse(beanStateCustomer.getLoadedProps().contains("billingAddress"));
		
		customer.getName();
		
		Address billingAddress = customer.getBillingAddress();
		System.out.println(billingAddress);
		billingAddress.getLine1();
		
		Assert.assertTrue(list.size() > 0);
		
	}
}

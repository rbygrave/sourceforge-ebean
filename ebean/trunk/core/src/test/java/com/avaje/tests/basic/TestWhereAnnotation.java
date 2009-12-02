package com.avaje.tests.basic;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestWhereAnnotation extends TestCase {

	public void testWhere() {
		
		ResetBasicData.reset();
		Customer custTest = ResetBasicData.createCustAndOrder("testWhereAnn");
		
		Customer customer = Ebean.find(Customer.class, custTest.getId());
		List<Order> orders = customer.getOrders();
		
		Assert.assertTrue(orders.size() > 0);
		
		
		Query<Customer> q1 = Ebean.find(Customer.class)
			.join("orders")
			.where().idEq(1)
			.query();
		
		q1.findUnique();
		String s1 = q1.getGeneratedSql();
		Assert.assertTrue(s1.contains("co.order_date is not null"));
	}
}

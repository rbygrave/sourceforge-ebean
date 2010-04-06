package com.avaje.tests.basic;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.List;

public class TestOrderByAnnotation extends TestCase {

	public void testOrderBy() {

		ResetBasicData.reset();
		Customer custTest = ResetBasicData.createCustAndOrder("testOrderByAnn");

		Customer customer = Ebean.find(Customer.class, custTest.getId());
		List<Order> orders = customer.getOrders();

		Assert.assertTrue(orders.size() > 0);


		Query<Order> q1 = Ebean.find(Order.class)
			.join("details");

		q1.findList();
		
		String s1 = q1.getGeneratedSql();

		Assert.assertTrue(s1.contains("order by o.id , od.id asc, od.order_qty asc, od.cretime desc"));
	}
}
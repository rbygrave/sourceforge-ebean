package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryParse extends TestCase {

	public void test() {
		//GlobalProperties.put("ebean.ddl.generate", "false");
		//GlobalProperties.put("ebean.ddl.run", "false");
		ResetBasicData.reset();
		
		String oql = "where 1=1 order by customer.name desc, status";
		Query<Order> query = Ebean.createQuery(Order.class)
			.setQuery(oql);
		
		OrderBy<Order> order = query.order();
		Assert.assertTrue(order.getProperties().size() == 2);
		Assert.assertEquals("customer.name",order.getProperties().get(0).getProperty());
		Assert.assertFalse(order.getProperties().get(0).isAscending());
		Assert.assertEquals("status",order.getProperties().get(1).getProperty());
		Assert.assertTrue(order.getProperties().get(1).isAscending());
	}
	
}

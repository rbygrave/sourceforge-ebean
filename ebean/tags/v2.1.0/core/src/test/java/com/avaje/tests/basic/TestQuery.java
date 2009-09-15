package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQuery extends TestCase {

	public void testCountOrderBy() {
		
		ResetBasicData.reset();
		
		Query<Order> query = Ebean.find(Order.class)
			.setAutofetch(false)
			.orderBy("orderDate");
		
		int rc = query.findRowCount();
		Assert.assertTrue(rc > 0);
	}
}

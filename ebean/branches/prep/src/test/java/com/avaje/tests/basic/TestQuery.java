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
			.order().asc("orderDate")
			.order().desc("id");
			//.orderBy("orderDate");
		
		int rc = query.findList().size();
		//int rc = query.findRowCount();
		Assert.assertTrue(rc > 0);
		//String generatedSql = query.getGeneratedSql();
		//Assert.assertFalse(generatedSql.contains("order by"));
		
	}
}

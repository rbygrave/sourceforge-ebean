package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestBackgroundFetchAfter extends TestCase {
	
	public void testWrtJoin() {
		
		ResetBasicData.reset();
		
		// limit not in sql as join to many
		Query<Order> q = Ebean.find(Order.class)
			.join("details")
			.setBackgroundFetchAfter(3)
			.setMaxRows(10);
		
		q.findList();
		String sql = q.getGeneratedSql();
		
		Assert.assertTrue(sql.indexOf("limit") == -1);
		
		// allows limit use as no join to many
		q = Ebean.find(Order.class)
			.setBackgroundFetchAfter(3)
			.setMaxRows(10);
	
		q.findList();
		sql = q.getGeneratedSql();
		
		Assert.assertTrue(sql.indexOf("limit") > -1);

		// allows limit use as join to one (not many)
		q = Ebean.find(Order.class)
			.join("customer")
			.setBackgroundFetchAfter(3)
			.setMaxRows(10);
	
		q.findList();
		sql = q.getGeneratedSql();
		
		Assert.assertTrue(sql.indexOf("limit") > -1);

	}

}

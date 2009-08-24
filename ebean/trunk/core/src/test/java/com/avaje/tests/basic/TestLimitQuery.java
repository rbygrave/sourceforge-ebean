package com.avaje.tests.basic;

import junit.framework.TestCase;

public class TestLimitQuery extends TestCase {

	public void testNothing() {
		
	}
	
//	public void testLimitWithMany() {
//		ResetBasicData.reset();
//		
//		Query<Order> query = Ebean.find(Order.class)
//			.setAutofetch(false)
//			.join("details")
//			.where().gt("details.id", 0)
//			.setMaxRows(10);
//			//.findList();
//		
//		List<Order> list = query.findList();
//		
//		Assert.assertTrue("sz > 0", list.size() > 0);
//	
//		String sql = query.getGeneratedSql();
//		boolean hasDetailsJoin = sql.indexOf("left outer join o_order_detail") > -1;
//		boolean hasLimit = sql.indexOf("limit 11") > -1;
//		boolean hasSelectedDetails = sql.indexOf("od.id,") > -1;
//		boolean hasDistinct = sql.indexOf("select distinct") > -1;
//		
//		Assert.assertTrue(hasDetailsJoin);
//		Assert.assertTrue(hasLimit);
//		Assert.assertFalse(hasSelectedDetails);
//		Assert.assertTrue(hasDistinct);
//		
//		query = Ebean.find(Order.class)
//			.setAutofetch(false)
//			.join("details")
//			.setMaxRows(10);
//		
//		query.findList();
//		
//		sql = query.getGeneratedSql();
//		hasDetailsJoin = sql.indexOf("left outer join o_order_detail") > -1;
//		hasLimit = sql.indexOf("limit 11") > -1;
//		hasSelectedDetails = sql.indexOf("od.id") > -1;
//		hasDistinct = sql.indexOf("select distinct") > -1;
//		
//		System.out.println(">>");
//		System.out.println(sql);
//		System.out.println("<<");
//		Assert.assertFalse("no join with maxRows",hasDetailsJoin);
//		Assert.assertTrue(hasLimit);
//		Assert.assertFalse(hasSelectedDetails);
//		Assert.assertFalse(hasDistinct);
//			
//	}
}

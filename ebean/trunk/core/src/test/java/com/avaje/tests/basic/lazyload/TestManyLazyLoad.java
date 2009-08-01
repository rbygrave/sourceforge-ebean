package com.avaje.tests.basic.lazyload;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.OrderDetail;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestManyLazyLoad extends TestCase {

	
	public void testLazyLoadRef() {
	
		ResetBasicData.reset();
		
		List<Order> list = Ebean.find(Order.class).findList();
		Assert.assertTrue("sz > 0", list.size() > 0);
		
		// just use the first one
		Order order = list.get(0);
		
		// get it as a reference
		Order order1 = Ebean.getReference(Order.class, order.getId());
		Assert.assertNotNull(order1);
		List<OrderDetail> details = order1.getDetails();
		
		// lazy load the details
		int sz = details.size();
		Assert.assertTrue("sz > 0", sz > 0);
		
		
	}
	
}

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
		
		Order order1 = Ebean.getReference(Order.class, 1);
		Assert.assertNotNull(order1);
		List<OrderDetail> details = order1.getDetails();
		int sz = details.size();
		Assert.assertTrue("sz > 0", sz > 0);
		
		
	}
	
}

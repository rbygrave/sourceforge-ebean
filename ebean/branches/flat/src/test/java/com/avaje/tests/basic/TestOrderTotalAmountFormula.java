package com.avaje.tests.basic;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestOrderTotalAmountFormula extends TestCase {

//	public void testTransientNature() {
//
//		ResetBasicData.reset();
//		
//        Order o0 = Ebean.find(Order.class)
//	    	.setAutofetch(false)
//	    	.setId(1)
//	    	.findUnique();
//
//	    Assert.assertFalse(Ebean.getBeanState(o0).isReference());
//	    Assert.assertNull(Ebean.getBeanState(o0).getLoadedProps());
//	    
//	    // these are transient... not included in query by default
//	    // ... and don't invoke lazy loading
//	    Assert.assertNull(o0.getTotalAmount());
//	    Assert.assertNull(o0.getTotalItems());
//
//	    Order o1 = Ebean.find(Order.class)
//	    	.select("id, totalAmount, totalItems")
//	    	.setId(1)
//	    	.findUnique();
//
//	    Assert.assertFalse(Ebean.getBeanState(o1).isReference());
//	    Set<String> props = Ebean.getBeanState(o1).getLoadedProps();
//	    Assert.assertNotNull(props);
//	    Assert.assertTrue(props.contains("id"));
//	    Assert.assertTrue(props.contains("totalAmount"));
//	    Assert.assertTrue(props.contains("totalItems"));
//	    Assert.assertFalse(props.contains("status"));
//	    
//	    Assert.assertNotNull(o1.getTotalAmount());
//	    Assert.assertNotNull(o1.getTotalItems());
//
//	    // invoke lazy loading
//	    o1.getStatus();
//	    
//	    // now fully loaded - propsAfterLazyLoad becomes null (all properties loaded)
//	    Set<String> propsAfterLazyLoad = Ebean.getBeanState(o1).getLoadedProps();
//	    Assert.assertNull(propsAfterLazyLoad);
//	    
//	    Assert.assertNotNull(o1.getTotalAmount());
//	    Assert.assertNotNull(o1.getTotalItems());
//	    
//	}
//	
//    public void test() {
//
//        ResetBasicData.reset();
//        
//        
//        List<Order> list = Ebean.find(Order.class)
//        	.select("id, totalAmount, totalItems")
//        	.where()
//        		.eq("status", Order.Status.NEW)
//        		.gt("totalItems", 1)
//        		.gt("totalAmount", 10)
//        	.findList();
//        
//        Assert.assertTrue(list.size() > 0);
//        
//        for (Order order : list) {
//        	Integer id = order.getId();
//        	Double totalAmount = order.getTotalAmount();
//        	Integer totalItems = order.getTotalItems();
//        	System.out.println("id:"+id+" totalAmount:"+totalAmount+" totalItems:"+totalItems);
//        	
//        	Assert.assertNotNull(id);
//        	Assert.assertTrue(totalAmount > 10);
//        	Assert.assertTrue(totalItems > 1);
//		}
//        
//
//		List<Order> l2 = Ebean.find(Order.class)
//			.select("id, totalAmount")
//			.where()
//				.eq("status", Order.Status.NEW)
//				.gt("totalItems", 1)
//				.gt("totalAmount", 10)
//			.findList();
//
//        Assert.assertTrue(l2.size() > 0);
//        
//        for (Order order : l2) {
//        	Integer id = order.getId();
//        	Double totalAmount = order.getTotalAmount();
//        	Integer totalItems = order.getTotalItems();
//        	System.out.println("id:"+id+" totalAmount:"+totalAmount+" totalItems:"+totalItems);
//        	
//        	Assert.assertNotNull(id);
//        	Assert.assertTrue(totalAmount > 10);
//        	Assert.assertNull(totalItems);
//		}
//    }

    public void testAsJoin() {

    	ResetBasicData.reset();
    	
      List<Customer> l0 = Ebean.find(Customer.class)
    	.select("id, name")
    	.join("orders", "status, totalAmount")
    	.where()
    		.gt("id", 0)
    		.gt("orders.totalAmount", 10)
    	.findList();

	    for (Customer c0 : l0) {
			System.out.println("customer: "+c0.getId());
			List<Order> orders = c0.getOrders();
			for (Order order : orders) {
				System.out.println("... order:"+order);
			}
		}

    }
    
}

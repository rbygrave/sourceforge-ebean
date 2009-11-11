package com.avaje.tests.batchload;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.JoinConfig;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Contact;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestLazyJoin2 extends TestCase {

	public void testLazyOnNonLoaded() {

		ResetBasicData.reset();

		 // This will use 3 SQL queries to build this object graph
		 List<Order> l0 = Ebean.find(Order.class)
		   .select("status, shipDate")
		   
		   .join("details", "orderQty, unitPrice", new JoinConfig().query())
		   .join("details.product", "sku, name")
		   
		   .join("customer", "name", new JoinConfig().query(10))
		   .join("customer.contacts","firstName, lastName, mobile")
		   .join("customer.shippingAddress","line1, city")
		   .findList();
		 
		 
		 Order o0 = l0.get(0);
		 Customer c0 = o0.getCustomer();
		 List<Contact> contacts = c0.getContacts();
		 Assert.assertTrue(contacts.size() > 0);
		 
		 // query 1) find order (status, shipDate)
		 // query 2) find orderDetail (quantity, price) join product (sku, name) where order.id in (?,? ...)
		 // query 3) find customer (name) join contacts (*) join shippingAddress (*) where id in (?,?,?,?,?)
		 
				 

				 
		List<Order> orders = Ebean.find(Order.class)
			//.select("status")
			.join("customer", new JoinConfig().query(3).lazy(10))
			.findList();
			//.join("customer.contacts");

		//List<Order> list = query.findList();

		
		Order order = orders.get(0);
		Customer customer = order.getCustomer();
		
		// this invokes lazy loading on a property that is
		// not one of the selected ones (name, status) ... and
		// therefore the lazy load query selects all properties
		// in the customer (not just name and status)
		Address billingAddress = customer.getBillingAddress();

		Assert.assertNotNull(billingAddress);
	}
	
}

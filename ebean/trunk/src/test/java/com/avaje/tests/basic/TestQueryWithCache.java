package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.core.ReferenceOptions;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.tests.model.basic.Country;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryWithCache extends TestCase {

	
	public void testJoinCache() {

		ResetBasicData.reset();

		Ebean.getServer(null).runCacheWarming();
		
		Query<Order> query = Ebean.createQuery(Order.class)
			.setAutofetch(false)
			.join("customer","+cache +readonly")
			.setId(1);
		
		Order order = query.findUnique();
		Customer customer = order.getCustomer();
		Assert.assertTrue(Ebean.getBeanState(customer).isReadOnly());
		
//		// invoke lazy loading
//		customer.getName();
//		
//		order = query.findUnique();
//		customer = order.getCustomer();
//		custState = Ebean.getBeanState(customer);
//		Assert.assertFalse(custState.isReadOnly());
		
	}
	
	public void testFindId() {
		
		ResetBasicData.reset();

		Order o  = Ebean.find(Order.class)
			.setUseCache(true)
			.setReadOnly(true)
			.setId(1)
			.findUnique();

		BeanState beanState = Ebean.getBeanState(o);
		Assert.assertTrue(beanState.isReadOnly());

		Order o2  = Ebean.find(Order.class)
			.setUseCache(true)
			.setReadOnly(true)
			.setId(1)
			.findUnique();

		BeanState beanState2 = Ebean.getBeanState(o2);

		// same instance as readOnly = true
		Assert.assertTrue("same instance", o == o2);
		Assert.assertTrue(beanState2.isReadOnly());
		
		Order o3  = Ebean.find(Order.class)
			.setUseCache(true)
			.setReadOnly(false)
			.setId(1)
			.findUnique();

		// NOT the same instance as readOnly = false
		Assert.assertTrue("not same instance", o != o3);
	}
	
	public void testCountryDeploy() {

		ResetBasicData.reset();
		
		SpiEbeanServer server = (SpiEbeanServer)Ebean.getServer(null);
		BeanDescriptor<Country> beanDescriptor = server.getBeanDescriptor(Country.class);
		ReferenceOptions referenceOptions = beanDescriptor.getReferenceOptions();
		
		Assert.assertNotNull(referenceOptions);
		Assert.assertTrue(referenceOptions.isUseCache());
		Assert.assertTrue(referenceOptions.isReadOnly());
		
		
		Country nz1 = Ebean.getReference(Country.class, "NZ");
		// has the effect of loading the cache via lazy loading
		nz1.getName();

		Country nz2 = Ebean.getReference(Country.class, "NZ");
		Country nz2b = Ebean.getReference(Country.class, "NZ");
		
		Country nz3 = Ebean.find(Country.class, "NZ");

		Country nz4 = Ebean.find(Country.class)
			.setId("NZ")
			.setAutofetch(false)
			.setUseCache(false)
			.findUnique();
		
		Assert.assertTrue(nz2 == nz2b);
		Assert.assertTrue(nz2 == nz3);
		Assert.assertTrue(nz3 != nz4);
		
	}
}

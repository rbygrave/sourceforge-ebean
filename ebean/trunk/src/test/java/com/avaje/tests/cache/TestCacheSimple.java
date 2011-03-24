package com.avaje.tests.cache;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheStatistics;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Country;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestCacheSimple extends TestCase {

	
	public void test(){
		
		ResetBasicData.reset();
		
		Ebean.getServerCacheManager().clear(Country.class);
		ServerCache countryCache = Ebean.getServerCacheManager().getBeanCache(Country.class);
		
		Ebean.runCacheWarming(Country.class);
		Assert.assertTrue(countryCache.size() > 0);
		
		// reset the statistics
		countryCache.getStatistics(true);
		
		Country c0 = Ebean.getReference(Country.class, "NZ");
		ServerCacheStatistics statistics = countryCache.getStatistics(false);
		int hc = statistics.getHitCount();
		Assert.assertEquals(0, hc);

		Country c1 = Ebean.getReference(Country.class, "NZ");
		Assert.assertEquals(0, countryCache.getStatistics(false).getHitCount());
		//Assert.assertEquals(100, countryCache.getStatistics(false).getHitRatio());

		// same instance as caching with readOnly=true
		Assert.assertTrue(c0 != c1);

		c0.getName();
		c1.getName();
		
		// reset the statistics
		Assert.assertEquals(2,countryCache.getStatistics(true).getHitCount());
		// now the count should be 0 again 
		Assert.assertEquals(0, countryCache.getStatistics(false).getHitCount());
		// and hitRatio is 0 as well
		Assert.assertEquals(0, countryCache.getStatistics(false).getHitRatio());

		// hit the country cache automatically via join
		
		Customer custTest = ResetBasicData.createCustAndOrder("cacheBasic");
		Integer id = custTest.getId();
		Customer customer = Ebean.find(Customer.class, id);
		
		Address billingAddress = customer.getBillingAddress();
		Country c2 = billingAddress.getCountry();
		c2.getName();

		ServerCacheStatistics statistics2 = countryCache.getStatistics(false);
		System.out.println(statistics2);



		// clear the cache
		Ebean.getServerCacheManager().clear(Country.class);
		// reset statistics
		countryCache.getStatistics(true);

		// try to hit the country cache automatically via join
		customer = Ebean.find(Customer.class, id);
		billingAddress = customer.getBillingAddress();
		Country c5 = billingAddress.getCountry();
		// but cache is empty at this point
		Assert.assertEquals("empty cache",0,countryCache.getStatistics(false).getSize());
		
		// lazy load on c5 populates the cache
		c5.getName();
		Assert.assertEquals("cache populated via lazy load",1,countryCache.getStatistics(false).getSize());
		
		int hcBefore = countryCache.getStatistics(false).getHitCount();
		// now these get hits in the cache
		Country c6 = Ebean.find(Country.class, "NZ");
		Assert.assertNotNull(c6);
		
		int hcAfter = countryCache.getStatistics(false).getHitCount();
		Assert.assertEquals("hit count increments",hcBefore+1, hcAfter);
				

		
		Country cReadOnly = Ebean.find(Country.class)
			.setId("NZ")
			.setReadOnly(true)
			.findUnique();
		
		// Explicitly readOnly 
		Assert.assertTrue("read only",Ebean.getBeanState(cReadOnly).isReadOnly());
		
		Country c8 = Ebean.find(Country.class)
			.setId("NZ")
			.findUnique();
		
		Assert.assertEquals("1 countries in cache", 1, countryCache.size()); 
		c8.setName("Nu Zilund");
		// the update will remove the entry from the cache
		Ebean.save(c8);
		
		Assert.assertEquals("0 country in cache", 0, countryCache.size()); 
		
		Country c9 = Ebean.find(Country.class)
			.setReadOnly(false)
			.setId("NZ")
			.findUnique();

		// Find loads cache ...
		Assert.assertFalse(Ebean.getBeanState(c9).isReadOnly()); 
		Assert.assertTrue(countryCache.size() > 0); 
		
		Ebean.getServerCacheManager().clear(Country.class);
		Assert.assertEquals("0 country in cache", 0, countryCache.size()); 

		// reference doesn't load cache yet
		Country c11 = Ebean.getReference(Country.class, "NZ");

		// still 0 in cache
		Assert.assertEquals("0 country in cache", 0, countryCache.size());

		// will invoke lazy loading..
		c11.getName();
		Assert.assertTrue(countryCache.size() > 0);

	}
}

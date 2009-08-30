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

public class TestCacheBasic extends TestCase {

	
	public void test(){
		
		ResetBasicData.reset();
		
		ServerCache countryCache = Ebean.getServerCacheManager().getBeanCache(Country.class);
		countryCache.clear();
		
		Ebean.runCacheWarming(Country.class);
		Assert.assertTrue(countryCache.size() > 0);
		
		Country c0 = Ebean.getReference(Country.class, "NZ");
		ServerCacheStatistics statistics = countryCache.getStatistics(false);
		int hc = statistics.getHitCount();
		Assert.assertEquals(1, hc);

		Country c1 = Ebean.getReference(Country.class, "NZ");
		Assert.assertEquals(2, countryCache.getStatistics(false).getHitCount());
		Assert.assertEquals(100, countryCache.getStatistics(false).getHitRatio());

		// same instance as caching with readOnly=true
		Assert.assertTrue(c0 == c1);

		
		// reset the statistics
		Assert.assertEquals(2,countryCache.getStatistics(true).getHitCount());
		// now the count should be 0 again 
		Assert.assertEquals(0, countryCache.getStatistics(false).getHitCount());
		// and hitRatio is 0 as well
		Assert.assertEquals(0, countryCache.getStatistics(false).getHitRatio());

		// hit the country cache automatically via join
		Customer customer = Ebean.find(Customer.class, 1);
		Address billingAddress = customer.getBillingAddress();
		Country c2 = billingAddress.getCountry();

		Assert.assertEquals(1,countryCache.getStatistics(false).getHitCount());

		Country c3 = Ebean.getReference(Country.class, "NZ");
		Country c4 = Ebean.find(Country.class, "NZ");

		// all the same instance as caching with readOnly=true
		Assert.assertTrue(c2 == c3);
		Assert.assertTrue(c2 == c4);
		Assert.assertTrue(c2 == c0);


		// clear the cache
		countryCache.clear();
		// reset statistics
		countryCache.getStatistics(true);

		// try to hit the country cache automatically via join
		customer = Ebean.find(Customer.class, 1);
		billingAddress = customer.getBillingAddress();
		Country c5 = billingAddress.getCountry();
		// but cache is empty so c5 is reference that will load cache
		// if it is lazy loaded
		Assert.assertEquals("empty cache",0,countryCache.getStatistics(false).getSize());
		Assert.assertEquals("missCount 1",1,countryCache.getStatistics(false).getMissCount());

		// lazy load on c5 populates the cache
		c5.getName();
		Assert.assertEquals("cache populated via lazy load",1,countryCache.getStatistics(false).getSize());
		
		// now these get hits in the cache
		Country c6 = Ebean.find(Country.class, "NZ");
		Country c7 = Ebean.getReference(Country.class, "NZ");

		Assert.assertTrue("different instance as cache cleared",c2 != c5);
		Assert.assertTrue("these 2 are different",c5 != c6);
		Assert.assertTrue("but these 2 are the same",c6 == c7);
		
		// by default readOnly based on deployment annotation
		Assert.assertTrue("read only",Ebean.getBeanState(c6).isReadOnly());
		
		try {
			// can't modify a readOnly bean
			c6.setName("Nu Zilund");
			Assert.assertFalse("Never get here",true);
		} catch (IllegalStateException e){
			Assert.assertTrue("This is readOnly",true);
		}
		
		Country c8 = Ebean.find(Country.class)
			.setId("NZ")
			.setReadOnly(false)
			.findUnique();
		
		// Explicitly NOT readOnly 
		Assert.assertFalse("NOT read only",Ebean.getBeanState(c8).isReadOnly());
		
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
		Assert.assertFalse(Ebean.getBeanState(c9).isSharedInstance()); 
		Assert.assertFalse(Ebean.getBeanState(c9).isReadOnly()); 
		Assert.assertEquals("1 country in cache", 1, countryCache.size()); 

		Country c10 = Ebean.find(Country.class,"NZ");

		Assert.assertTrue(Ebean.getBeanState(c10).isSharedInstance()); 
		Assert.assertTrue(Ebean.getBeanState(c10).isReadOnly()); 
		Assert.assertEquals("1 country in cache", 1, countryCache.size()); 
		
		countryCache.clear();
		Assert.assertEquals("0 country in cache", 0, countryCache.size()); 

		// reference doesn't load cache yet
		Country c11 = Ebean.getReference(Country.class, "NZ");

		// still 0 in cache
		Assert.assertEquals("0 country in cache", 0, countryCache.size());

		// will invoke lazy loading..
		c11.getName();
		Assert.assertEquals("1 country in cache", 1, countryCache.size());

	}
}

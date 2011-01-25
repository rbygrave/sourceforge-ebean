package com.avaje.tests.cache;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.tests.model.basic.EBasicVer;

public class TestQueryCacheInsert extends TestCase {

	public void test() {

		EbeanServer server = Ebean.getServer(null);

		EBasicVer account = new EBasicVer();
		server.save(account);

		List<EBasicVer> accounts =
		        server.find(EBasicVer.class)
		                .setUseQueryCache(true)
		                .findList();

		int sizeOne = accounts.size();

		account = new EBasicVer();
		server.save(account);

		accounts = server.find(EBasicVer.class)
		        .setUseQueryCache(true)
		        .findList();

		List<EBasicVer> noQueryCacheList = server.find(EBasicVer.class)
	        .setUseQueryCache(false)
	        .findList();
		
		int sizeTwo = accounts.size();
		
		Assert.assertTrue(sizeOne != sizeTwo);
		Assert.assertTrue(sizeOne != noQueryCacheList.size());
	}
}

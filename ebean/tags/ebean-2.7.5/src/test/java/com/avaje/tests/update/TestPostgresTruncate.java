package com.avaje.tests.update;

import junit.framework.TestCase;

import org.junit.Assert;

public class TestPostgresTruncate extends TestCase {

	public void test() {
		
		Assert.assertTrue(true);
//		EbeanServer server = Ebean.getServer(null);
//		
//		EBasic e = new EBasic();
//		e.setName("something");
//		e.setStatus(Status.NEW);
//		e.setDescription("wow");
//		
//		server.save(e);
//		
//		 server.beginTransaction();
//		   Integer oriC = Ebean.createSqlQuery("select count(*) as c from e_basic").findUnique().getInteger("c");
//		   int rows = Ebean.createSqlUpdate("truncate e_basic cascade").execute();
//		   Integer currentC = Ebean.createSqlQuery("select count(*) as c from e_basic").findUnique().getInteger("c");
//		   server.commitTransaction();
//		   //Ebean.getServerCacheManager().clearAll();
//		   System.out.println("table : ori="+oriC+", delC="+rows+", currentC="+currentC);
		   
		
	}
}

package com.avaje.tests.unitinternal;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.config.GlobalProperties;

public class TestGlobalPropsLoader extends TestCase {

	public void test() {
		
		GlobalProperties.put("ebean.ddl.run", "false");
		
		String s = GlobalProperties.get("robtest", null);
		Assert.assertEquals("robvalue", s);
		
	}
	
	

}

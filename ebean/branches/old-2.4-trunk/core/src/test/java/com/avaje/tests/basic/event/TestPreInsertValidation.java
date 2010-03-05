package com.avaje.tests.basic.event;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.TWithPreInsert;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestPreInsertValidation extends TestCase {

	public void test() {
		
		TWithPreInsert e = new TWithPreInsert();
		
		// the perInsert should populate the
		// name with should not be null
		Ebean.save(e);
		
		// the save worked
		Assert.assertNotNull(e.getId());
		
	}
	
}

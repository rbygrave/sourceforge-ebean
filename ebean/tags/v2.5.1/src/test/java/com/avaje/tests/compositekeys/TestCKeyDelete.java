package com.avaje.tests.compositekeys;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.CKeyParent;
import com.avaje.tests.model.basic.CKeyParentId;

public class TestCKeyDelete extends TestCase {

	public void test() {
		
		CKeyParentId id = new CKeyParentId(100, "deleteMe");
        CKeyParentId searchId = new CKeyParentId(100, "deleteMe");
		
		CKeyParent p = new CKeyParent();
		p.setId(id);
		p.setName("testDelete");

		Ebean.save(p);

		CKeyParent found = Ebean.find(CKeyParent.class)
			.where().idEq(searchId)
			.findUnique();
		
		Assert.assertNotNull(found);

		Ebean.delete(CKeyParent.class, searchId);
		
		
        CKeyParent notFound = Ebean.find(CKeyParent.class)
            .where().idEq(searchId)
            .findUnique();
    
        Assert.assertNull(notFound);

		
	}
}

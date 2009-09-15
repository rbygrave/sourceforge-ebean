package com.avaje.tests.inheritance;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.TIntChild;
import com.avaje.tests.model.basic.TIntRoot;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestIntInherit extends TestCase {

	public void testMe() {

		TIntRoot r = new TIntRoot();
		r.setName("root1");
		
		TIntRoot r2 = new TIntRoot();
		r.setName("root2");
		
		TIntChild c1 = new TIntChild();
		c1.setName("child1");
		c1.setChildProperty("cp1");
		
		TIntChild c2 = new TIntChild();
		c2.setName("child2");
		c2.setChildProperty("cp2");
		
		
		Ebean.save(r);
		Ebean.save(r2);
		Ebean.save(c1);
		Ebean.save(c2);
		
		TIntRoot result1 = Ebean.find(TIntRoot.class, 1);
		Assert.assertTrue(result1 instanceof TIntRoot);
		
		TIntRoot ref3 = Ebean.getReference(TIntRoot.class, 3);
		Assert.assertTrue(ref3 instanceof TIntChild);
		
		TIntRoot result3 = Ebean.find(TIntRoot.class, 3);
		Assert.assertTrue(result3 instanceof TIntChild);
		
	}
	
}

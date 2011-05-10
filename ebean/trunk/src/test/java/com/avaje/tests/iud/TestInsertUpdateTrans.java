package com.avaje.tests.iud;

import java.sql.Timestamp;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.EBasicVer;

public class TestInsertUpdateTrans extends TestCase {

	public void test() {

		Ebean.beginTransaction();
		try {

			EBasicVer e0 = new EBasicVer();
			e0.setName("onInsert");
			e0.setDescription("something");
			Ebean.save(e0);

			// // only use the below test when not using enhancement
			// boolean entity = (e0 instanceof EntityBean);
			// Assert.assertTrue(!entity);

			Assert.assertNotNull(e0.getId());
			Assert.assertNotNull(e0.getLastUpdate());
			Timestamp lastUpdate0 = e0.getLastUpdate();
			
			
			e0.setName("onUpdate");
			e0.setDescription("differentFromInsert");
			
			Ebean.save(e0);
			
			EBasicVer e1 = Ebean.find(EBasicVer.class, e0.getId());
			
			Timestamp lastUpdate1 = e1.getLastUpdate();
			
			// we should fetch back the updated data (not inserted)
			Assert.assertEquals(e0.getId(), e1.getId());
			Assert.assertEquals("onUpdate", e1.getName());
			Assert.assertEquals("differentFromInsert", e1.getDescription());
			long diff = lastUpdate1.getTime() - lastUpdate0.getTime();
			Assert.assertTrue(diff > 0);
			
			
			Ebean.commitTransaction();
			
		} finally {
			Ebean.endTransaction();
		}
	}
}

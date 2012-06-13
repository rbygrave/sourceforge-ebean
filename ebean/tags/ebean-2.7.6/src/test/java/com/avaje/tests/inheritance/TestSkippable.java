package com.avaje.tests.inheritance;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.AttributeHolder;
import com.avaje.tests.model.basic.ListAttribute;
import com.avaje.tests.model.basic.ListAttributeValue;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestSkippable extends TestCase {



	/**
	 * Test query.
	 * <p>This test covers the BUG 276. Cascade was not propagating to the ListAttribute because
	 * it was considered safe to skip as it didn't take into account any derived classes 
	 * into account with e.g. collections and Cascade options </p>
	 */
	public void testQuery()
	{
		// Setup the data first
		final ListAttributeValue value1 = new ListAttributeValue();
		final ListAttributeValue value2 = new ListAttributeValue();
		
		Ebean.save(value1);
		Ebean.save(value2);
		
		final ListAttribute listAttribute = new ListAttribute();
		listAttribute.add(value1);
		Ebean.save(listAttribute);
		
		final ListAttribute listAttributeDB = Ebean.find(ListAttribute.class, listAttribute.getId());
		Assert.assertNotNull(listAttributeDB);

		final ListAttributeValue value1_DB = listAttributeDB.getValues().iterator().next();
		
		
		Assert.assertTrue(value1.getId().equals(value1_DB.getId()));
		
		
		final AttributeHolder holder = new AttributeHolder();
		holder.add(listAttributeDB);
		
		Ebean.save(holder);
		
		// Now change the M2M listAttribute.values and save the holder
		// The save should cascade as follows 
		// holder.attributes..ListAttribute.values
		listAttributeDB.getValues().clear();
		listAttributeDB.add(value2);
		
		// Save the holder - should cascade down to the listAtribute and save the values
		Ebean.save(holder);
		

		final ListAttribute listAttributeDB_2 = Ebean.find(ListAttribute.class, listAttributeDB.getId());
		Assert.assertNotNull(listAttributeDB_2);

		final ListAttributeValue value2_DB_2 = listAttributeDB_2.getValues().iterator().next();
		
		
		Assert.assertTrue("Cascade failed", value2.getId().equals(value2_DB_2.getId()));
		
	}
}

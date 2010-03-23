package com.avaje.tests.compositekeys;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.tests.compositekeys.db.Item;
import com.avaje.tests.compositekeys.db.ItemKey;
import com.avaje.tests.compositekeys.db.Region;
import com.avaje.tests.compositekeys.db.RegionKey;
import com.avaje.tests.compositekeys.db.SubType;
import com.avaje.tests.compositekeys.db.SubTypeKey;
import com.avaje.tests.compositekeys.db.Type;
import com.avaje.tests.compositekeys.db.TypeKey;
import com.avaje.tests.lib.EbeanTestCase;

import java.util.List;

/**
 * Test some of the Avaje core functionality in conjunction with composite keys like
 * <ul>
 * <li>write</li>
 * <li>find</li>
 * </ul>
 */
public class TestCore extends EbeanTestCase
{
	//private boolean setup;

    @Override
    public void setUp() throws Exception
    {
		Ebean.createUpdate(Item.class, "delete from Item").execute();
		Ebean.createUpdate(Region.class, "delete from Region").execute();
		Ebean.createUpdate(Type.class, "delete from Type").execute();
		Ebean.createUpdate(SubType.class, "delete from SubType").execute();

        Transaction tx = getServer().beginTransaction();

		SubType subType = new SubType();
		SubTypeKey subTypeKey = new SubTypeKey();
		subTypeKey.setSubTypeId(1);
		subType.setKey(subTypeKey);
		subType.setDescription("ANY SUBTYPE");
		getServer().save(subType);

        Type type = new Type();
        TypeKey typeKey = new TypeKey();
        typeKey.setCustomer(1);
        typeKey.setType(10);
        type.setKey(typeKey);
        type.setDescription("Type Old-Item - Customer 1");
		type.setSubType(subType);
        getServer().save(type);

        type = new Type();
        typeKey = new TypeKey();
        typeKey.setCustomer(2);
        typeKey.setType(10);
        type.setKey(typeKey);
        type.setDescription("Type Old-Item - Customer 2");
		type.setSubType(subType);
        getServer().save(type);

        Region region = new Region();
        RegionKey regionKey = new RegionKey();
        regionKey.setCustomer(1);
        regionKey.setType(500);
        region.setKey(regionKey);
        region.setDescription("Region West - Customer 1");
        getServer().save(region);

        region = new Region();
        regionKey = new RegionKey();
        regionKey.setCustomer(2);
        regionKey.setType(500);
        region.setKey(regionKey);
        region.setDescription("Region West - Customer 2");
        getServer().save(region);

        Item item = new Item();
        ItemKey itemKey = new ItemKey();
        itemKey.setCustomer(1);
        itemKey.setItemNumber("ITEM1");
        item.setKey(itemKey);
		item.setUnits("P");
        item.setDescription("Fancy Car - Customer 1");
        item.setRegion(500);
        item.setType(10);
        getServer().save(item);

        item = new Item();
        itemKey = new ItemKey();
        itemKey.setCustomer(2);
        itemKey.setItemNumber("ITEM1");
        item.setKey(itemKey);
		item.setUnits("P");
        item.setDescription("Another Fancy Car - Customer 2");
        item.setRegion(500);
        item.setType(10);
        getServer().save(item);

        tx.commit();
    }

    public void testFind()
    {
        List<Item> items = getServer().find(Item.class).findList();

        assertNotNull(items);
        assertEquals(2, items.size());

        Query<Item> qItems = getServer().find(Item.class);
//        qItems.where(Expr.eq("key.customer", Integer.valueOf(1)));
        
        // I want to discourage the direct use of Expr
        qItems.where().eq("key.customer", Integer.valueOf(1));
        items = qItems.findList();

        assertNotNull(items);
        assertEquals(1, items.size());
    }

	/**
	 * This partially loads the item and then lazy loads the ManyToOne assoc
	 */
	public void testDoubleLazyLoad()
	{
		ItemKey itemKey = new ItemKey();
		itemKey.setCustomer(2);
		itemKey.setItemNumber("ITEM1");
		
		Item item = getServer().find(Item.class).select("description").where().idEq(itemKey).findUnique();
		assertNotNull(item);
		assertNotNull(item.getUnits());
		assertEquals("P", item.getUnits());

		Type type = item.getEType();
		assertNotNull(type);
		assertNotNull(type.getDescription());

		SubType subType = type.getSubType();
		assertNotNull(subType);
		assertNotNull(subType.getDescription());
	}

	public void testEmbeddedWithOrder()
	{
		List<Item> items = getServer().find(Item.class).order("auditInfo.created asc, type asc").findList();

		assertNotNull(items);
		assertEquals(2, items.size());
	}
	
}
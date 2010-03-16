package com.avaje.tests.query;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.CKeyParent;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestSubQuery extends TestCase {

    public void testId() {
        
        ResetBasicData.reset();

        List<Integer> productIds = new ArrayList<Integer>();
        productIds.add(3);
        
        Query<Order> sq = Ebean.createQuery(Order.class)
            .select("id")
            .where().in("details.product.id", productIds)
            .query();
        
        List<Order> list = Ebean.find(Order.class)
            .where().in("id", sq)
            .findList();
        
        System.out.println(list);
        // FIXME: need to clear out old orders..
        //Assert.assertEquals(2,list.size());
        
        String oq = " find order (id, status) where id in "
            +"(select a.id from o_order a join o_order_detail ad on ad.order_id = a.id where ad.product_id in (:prods)) ";

        List<Order> list2 = Ebean.createQuery(Order.class, oq)
            .setParameter("prods", productIds)
            .findList();

        System.out.println(list2);
        //Assert.assertEquals(2,list2.size());

    }

	/*
	public void testCompositeKey()
	{
		ResetBasicData.reset();

		Query<CKeyParent> sq = Ebean.createQuery(CKeyParent.class)
			.select("id.oneKey")
			.where()
			.query();

		Query<CKeyParent> pq = Ebean.find(CKeyParent.class)
			.where().in("id.oneKey", sq)
			.query();

		pq.findList();
		
		String sql = pq.getGeneratedSql();

		String golden = "(c.one_key) in (select c.one_key c0 from ckey_parent c)";

		if (sql.indexOf(golden) < 0)
		{
			fail("golden string not found");
		}
	}
	*/
}

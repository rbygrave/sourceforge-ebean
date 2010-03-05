package com.avaje.tests.query;

import java.util.ArrayList;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestSubQuery extends TestCase {

    public void test() {
        
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
}

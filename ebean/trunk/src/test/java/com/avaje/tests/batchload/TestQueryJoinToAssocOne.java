package com.avaje.tests.batchload;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.JoinConfig;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryJoinToAssocOne extends TestCase {

    public void testLazyOnNonLoaded() {

        ResetBasicData.reset();

        // This will use 3 SQL queries to build this object graph
        List<Order> l0 = Ebean.find(Order.class)
           .select("status, shipDate")
           .join("details", "orderQty, unitPrice", new JoinConfig().query())
           .join("details.product", "sku, name")
           
           //.join("customer", "name", new JoinConfig().query(10))
           //.join("customer.contacts","firstName, lastName, mobile")
           //.join("customer.shippingAddress","line1, city")
           .findList();
        
        Assert.assertTrue(l0.size() > 0);
        
        
    }
}

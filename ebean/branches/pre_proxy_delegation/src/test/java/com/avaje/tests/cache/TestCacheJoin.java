package com.avaje.tests.cache;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestCacheJoin {

    @Test
    public void test() {
        
        ResetBasicData.reset();
        
        List<Customer> list = Ebean.find(Customer.class)
            .setUseCache(true)
            .setLoadBeanCache(true)
            .setReadOnly(true)
            .orderBy("id")
            .findList();
        
        Assert.assertTrue(list.size() > 0);
        
        Customer c = list.get(0);
        
        Customer c2 = Ebean.find(Customer.class)
            .setId(c.getId())
            .setUseCache(true)
            .setReadOnly(true)
            .findUnique();
            
        Assert.assertTrue(c == c2);
        
        List<Order> orders = Ebean.find(Order.class)
            .join("customer","+cache +readonly")
            .where().eq("customer.id", c.getId())
            .findList();
        
        Customer c3 = orders.get(0).getCustomer();
        
        Assert.assertTrue(c3 == c);
        
        
    }
}

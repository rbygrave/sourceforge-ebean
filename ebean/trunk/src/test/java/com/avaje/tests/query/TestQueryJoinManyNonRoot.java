package com.avaje.tests.query;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryJoinManyNonRoot extends TestCase {

    public void test_manyNonRoot() {
        
        ResetBasicData.reset();
        
        Query<Order> q = Ebean.find(Order.class)
            .join("customer")
            .join("customer.contacts")
            .where().gt("id", 0)
            .query();

        List<Order> list = q.findList();
        String sql = q.getGeneratedSql();
        
        Assert.assertTrue(list.size() > 0);
        Assert.assertTrue(sql.contains("join o_customer oc on oc.id "));
        Assert.assertTrue(sql.contains("left outer join contact occ on"));
    
    }

    public void test_manyRootQueryJoinOrderDetailsFirst() {
        
        ResetBasicData.reset();
        
        Query<Order> q = Ebean.find(Order.class)
            .join("details")
            .join("details.product")
            .join("customer")
            .join("customer.contacts")
            .where().gt("id", 0)
            .query();

        List<Order> list = q.findList();
        String sql = q.getGeneratedSql();
        
        Assert.assertTrue(list.size() > 0);
        Assert.assertTrue(sql.contains("join o_customer oc on oc.id "));
        Assert.assertTrue(sql.contains("left outer join o_order_detail "));
        Assert.assertTrue(sql.contains("left outer join o_product "));
                
        Assert.assertFalse(sql.contains("left outer join contact occ on"));
    
    }
    
    
    public void test_manyRootQueryJoinCustomerContactsFirst() {
        
        ResetBasicData.reset();
        
        Query<Order> q = Ebean.find(Order.class)
            .join("customer")
            .join("customer.contacts")
            .join("details")
            .join("details.product")
            .where().gt("id", 0)
            .query();

        List<Order> list = q.findList();
        String sql = q.getGeneratedSql();
        
        Assert.assertTrue(list.size() > 0);
        Assert.assertTrue(sql.contains("join o_customer oc on oc.id "));
        Assert.assertTrue(sql.contains("left outer join contact occ on"));

        Assert.assertFalse(sql.contains("left outer join o_order_detail "));
        Assert.assertFalse(sql.contains("left outer join o_product "));
                
    
    }

}

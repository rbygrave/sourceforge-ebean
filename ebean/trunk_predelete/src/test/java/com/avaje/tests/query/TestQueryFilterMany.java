package com.avaje.tests.query;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.JoinConfig;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryFilterMany extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        Query<Customer> query = Ebean.find(Customer.class)
            .join("orders", new JoinConfig().lazy())
            .where().ilike("name","Rob%")
            .filterMany("orders").eq("status", Order.Status.NEW)
            .where().gt("id", 0)
            .query();
        
        //query.filterMany("orders").eq("status", Order.Status.NEW);
            
        List<Customer> list = query.findList();
        for (Customer customer : list) {
            customer.getOrders().size();
        }
        
        Customer c0 = list.get(0);
        System.out.println("......... refreshMany ...");
        Ebean.refreshMany(c0, "orders");
        
    }
}

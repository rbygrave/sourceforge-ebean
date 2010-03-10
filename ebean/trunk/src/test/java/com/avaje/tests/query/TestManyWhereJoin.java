package com.avaje.tests.query;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestManyWhereJoin extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        Query<Customer> query = Ebean.find(Customer.class)
            .select("id,status")
            .join("orders")
            // the where on a 'many' (like orders) requires an 
            // additional join and distinct which is independent
            // of a fetch join (if there is a fetch join) 
            .where().gt("orders.id", 3)
            .query();
        
        query.findList();
        
    }
}

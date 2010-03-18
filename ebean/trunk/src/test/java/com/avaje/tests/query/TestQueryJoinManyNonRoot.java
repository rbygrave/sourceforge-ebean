package com.avaje.tests.query;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Contact;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryJoinManyNonRoot extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        List<Order> list = Ebean.find(Order.class)
            .join("customer")
            .join("customer.contacts")
            .where().gt("id", 0)
            .findList();

        Assert.assertNotNull(list);
        System.out.println(list);
      
        for (Order order : list) {
            List<Contact> contacts = order.getCustomer().getContacts();
            contacts.size();
        }
  
    
//    String oq = "find order join customer join customer.contacts join details (+query(4),+lazy(5))";
//    Query<Order> q = Ebean.createQuery(Order.class, oq);
//    q.findList();
//    String sql = q.getGeneratedSql();
    
    }
}

package com.avaje.tests.query;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.JoinConfig;
import com.avaje.ebean.Query;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.tests.model.basic.Contact;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryJoinQueryNonRoot extends TestCase {

    public void test() {
        
        
        SpiEbeanServer server = (SpiEbeanServer)Ebean.getServer(null);
        BeanDescriptor<Order> d = server.getBeanDescriptor(Order.class);
        ElPropertyValue elGetValue = d.getElGetValue("customer.contacts");
        
        Assert.assertTrue(elGetValue.containsMany());
        
        
        ResetBasicData.reset();
        
        List<Order> list = Ebean.find(Order.class)
            .join("customer")
            .join("customer.contacts", new JoinConfig().query().lazy(10))
            .where().lt("id", 3)
            .findList();

        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() > 0);
      
        for (Order order : list) {
            List<Contact> contacts = order.getCustomer().getContacts();
            for (Contact contact : contacts) {
                contact.getFirstName();
            }
        }
  
    
        String oq = "find order join customer join customer.contacts join details (+query(4),+lazy(5))";
        Query<Order> q = Ebean.createQuery(Order.class, oq);
        q.setAutofetch(false);
        List<Order> list2 = q.findList();
        
        Assert.assertTrue(list2.size() > 0);
     
    }
}

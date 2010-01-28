package com.avaje.tests.basic.vanilla;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestVanillaQuery extends TestCase {

    public void test() {
        
        GlobalProperties.put("ebean.vanillaMode", "true");
        
        ResetBasicData.reset();
        
        List<Order> list = 
            Ebean.find(Order.class)
            .join("details")
            //.setVanillaMode(true)
            .findList();
        
        Assert.assertTrue(list.size() > 0);
        
        Order o = list.get(0);
        
        // actually only a vanilla class when using subclass generation 
        Class<?> vanillaClass = Order.class;
        Class<?> returnedClass = o.getClass();
        
        Assert.assertEquals(vanillaClass, returnedClass);

        Ebean.refreshMany(o, "details");
        
        Ebean.refresh(o);
        
        if (!(o instanceof EntityBean)){
            // using subclass generation ...
            list = 
                Ebean.find(Order.class)
                .setVanillaMode(false)
                .findList();
    
            Class<?> entityBeanClass = list.get(0).getClass();
            
            Assert.assertNotSame(vanillaClass, entityBeanClass);
        }
    }
}

package com.avaje.tests.batchload;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestLoadOnDirty extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        List<Customer> custs = Ebean.find(Customer.class).findList();
        
        Customer customer = Ebean.find(Customer.class)
            .setId(custs.get(0).getId())
            .select("name")
            .findUnique();
        
        BeanState beanState = Ebean.getBeanState(customer);
        Assert.assertTrue(!beanState.isNew());
        Assert.assertTrue(!beanState.isDirty());
        Assert.assertTrue(!beanState.isNewOrDirty());
        Assert.assertNotNull(beanState.getLoadedProps());
        
        customer.setName("dirtyNameProp");
        Assert.assertTrue(beanState.isDirty());
        Assert.assertTrue(beanState.getChangedProps().contains("name"));
        
        customer.setStatus(Customer.Status.INACTIVE);
        
        Assert.assertTrue(beanState.isDirty());
        Assert.assertTrue(beanState.getChangedProps().contains("state"));
        Assert.assertTrue(beanState.getChangedProps().contains("name"));
        
    }
}

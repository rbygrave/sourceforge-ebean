package com.avaje.tests.basic;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.Customer;

public class TestTransient extends TestCase {

    public void testTransient() {

        Customer cnew = new Customer();
        cnew.setName("testTrans");

        Ebean.save(cnew);
        Integer custId = cnew.getId();

        Customer c = Ebean.find(Customer.class).setAutofetch(false).setId(custId).findUnique();

        Assert.assertNotNull(c);

        BeanState beanState = Ebean.getBeanState(c);
        Assert.assertFalse("not new or dirty as transient", beanState.isNewOrDirty());

        c.getLock().tryLock();
        try {
            c.setSelected(Boolean.TRUE);
        } finally {
            c.getLock().unlock();
        }

        Boolean selected = c.getSelected();
        Assert.assertNotNull(selected);

        Assert.assertFalse("not new or dirty as transient", beanState.isNewOrDirty());

        Ebean.save(c);

        selected = c.getSelected();
        Assert.assertNotNull(selected);

        c.setName("Modified");
        Assert.assertTrue("dirty now", beanState.isNewOrDirty());

        selected = c.getSelected();
        Assert.assertNotNull(selected);

        Ebean.save(c);
        Assert.assertFalse("Not dirty after save", beanState.isNewOrDirty());

        selected = c.getSelected();
        Assert.assertNotNull(selected);

        String updateStmt = "update customer set name = 'Rob' where id = :id";
        int rows = Ebean.createUpdate(Customer.class, updateStmt).set("id", custId).execute();

        Assert.assertTrue("changed name back", 1 == rows);
    }
}

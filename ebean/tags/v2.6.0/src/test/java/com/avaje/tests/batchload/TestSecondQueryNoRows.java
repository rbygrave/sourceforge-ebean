package com.avaje.tests.batchload;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.JoinConfig;
import com.avaje.tests.model.basic.Customer;

public class TestSecondQueryNoRows extends TestCase {

    public void test() {

        Customer cnew = new Customer();
        cnew.setName("testSecQueryNoRows");

        Ebean.save(cnew);

        Customer c = Ebean.find(Customer.class)
            .setAutofetch(false)
            .setId(cnew.getId())
            .join("contacts", new JoinConfig().query())
            .findUnique();

        Assert.assertNotNull(c);
    }
}

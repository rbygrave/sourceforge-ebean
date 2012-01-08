package com.avaje.tests.batchload;

import com.avaje.ebean.Ebean;
import com.avaje.tests.basic.MyTestDataSourcePoolListener;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestBasicLazy extends TestCase {

	public void testQueries() {

		ResetBasicData.reset();

		Order order = Ebean.find(Order.class)
			.select("totalAmount")
			.setMaxRows(1)
			.order("id")
			.findUnique();

		Assert.assertNotNull(order);

		Customer customer = order.getCustomer();
		Assert.assertNotNull(customer);
		Assert.assertNotNull(customer.getName());

		Address address = customer.getBillingAddress();
		Assert.assertNotNull(address);
		Assert.assertNotNull(address.getCity());
	}

    public void testRaceCondition() throws Throwable
    {

   		ResetBasicData.reset();

   		Order order = Ebean.find(Order.class)
   			.select("totalAmount")
   			.setMaxRows(1)
   			.order("id")
   			.findUnique();

   		Assert.assertNotNull(order);

   		final Customer customer = order.getCustomer();
   		Assert.assertNotNull(customer);
        
        Assert.assertTrue(Ebean.getBeanState(customer).isReference());

        final Throwable throwables[] = new Throwable[2];
        Thread t1 = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Assert.assertNotNull(customer.getName());
                }
                catch (Throwable e)
                {
                    throwables[0]=e;
                }
            }
        };

        Thread t2=new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Assert.assertNotNull(customer.getName());
                }
                catch (Throwable e)
                {
                    throwables[1]=e;
                }
            }
        };

        try
        {
            // prepare for race condition
            MyTestDataSourcePoolListener.SLEEP_AFTER_BORROW=2000;

            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        finally
        {
            MyTestDataSourcePoolListener.SLEEP_AFTER_BORROW=0;
        }

        Assert.assertFalse(Ebean.getBeanState(customer).isReference());

        if (throwables[0] != null)
        {
            throw throwables[0];
        }
        if (throwables[1] != null)
        {
            throw throwables[1];
        }
   	}
}
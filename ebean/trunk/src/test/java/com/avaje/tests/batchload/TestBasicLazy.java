package com.avaje.tests.batchload;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.FetchConfig;
import com.avaje.tests.basic.MyTestDataSourcePoolListener;
import com.avaje.tests.model.basic.Address;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestBasicLazy extends TestCase
{

    public void testQueries()
    {

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

    public void testRaceCondition_Simple() throws Throwable
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
                    throwables[0] = e;
                }
            }
        };

        Thread t2 = new Thread()
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
                    throwables[1] = e;
                }
            }
        };

        try
        {
            // prepare for race condition
            MyTestDataSourcePoolListener.SLEEP_AFTER_BORROW = 2000;

            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        finally
        {
            MyTestDataSourcePoolListener.SLEEP_AFTER_BORROW = 0;
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

    private final Object mutex = new Object();
    private List<Order> orders;
    
    private List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

    private class FetchThread extends Thread
    {
        private int index;

        private FetchThread(ThreadGroup tg, int index)
        {
            super(tg, "fetcher-" + index);
            this.index = index;
        }

        @Override
        public void run()
        {
            synchronized (mutex)
            {
                System.err.println("** WAIT **");
                try
                {
                    mutex.wait();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }

            try
            {
                orders.get(index).getCustomer().getName();
            }
            catch (Throwable e)
            {
                exceptions.add(e);
            }
        }
    }

    public void testRaceCondition_Complex() throws Throwable
    {
        ResetBasicData.reset();

        ThreadGroup tg = new ThreadGroup("fetchers");
        new FetchThread(tg, 0).start();
        new FetchThread(tg, 1).start();
        new FetchThread(tg, 2).start();
        new FetchThread(tg, 3).start();
        new FetchThread(tg, 0).start();
        new FetchThread(tg, 1).start();
        new FetchThread(tg, 2).start();
        new FetchThread(tg, 3).start();

        orders = Ebean.find(Order.class)
            .fetch("customer", new FetchConfig().lazy(100))
            .findList();
        assertTrue(orders.size() >= 4);

        synchronized (mutex)
        {
            try
            {
                MyTestDataSourcePoolListener.SLEEP_AFTER_BORROW = 2000;

                mutex.notifyAll();
            }
            finally
            {
                MyTestDataSourcePoolListener.SLEEP_AFTER_BORROW = 0;
            }
        }

        while(tg.activeCount() > 0)
        {
            Thread.sleep(100);
        }

        if (exceptions.size() > 0)
        {
            System.err.println("Seen Exceptions:");
            for (Throwable exception : exceptions)
            {
                exception.printStackTrace();
            }
            Assert.fail();
        }
    }
}
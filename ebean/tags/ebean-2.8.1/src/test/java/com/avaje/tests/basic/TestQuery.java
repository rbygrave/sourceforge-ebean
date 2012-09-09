package com.avaje.tests.basic;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;
import junit.framework.Assert;
import junit.framework.TestCase;

public class TestQuery extends TestCase
{

    public void testCountOrderBy()
    {

        ResetBasicData.reset();

        Query<Order> query = Ebean.find(Order.class)
            .setAutofetch(false)
            .order().asc("orderDate")
            .order().desc("id");
        //.orderBy("orderDate");

        int rc = query.findList().size();
        //int rc = query.findRowCount();
        Assert.assertTrue(rc > 0);
        //String generatedSql = query.getGeneratedSql();
        //Assert.assertFalse(generatedSql.contains("order by"));

    }

    public void testForUpdate()
    {
        ResetBasicData.reset();

        Query<Order> query = Ebean.find(Order.class)
            .setAutofetch(false)
            .setForUpdate(false)
            .setMaxRows(1)
            .order().asc("orderDate")
            .order().desc("id");
        
        int rc = query.findList().size();
        Assert.assertTrue(rc > 0);
        assertTrue(query.getGeneratedSql().toLowerCase().indexOf("for update") < 0);

        query = Ebean.find(Order.class)
            .setAutofetch(false)
            .setForUpdate(true)
            .setMaxRows(1)
            .order().asc("orderDate")
            .order().desc("id");

        rc = query.findList().size();
        Assert.assertTrue(rc > 0);
        assertTrue(query.getGeneratedSql().toLowerCase().indexOf("for update") > -1);
    }
}

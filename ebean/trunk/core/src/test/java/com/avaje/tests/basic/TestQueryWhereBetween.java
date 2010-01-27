package com.avaje.tests.basic;

import java.sql.Timestamp;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestQueryWhereBetween extends TestCase {

    public void testCountOrderBy() {

        ResetBasicData.reset();

        Timestamp t = new Timestamp(System.currentTimeMillis());

        Query<Order> query = Ebean.find(Order.class)
            .setAutofetch(false)
            .where().betweenProperties("cretime","updtime", t)
            .order().asc("orderDate")
            .order().desc("id");

        int rc = query.findList().size();

        String sql = query.getGeneratedSql();
        Assert.assertTrue(sql.indexOf("between o.cretime and o.updtime") > -1);

        Assert.assertTrue(rc == 0);
    }
}

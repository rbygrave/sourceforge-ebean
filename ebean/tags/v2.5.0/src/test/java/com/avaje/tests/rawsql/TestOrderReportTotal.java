package com.avaje.tests.rawsql;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.OrderReport;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestOrderReportTotal extends TestCase {

    public void test() {
        
        ResetBasicData.reset();
        
        Query<OrderReport> query = Ebean.createNamedQuery(OrderReport.class, "total.qty");
        
        List<OrderReport> list = query.findList();
        assertNotNull(list);
        
        Query<OrderReport> q2 = Ebean.createNamedQuery(OrderReport.class, "total.qty");
        q2.where().gt("id", 1);
        q2.having().gt("totalItems", 1);
        
        List<OrderReport> l2 = q2.findList();
        assertNotNull(l2);
        
        
        
    }
}

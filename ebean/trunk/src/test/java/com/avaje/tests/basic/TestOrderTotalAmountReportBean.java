package com.avaje.tests.basic;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.OrderReport;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestOrderTotalAmountReportBean extends TestCase {

    public void test() {

        ResetBasicData.reset();

		List<OrderReport> l0 = 
		     Ebean.createQuery(OrderReport.class)
		     .findList();
		
		for (OrderReport r0 : l0) {
            System.out.println(r0);			
		}
		     
		List<OrderReport> list = 
		     Ebean.createNamedQuery(OrderReport.class,"total.only")
		         .where().gt("orderId", 0)
		         .having().gt("totalAmount", 50)
		         .findList();
        
        for (OrderReport r1 : list) {
            System.out.println(r1);
            Assert.assertTrue(r1.getTotalAmount() > 20.50);
            // partial object query without totalItems
            // ... no lazy loading invoked on this type of bean
            Assert.assertTrue(r1.getTotalItems() == null);
        }
        
        
        List<OrderReport> l2 = 
            Ebean.createQuery(OrderReport.class)
		        .where().gt("orderId", 0)
                .having().lt("totalItems", 3).gt("totalAmount", 50)
                .findList();
        
        for (OrderReport r2 : l2) {
            //System.out.println(r2);
            Assert.assertTrue(r2.getTotalItems() < 3);
		}
        
    }

}

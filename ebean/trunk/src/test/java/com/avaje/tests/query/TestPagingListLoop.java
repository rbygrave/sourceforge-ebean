package com.avaje.tests.query;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestPagingListLoop extends TestCase {

    public void test() {
  
        boolean autoRunTest = false;
        
        if (!autoRunTest){
            // we only want to run this test manually.
            return;
        }
        
//        try {
            ResetBasicData.reset();
            
            for (int i = 0; i < 50; i++) {
                //Ebean.find(Customer.class).findPagingList(10);
                createLeak();
            }
                        
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//        
//        System.out.println("getting busy connections ... ");
//        
//        // anything open now is a leak
//        DataSourceGlobalManager.getDataSource("h2").closeBusyConnections(0);
        
    }
    
    private void createLeak() {
        
        // create a transaction we never close ...
        // ... a connection pool leak
        Ebean.getServer(null).createTransaction();
        
    }
    
}

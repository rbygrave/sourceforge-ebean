package com.avaje.tests.basic;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Transaction;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.EBasicVer;
import com.avaje.tests.model.basic.Order;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestLogTransLogOnError extends TestCase {

    public void testQueryError() {
        
        ResetBasicData.reset();
        
        Transaction t  = Ebean.beginTransaction();
        try {
            t.log("--- hello");
            Ebean.find(Customer.class).findList();
            Ebean.find(Order.class).where().gt("id", 1).findList();
            
            EBasicVer newBean = new EBasicVer();
            newBean.setDescription("something");
            newBean.setName("aName");
            
            //Ebean.save(newBean);
            
            t.log("--- next query should error");
            Ebean.find(Customer.class)
                .where().gt("id", "NotAnInt!!")
                .findList();
            
            // never get here
            Assert.assertTrue(false);
            
        } catch (RuntimeException e){
            //e.printStackTrace();
            Assert.assertTrue(true);
            
        } finally {
            Ebean.endTransaction();
        }
    }
    
    public void testPersistError() {
        
        ResetBasicData.reset();
        
        Transaction t  = Ebean.beginTransaction();
        try {
            t.log("--- hello testPersistError");
            Ebean.find(Customer.class).findList();
            
            EBasicVer newBean = new EBasicVer();
            newBean.setDescription("something sdfjksdjflsjdflsjdflksjdfkjd fsjdfkjsdkfjsdkfjskdjfskjdf"
                    +" sjdf sdjflksjdfkjsdlfkjsdkfjs ksjdfksjdlfjsldf something sdfjksdjflsjdflsjdflksjdfkjd"
                    +"fsjdfkjsdkfjsdkfjskdjfskjdf sjdf sdjflksjdfkjsdlfkjsdkfjs ksjdfksjdlfjsldf something s"
                    +"dfjksdjflsjdflsjdflksjdfkjd fsjdfkjsdkfjsdkfjskdjfskjdf sjdf sdjflksjdfkjsdlfkjsdkfjs ");
            newBean.setName("aName");
            
            t.log("--- next insert should error");
            Ebean.save(newBean);
                        
            // never get here
            Assert.assertTrue(false);
            
        } catch (RuntimeException e){
            //e.printStackTrace();
            Assert.assertTrue(true);
            
        } finally {
            Ebean.endTransaction();
        }
        
        
    }
}

package com.avaje.tests.basic;

import java.sql.Timestamp;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.EBasicVer;

public class TestIUDVanilla extends TestCase {

    public void test() {
        
        EBasicVer e0 = new EBasicVer();
        e0.setName("vanilla");
        
        Ebean.save(e0);
      
//        // only use the below test when not using enhancement
//        boolean entity = (e0 instanceof EntityBean);
//        Assert.assertTrue(!entity);
        
        Assert.assertNotNull(e0.getId());
        Assert.assertNotNull(e0.getLastUpdate());
        
        Timestamp lastUpdate0 = e0.getLastUpdate();
        
        e0.setName("modified");
        Ebean.save(e0);
        
        Timestamp lastUpdate1 = e0.getLastUpdate();
        Assert.assertNotNull(lastUpdate1);
        Assert.assertNotSame(lastUpdate0, lastUpdate1);
        
    }
}

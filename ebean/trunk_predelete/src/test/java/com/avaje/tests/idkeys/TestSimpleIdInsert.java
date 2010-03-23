package com.avaje.tests.idkeys;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.tests.model.basic.ESimple;

public class TestSimpleIdInsert extends TestCase {

    
    public void test() {
        
        
        GlobalProperties.put("datasource.default", "h2");
        GlobalProperties.put("ebean.classes", ESimple.class.getName());
        
        ESimple e = new ESimple();
        e.setName("name");
        
        Ebean.save(e);
        
        Assert.assertNotNull(e.getId());
        
        
    }
}

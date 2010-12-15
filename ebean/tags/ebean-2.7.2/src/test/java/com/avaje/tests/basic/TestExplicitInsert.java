package com.avaje.tests.basic;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.tests.model.basic.EBasic;

public class TestExplicitInsert extends TestCase {

    public void test() {
        
        EBasic b = new EBasic();
        b.setName("exp insert");
        b.setDescription("explicit insert");
        b.setStatus(EBasic.Status.ACTIVE);
        
        EbeanServer server = Ebean.getServer(null);
        server.insert(b);
        
        assertNotNull(b.getId());
     
        EBasic b2 = server.find(EBasic.class, b.getId());
        b2.setId(null);
        
        b2.setName("force insert");
        server.insert(b2);
        
        assertNotNull(b2.getId());
        assertTrue(!b.getId().equals(b2.getId()));
        
        
    }
    
}

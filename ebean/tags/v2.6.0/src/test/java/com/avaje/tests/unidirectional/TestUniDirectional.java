package com.avaje.tests.unidirectional;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.UTDetail;
import com.avaje.tests.model.basic.UTMaster;

public class TestUniDirectional extends TestCase {

    public void test() {
        
        UTMaster m = new UTMaster();
        m.setName("mast1");
        m.addDetail(new UTDetail("d11", 10, 20.50));
        m.addDetail(new UTDetail("d12", 3, 5.50));
        m.addDetail(new UTDetail("d13", 5, 1.0));
        
        Ebean.save(m);
        
        Assert.assertNotNull(m.getId());
        Assert.assertNotNull(m.getVersion());
        
        List<UTDetail> details = m.getDetails();
        for (UTDetail utDetail : details) {
            Assert.assertNotNull(utDetail.getId());
            Assert.assertNotNull(utDetail.getVersion());
        }
        
        UTDetail d4 = new UTDetail("d14", 2, 3.0);
        m.addDetail(d4);
        
        Ebean.save(m);

        Assert.assertNotNull(d4.getId());
        Integer d4ver = d4.getVersion();
        Assert.assertNotNull(d4ver);

        d4.setName("d14Upd");
        Ebean.save(d4);
        Integer d4ver2 = d4.getVersion();
        Assert.assertNotNull(d4ver2);
        Assert.assertEquals((d4ver + 1), d4ver2.intValue());
        
        Ebean.delete(d4);
    }
}

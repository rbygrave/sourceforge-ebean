package com.avaje.tests.unitinternal;

import java.sql.Timestamp;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.config.EncryptKey;
import com.avaje.ebeaninternal.server.type.SimpleAesEncryptor;
import com.avaje.tests.basic.encrypt.BasicEncryptKey;

public class TestSimpleEncryptor extends TestCase {

    
    public void test() {
        
        SimpleAesEncryptor e = new SimpleAesEncryptor();
        
        EncryptKey key = new BasicEncryptKey("hello");
        
        byte[] data = "test123".getBytes();
        
        byte[] ecData = e.encrypt(data, key);
        System.out.println(ecData);
 
        byte[] deData = e.decrypt(ecData, key);
     
        String s  = new String(deData);
        
        System.out.println(s);
        
        Timestamp t = new Timestamp(System.currentTimeMillis());
        byte[] ecTimestamp = e.encryptString(t.toString(), key);
        System.out.println(t+" encrypted -> "+ecTimestamp);
        
        String tsFormat = e.decryptString(ecTimestamp, key);
        Timestamp t1 = Timestamp.valueOf(tsFormat);
        Assert.assertEquals(t, t1);
        
    }
}

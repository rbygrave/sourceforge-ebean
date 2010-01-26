package com.avaje.tests.unitinternal;

import java.sql.Timestamp;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.config.EncryptKey;
import com.avaje.ebean.server.type.DefaultTypeManager;
import com.avaje.ebean.server.type.ScalarTypeTimestamp;
import com.avaje.ebean.server.type.SimpleAesEncryptor;
import com.avaje.tests.basic.encrypt.BasicEncryptKey;

public class TestSimpleEncryptor extends TestCase {

    
    public void test() {
        
        SimpleAesEncryptor e = new SimpleAesEncryptor(new DefaultTypeManager(null, null));
        e.addParser(Timestamp.class, new ScalarTypeTimestamp());
        
        EncryptKey key = new BasicEncryptKey("hello");
        
        byte[] data = "test123".getBytes();
        
        byte[] ecData = e.encrypt(data, key);
        System.out.println(ecData);
 
        byte[] deData = e.decrypt(ecData, key);
     
        String s  = new String(deData);
        
        System.out.println(s);
        
        Timestamp t = new Timestamp(System.currentTimeMillis());
        byte[] ecTimestamp = e.encryptObject(t, key);
        System.out.println(t+" encrypted -> "+ecTimestamp);
        
        Timestamp t1 = e.decryptObject(ecTimestamp, key, Timestamp.class);
        
        Assert.assertEquals(t, t1);
        
    }
}

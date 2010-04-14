package com.avaje.ebeaninternal.server.lib.sql;

import junit.framework.TestCase;

import org.junit.Assert;

public class TestArrayBuffer extends TestCase {

    public void test() {
        
        ArrayBuffer<String> b = new ArrayBuffer<String>(4);
        b.add("1");
        b.add("2");
        b.add("3");
        Assert.assertEquals(3, b.size());
        
        String s = b.remove();
        Assert.assertEquals("3", s);
        s = b.remove();
        Assert.assertEquals("2", s);
        s = b.remove();
        Assert.assertEquals("1", s);
        Assert.assertEquals(0, b.size());

        b.add("1");
        b.add("2");
        b.add("3");
        b.add("4");
        b.add("5");
        Assert.assertEquals(5, b.size());
        s = b.remove();
        Assert.assertEquals("5", s);
        Assert.assertEquals(4, b.size());

    
    }
    
}

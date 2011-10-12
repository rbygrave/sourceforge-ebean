package com.avaje.ebean.jaxrs;

import junit.framework.TestCase;

public class TestSplitUriOptions extends TestCase {

    public void test() {
        
        String[] s = SplitUriOptions.split(":(a,b(d))");
        
        assertEquals(1,s.length);
        assertEquals("(a,b(d))", s[0]);
        
        s = SplitUriOptions.split(":(a,b(d)):sort(a desc, b asc)");

        assertEquals(2,s.length);
        assertEquals("(a,b(d))", s[0]);
        assertEquals("sort(a desc, b asc)", s[1]);

        s = SplitUriOptions.split(":(a,b(d)):sort(a desc, b asc)::(1,2,3)");

        assertEquals(3,s.length);
        assertEquals("(a,b(d))", s[0]);
        assertEquals("sort(a desc, b asc)", s[1]);
        assertEquals(":(1,2,3)", s[2]);

        s = SplitUriOptions.split("::(1,2,3):(a,b(d)):sort(a desc, b asc)");

        assertEquals(3,s.length);
        assertEquals(":(1,2,3)", s[0]);
        assertEquals("(a,b(d))", s[1]);
        assertEquals("sort(a desc, b asc)", s[2]);

    }
}

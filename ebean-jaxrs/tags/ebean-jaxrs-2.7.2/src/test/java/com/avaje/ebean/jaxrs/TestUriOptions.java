package com.avaje.ebean.jaxrs;

import junit.framework.TestCase;

public class TestUriOptions extends TestCase {

    public void test() {
        
        UriOptions u  = UriOptions.parse("::(1,2,3)");
        assertEquals(3, u.getIdList().size());
        assertEquals("1", u.getIdList().get(0));
        assertEquals("2", u.getIdList().get(1));
        assertEquals("3", u.getIdList().get(2));
        assertNull(u.getUnknownSegments());
        assertNull(u.getPathProperties());
        assertNull(u.getSort());
        
        u  = UriOptions.parse("::(1,2,3):(a,b,c(d,e)):sort(a desc,b asc)");
        assertEquals(3, u.getIdList().size());
        assertEquals("1", u.getIdList().get(0));
        assertEquals("2", u.getIdList().get(1));
        assertEquals("3", u.getIdList().get(2));
        assertNull(u.getUnknownSegments());
        assertEquals(2,u.getPathProperties().getPaths().size());
        assertTrue(u.getPathProperties().getPaths().contains(null));
        assertTrue(u.getPathProperties().getPaths().contains("c"));
        assertEquals("a desc,b asc",u.getSort());
        
    }
}

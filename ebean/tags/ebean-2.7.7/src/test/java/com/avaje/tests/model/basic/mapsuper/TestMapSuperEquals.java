package com.avaje.tests.model.basic.mapsuper;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.bean.EntityBean;

public class TestMapSuperEquals extends TestCase {

  public void testEquals() {
    
    MapSuperActual a = new MapSuperActual();
    
    if (a instanceof EntityBean) {
      // test on enhanced beans only

      MapSuperActual b = new MapSuperActual();
      b.setId(456l);

      MapSuperActual c = new MapSuperActual();
      c.setId(2l);
      
      a.setId(456l);
      
      Assert.assertTrue("equals By Id value on enhanced mapped super",a.equals(b));
      Assert.assertTrue(b.equals(a));
      Assert.assertTrue(!a.equals(c));
      Assert.assertTrue(!b.equals(c));
      
    } else {
      System.out.println("--- ok, not running TestMapSuperEquals test");
    }
    
  }
  
}

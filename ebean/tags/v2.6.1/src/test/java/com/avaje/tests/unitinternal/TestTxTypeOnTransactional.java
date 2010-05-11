package com.avaje.tests.unitinternal;

import junit.framework.TestCase;

import com.avaje.tests.model.basic.xtra.DummyDao;

public class TestTxTypeOnTransactional extends TestCase {

    public void test() {
        
        DummyDao dao = new DummyDao();
        
        dao.doSomething();
        
    }
    
}

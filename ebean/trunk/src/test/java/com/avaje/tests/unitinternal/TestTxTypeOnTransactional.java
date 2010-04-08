package com.avaje.tests.unitinternal;

import com.avaje.tests.model.basic.xtra.TestDao;

import junit.framework.TestCase;

public class TestTxTypeOnTransactional extends TestCase {

    public void test() {
        
        TestDao dao = new TestDao();
        
        dao.doSomething();
        
    }
    
}

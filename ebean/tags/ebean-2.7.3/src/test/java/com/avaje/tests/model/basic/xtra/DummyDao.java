package com.avaje.tests.model.basic.xtra;

import com.avaje.ebean.TxType;
import com.avaje.ebean.annotation.Transactional;

public class DummyDao {

    @Transactional(type=TxType.REQUIRES_NEW)
    public void doSomething() {
        
        System.out.println("Hello World");
        
    }
}

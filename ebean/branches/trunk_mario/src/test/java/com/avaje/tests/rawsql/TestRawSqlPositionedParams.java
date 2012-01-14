package com.avaje.tests.rawsql;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.tests.model.basic.Customer;
import com.avaje.tests.model.basic.ResetBasicData;

public class TestRawSqlPositionedParams extends TestCase {

    public void test() {

        ResetBasicData.reset();
        
        RawSql rawSql = 
            RawSqlBuilder
                .parse("select r.id, r.name from o_customer r where r.id >= ? and r.name like ?")
                .columnMapping("r.id", "id")
                .columnMapping("r.name", "name")
                .create();
                    
        Query<Customer> query = Ebean.find(Customer.class);
        query.setRawSql(rawSql);
        query.setParameter(1, 1);
        query.setParameter(2, "R%");
        query.where().lt("id", 2001);
        
        List<Customer> list = query.findList();
        
        assertNotNull(list);
    }
    
    
    public void test_unparsed() {

        ResetBasicData.reset();
        
        RawSql rawSql = 
            RawSqlBuilder
                .unparsed("select r.id, r.name from o_customer r where r.id >= ? and r.name like ?")
                .columnMapping("r.id", "id")
                .columnMapping("r.name", "name")
                .create();
                    
        Query<Customer> query = Ebean.find(Customer.class);
        query.setRawSql(rawSql);
        query.setParameter(1, 1);
        query.setParameter(2, "R%");
        
        List<Customer> list = query.findList();
        
        assertNotNull(list);
    }
}

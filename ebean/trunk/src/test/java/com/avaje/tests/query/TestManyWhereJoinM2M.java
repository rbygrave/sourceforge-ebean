package com.avaje.tests.query;

import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.tests.model.basic.MRole;
import com.avaje.tests.model.basic.MUser;

public class TestManyWhereJoinM2M extends TestCase {

    public void test() {
        
        Ebean.beginTransaction();
        
        MRole r1 = new MRole();
        r1.setRoleName("role1");
        Ebean.save(r1);

        MRole r2 = new MRole();
        r2.setRoleName("role2special");
        Ebean.save(r2);

        MRole r3 = new MRole();
        r3.setRoleName("role3");
        Ebean.save(r3);

        MUser u0 = new MUser();
        u0.setUserName("user0");
        u0.addRole(r1);
        u0.addRole(r2);

        Ebean.save(u0);

        MUser u1 = new MUser();
        u1.setUserName("user1");
        u1.addRole(r1);

        Ebean.save(u1);
        
        Ebean.commitTransaction();
        
       
        
        Query<MUser> query = Ebean.find(MUser.class)
            .join("roles")
            // the where on a 'many' (like orders) requires an 
            // additional join and distinct which is independent
            // of a fetch join (if there is a fetch join) 
            .where().eq("roles.roleName", "role2special")
            .query();
        
        List<MUser> list = query.findList();
        System.out.println(list);
        
    }
}

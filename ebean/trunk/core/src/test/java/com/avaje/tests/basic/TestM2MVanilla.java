package com.avaje.tests.basic;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.MRole;
import com.avaje.tests.model.basic.MUser;

public class TestM2MVanilla extends TestCase {

    public void testVanilla() {

        MRole r1 = new MRole();
        r1.setRoleName("role1");
        Ebean.save(r1);

        MRole r2 = new MRole();
        r2.setRoleName("role2");
        Ebean.save(r2);

        MRole r3 = new MRole();
        r3.setRoleName("role3");
        Ebean.save(r3);

        MUser u0 = new MUser();
        u0.setUserName("something");

        Ebean.save(u0);

        MUser user = Ebean.find(MUser.class, u0.getUserid());

        List<MRole> roleList = new ArrayList<MRole>();
        roleList.add(r1);
        roleList.add(r2);

        user.setRoles(roleList);

        Ebean.save(user);

        MUser checkUser = Ebean.find(MUser.class, u0.getUserid());
        List<MRole> checkRoles = checkUser.getRoles();
        Assert.assertNotNull(checkRoles);
        Assert.assertTrue(checkRoles.size() == 2);

        checkRoles.add(r3);
        
        Ebean.save(checkUser);
        
        MUser checkUser2 = Ebean.find(MUser.class, u0.getUserid());
        List<MRole> checkRoles2 = checkUser2.getRoles();
        Assert.assertNotNull(checkRoles2);
        Assert.assertEquals("added a role", 3, checkRoles2.size());
        
    }

}

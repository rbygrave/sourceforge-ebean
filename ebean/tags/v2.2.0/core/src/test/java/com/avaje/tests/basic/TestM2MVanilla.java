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
		
		
	}
	
}

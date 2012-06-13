package com.avaje.tests.m2m;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.avaje.ebean.Ebean;
import com.avaje.tests.model.basic.MRole;
import com.avaje.tests.model.basic.MUser;

public class TestM2MModifyTest extends TestCase {

	public void test() {

		// Create 2 roles r0 and r1
		MRole r0 = new MRole("r0");
		MRole r1 = new MRole("r1");

		// Save r1 and r2
		Ebean.save(r0);
		Ebean.save(r1);

		// Create a new user
		MUser u0 = new MUser("usr0");
		u0.addRole(r0);
		u0.addRole(r1);

		// Save the user
		Ebean.save(u0);

		List<MRole> roles = u0.getRoles();

		Assert.assertTrue(roles.size() == 2);

		u0 = Ebean.find(MUser.class, u0.getUserid());

		roles = u0.getRoles();
		int nrRoles = roles.size();
		
		Assert.assertTrue(nrRoles == 2);

		roles.clear();
		roles.add(r0);
		roles.add(r1);
		roles.remove(r1);

		Ebean.save(u0);

		u0 = Ebean.find(MUser.class, u0.getUserid());

		roles = u0.getRoles();
		
		nrRoles = roles.size();

		Assert.assertTrue(nrRoles == 1);
	}
}

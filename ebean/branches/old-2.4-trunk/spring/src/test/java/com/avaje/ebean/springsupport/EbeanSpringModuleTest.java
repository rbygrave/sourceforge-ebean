/**
 * Copyright (C) 2009 the original author or authors
 *
 * This file is part of Ebean.
 *
 * Ebean is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Ebean is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebean.springsupport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Unit test for Ebean Spring Module.
 * @since 18.05.2009
 * @author E Mc Greal
 */
@ContextConfiguration(locations={"/init-database.xml"})
public class EbeanSpringModuleTest extends AbstractJUnit4SpringContextTests {

	/** The Constant logger. */
	private final static Logger logger = Logger.getLogger(EbeanSpringModuleTest.class.getName());

	/** The user service. */
	@Autowired
	private UserService userService;

    /**
     * Create the test case.
     */
    public EbeanSpringModuleTest(){
        super();
    }


	/**
	 * Test app.
	 */
	@Test
    public void testSaveUser(){
		logger.info("Saving new User...");
		User user = new User();
		user.setName("ebean");
		userService.save(user);
		logger.info("Saved new User");
    }

	/**
	 * Test app.
	 */
	@Test
    public void testFindUser(){
		logger.info("Finding User with OID = 1 ...");
		User user = userService.find(1);

		assertNotNull(user);
		assertTrue("ebean".equals(user.getName()));
		logger.info("Found User with OID = 1");
    }

	/**
	 * Gets the user service.
	 *
	 * @return the userService
	 */
	public UserService getUserService() {
		return userService;
	}


	/**
	 * Sets the user service.
	 *
	 * @param userService the userService to set
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
}

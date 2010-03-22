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
package com.avaje.ebean.spi.jpa;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.avaje.ebean.testdata.User;

/**
 * Unit test for Ebean Spring Module.
 * 
 * @since 18.05.2009
 * @author E Mc Greal
 */
@ContextConfiguration(locations = { "/init-jpa.xml" })
public class EbeanSpringJpaTest extends AbstractJUnit4SpringContextTests {

	/** The Constant logger. */
	private final static Logger logger = Logger.getLogger(EbeanSpringJpaTest.class.getName());

	/** The user service. */
	@Autowired
	private EntityManagerFactory emf;

	/**
	 * Create the test case.
	 */
	public EbeanSpringJpaTest() {
		super();
	}

	/**
	 * Test app.
	 */
	@Test
	public void testSaveUser() {
		logger.info("Saving new User...");
		EntityManager entityManager = (EntityManager) emf.createEntityManager();
		logger.info("em="+entityManager.getClass().getName());
		entityManager.getTransaction().begin();
		logger.info("txn started");
		User user = new User();
		user.setName("ebean");
		Assert.assertEquals("Should have unassigned user ID",0, user.getOid());
		entityManager.persist(user);
		entityManager.getTransaction().commit();
		Assert.assertEquals("Should generate user ID",1, user.getOid());
		logger.info("Saved new User");
	}

	/**
	 * Gets the user service.
	 * 
	 * @return the userService
	 */
	public EntityManagerFactory getEmf() {
		return emf;
	}

	/**
	 * Sets the user service.
	 * 
	 * @param userService
	 *            the userService to set
	 */
	public void setEmf(EntityManagerFactory v) {
		this.emf = v;
	}
}

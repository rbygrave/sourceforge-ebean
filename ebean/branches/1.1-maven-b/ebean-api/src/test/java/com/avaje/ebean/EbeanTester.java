package com.avaje.ebean;

/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

import javax.persistence.PersistenceException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test Harness for {@link Ebean}.
 * 
 * @author pmendelson
 * @since Jan, 2009
 * @version $Revision$, $Date: 2009-01-28 19:58:28 +0000 (Wed, 28 Jan
 *          2009) $
 */
public class EbeanTester {

	@Test(expectedExceptions=PersistenceException.class)
	public void testFalseStart() {
		Transaction t = Ebean.beginTransaction();
		Assert.assertNotNull(t);
	}

}

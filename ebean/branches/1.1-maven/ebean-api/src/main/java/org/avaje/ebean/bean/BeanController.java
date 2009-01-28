/**
 * Copyright (C) 2006  Robin Bygrave
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
package org.avaje.ebean.bean;

import java.util.Set;

import org.avaje.ebean.server.core.PersistRequest;

/**
 * Used to enhance or override the default bean persistence mechanism.
 * <p>
 * Note that if want to totally change the finding, you need to use a BeanFinder
 * rather than using postLoad().
 * </p>
 * <p>
 * Note that getTransaction() on the PersistRequest returns the transaction used
 * for the insert, update, delete or fetch. To explicitly use this same
 * transaction you should use this transaction via methods on EbeanServer.
 * </p>
 * 
 * <pre><code>
 *        Object extaBeanToSave = ...;
 *        Transaction t = request.getTransaction();
 *        EbeanServer server = request.getEbeanServer();
 *        server.save(extraBeanToSave, t);
 * </code></pre>
 * 
 * <p>
 * It is worth noting that BeanListener is different in three main ways from
 * BeanController postXXX methods.
 * <ul>
 * <li>BeanListener only sees successfully committed events. BeanController pre
 * and post methods occur before the commit or a rollback and will see events
 * that are later rolled back</li>
 * <li>BeanListener runs in a background thread and will not effect the
 * response time of the actual persist where as BeanController code will</li>
 * <li>BeanListener can be notified of events from other servers in a cluster.</li>
 * </ul>
 * </p>
 */
public interface BeanController {

	/**
	 * The types of entity bean this is the controller for.
	 */
	public Class<?>[] registerFor();
	
	/**
	 * Prior to the insert perform some action. Return true if you want the
	 * default functionality to continue.
	 * <p>
	 * Return false if you have completely replaced the insert functionality and
	 * do not want the default insert to be performed.
	 * </p>
	 */
	public boolean preInsert(PersistRequest request);

	/**
	 * Prior to the update perform some action. Return true if you want the
	 * default functionality to continue.
	 * <p>
	 * Return false if you have completely replaced the update functionality and
	 * do not want the default update to be performed.
	 * </p>
	 */
	public boolean preUpdate(PersistRequest request);

	/**
	 * Prior to the delete perform some action. Return true if you want the
	 * default functionality to continue.
	 * <p>
	 * Return false if you have completely replaced the delete functionality and
	 * do not want the default delete to be performed.
	 * </p>
	 */
	public boolean preDelete(PersistRequest request);

	/**
	 * Called after the insert was performed.
	 */
	public void postInsert(PersistRequest request);

	/**
	 * Called after the update was performed.
	 */
	public void postUpdate(PersistRequest request);

	/**
	 * Called after the delete was performed.
	 */
	public void postDelete(PersistRequest request);

	/**
	 * Called after every each bean is fetched and loaded from the database. You
	 * can override this to derive some information to set to the bean.
	 */
	public void postLoad(Object bean, Set<String> includedProperties);

}

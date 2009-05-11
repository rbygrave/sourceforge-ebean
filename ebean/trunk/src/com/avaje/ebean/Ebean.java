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
package com.avaje.ebean;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.DefaultServerFactory;
import com.avaje.ebean.server.core.ProtectedMethod;
import com.avaje.ebean.server.core.ServerFactory;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;
import com.avaje.ebean.server.util.InternalAssert;

/**
 * Provides API access for the 'default' Database and access to other Databases.
 * <p>
 * For each Database (javax.sql.DataSource) there is one EbeanServer. One of
 * those is referred to as the 'default' EbeanServer.
 * </p>
 * 
 * <p>
 * For developer convenience Ebean has static methods that proxy through to the
 * methods on the <em>'default'</em> EbeanServer. These methods are provided
 * for developers who are mostly using a single database. Many developers will
 * be able to use the methods on Ebean rather than get a EbeanServer.
 * </p>
 * 
 * <pre class="code">
 * // fetch shipped orders (and also their customer)
 * List&lt;Order&gt; list = 
 * 	Ebean.find(Order.class)
 * 	.join(&quot;customer&quot;)
 * 	.where()
 * 	.eq(&quot;status.code&quot;, &quot;SHIPPED&quot;)
 * 	.findList();
 * 
 *  // read/use the order list ...
 * for (Order order : list) {
 * 	Customer customer = order.getCustomer();
 * 	...
 * }
 * </pre>
 * 
 * <pre class="code">
 * // fetch order 10, modify and save
 * Order order = Ebean.find(Order.class, 10);   
 *                  
 * OrderStatus shipped = Ebean.getReference(OrderStatus.class, &quot;SHIPPED&quot;);
 * order.setStatus(shipped);
 * order.setShippedDate(shippedDate);
 * ...
 *                  
 * // implicitly creates a transaction and commits
 * Ebean.save(order);                                   
 * </pre>
 * 
 * <p>
 * When you have multiple databases and need access to a specific one the
 * {@link #getServer(String)} method provides access to the EbeanServer for that
 * specific database.
 * </p>
 * 
 * <pre class="code">
 * // Get access to the Human Resources EbeanServer/Database
 * EbeanServer hrDb = Ebean.getServer(&quot;hr&quot;);
 *                                                    
 *                                              
 * // fetch contact 3 from the HR database
 * Contact contact = hrDb.find(Contact.class, 3);
 *                                                    
 * contact.setName(&quot;I'm going to change&quot;);
 * ...
 *                                                    
 * // save the contact back to the HR database
 * hrDb.save(contact);                                   
 * </pre>
 * 
 * @version 1.2.0
 */
public final class Ebean {

	private static final Logger logger = Logger.getLogger(Ebean.class.getName());

	/**
	 * The version and date of build.
	 */
	private static final String EBVERSION = "1.2.0-090511";

	static {
		ProtectedMethodImpl pa = new ProtectedMethodImpl();
		ProtectedMethod.setPublicAccess(pa);
	}

	/**
	 * Manages creation and cache of EbeanServers.
	 */
	private static final Ebean.ServerManager serverMgr = new Ebean.ServerManager();

	/**
	 * Helper class for managing fast and safe access and creation of
	 * EbeanServers.
	 */
	private static final class ServerManager {

		/**
		 * Cache for fast concurrent read access.
		 */
		private final ConcurrentHashMap<String, EbeanServer> concMap = new ConcurrentHashMap<String, EbeanServer>();

		/**
		 * Cache for synchronized read, creation and put. Protected by the
		 * monitor object.
		 */
		private final HashMap<String, EbeanServer> syncMap = new HashMap<String, EbeanServer>();

		private final Object monitor = new Object();

		/**
		 * Factory for creating EbeanServer implementations.
		 */
		private final ServerFactory serverFactory;

		/**
		 * The 'default/primary' EbeanServer.
		 */
		private EbeanServer primaryServer;

		private ServerManager() {

			ConfigProperties props = GlobalProperties.getConfigProperties();

			String defaultServerName = getDefaultDataSourceName(props);
			serverFactory = createServerFactory(props);

			if (defaultServerName != null) {
				primaryServer = get(defaultServerName);
			}
		}

		private EbeanServer getPrimaryServer() {
			if (primaryServer == null) {
				String msg = "The default EbeanServer has not been defined?";
				msg += " This is normally set via the ebean.datasource.default property.";
				msg += " Otherwise it should be registered programatically via registerServer()";
				throw new PersistenceException(msg);
			}
			return primaryServer;
		}

		private EbeanServer register(ServerConfiguration configuration) {

			synchronized (monitor) {

				String name = configuration.getName();
				InternalAssert.notNull(name, "A name must be supplied");

				EbeanServer existingServer = syncMap.get(name);
				if (existingServer != null) {
					String msg = "Existing EbeanServer [" + name + "] is being replaced?";
					logger.warning(msg);
				}

				// create and put into both maps
				EbeanServer server = serverFactory.createServer(configuration);
				syncMap.put(name, server);
				concMap.put(name, server);

				if (configuration.isDefaultServer()) {
					primaryServer = server;
				}

				return server;
			}
		}

		private EbeanServer get(String name) {
			if (name == null) {
				return primaryServer;
			}
			// non-synchronized read
			EbeanServer server = concMap.get(name);
			if (server != null) {
				return server;
			}
			// synchronized read, create and put
			return getWithCreate(name);
		}

		/**
		 * Synchronized read, create and put of EbeanServers.
		 */
		private EbeanServer getWithCreate(String name) {

			synchronized (monitor) {

				EbeanServer server = syncMap.get(name);
				if (server == null) {
					// create and put into both maps
					server = serverFactory.createServer(name);
					syncMap.put(name, server);
					concMap.put(name, server);
				}
				return server;
			}
		}

		private ServerFactory createServerFactory(ConfigProperties baseProperties) {

			String implClassName = baseProperties.getProperty("ebean.serverfactory");

			String version = System.getProperty("java.version");
			logger.info("Ebean Version[" + EBVERSION + "] Java Version[" + version + "]");

			int delaySecs = baseProperties.getIntProperty("debug.start.delay", 0);
			delaySecs = baseProperties.getIntProperty("ebean.start.delay", delaySecs);
			if (delaySecs > 0) {
				try {
					// perhaps useful to delay the startup to give time to
					// attach a debugger when running in a server like tomcat.
					String m = "Ebean sleeping " + delaySecs + " seconds due to ebean.start.delay";
					logger.log(Level.INFO, m);
					Thread.sleep(delaySecs * 1000);

				} catch (InterruptedException e) {
					String m = "Interrupting debug.start.delay of " + delaySecs;
					logger.log(Level.SEVERE, m, e);
				}
			}
			if (implClassName == null) {
				return new DefaultServerFactory();

			} else {
				try {
					// use a client side implementation?
					Class<?> cz = Class.forName(implClassName);
					return (ServerFactory) cz.newInstance();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		/**
		 * Return the 'default' EbeanServer name. If this is null, then the
		 * default EbeanServer should be registered programmatically.
		 */
		private String getDefaultDataSourceName(ConfigProperties props) {
			String dflt = props.getProperty("ebean.datasource.default");
			if (dflt == null) {
				dflt = props.getProperty("datasource.default");
			}
			return dflt;
		}
	}

	private Ebean() {
	}

	/**
	 * Get the EbeanServer for a given DataSource. If name is null this will
	 * return the 'default' EbeanServer.
	 * <p>
	 * This is provided to access EbeanServer for databases other than the
	 * 'default' database. EbeanServer also provides more control over
	 * transactions and the ability to use transactions created externally to
	 * Ebean.
	 * </p>
	 * 
	 * <pre class="code">
	 * // use the &quot;hr&quot; database 
	 * EbeanServer hrDatabase = Ebean.getServer(&quot;hr&quot;);
	 * 
	 * Person person = hrDatabase.find(Person.class, 10);
	 * </pre>
	 * 
	 * @param name
	 *            the name of the server, use null for the 'default server'
	 */
	public static EbeanServer getServer(String name) {
		return serverMgr.get(name);
	}

	/**
	 * Programmatically create and register a EbeanServer.
	 * <p>
	 * You can register as many EbeanServers as you like but only one can be the
	 * 'default/primary' EbeanServer.
	 * </p>
	 * <p>
	 * Create a ServerConfiguration with a name and typically some other
	 * parameters such as the DataSource properties (or an already existing
	 * DataSource).
	 * </p>
	 * 
	 * @param configuration
	 *            Information used to construct the EbeanServer
	 * 
	 * @return the newly created and registered EbeanServer
	 */
	public static EbeanServer registerServer(ServerConfiguration configuration) {
		return serverMgr.register(configuration);
	}

	/**
	 * Log a comment to the transaction log of the current transaction.
	 */
	public static void logComment(String msg) {
		serverMgr.getPrimaryServer().logComment(msg);
	}

	/**
	 * Start a new explicit transaction.
	 * <p>
	 * The transaction is stored in a ThreadLocal variable and typically you
	 * only need to use the returned Transaction <em>IF</em> you wish to do
	 * things like use batch mode, change the transaction isolation level, use
	 * savepoints or log comments to the transaction log.
	 * </p>
	 * <p>
	 * Example of using a transaction to span multiple calls to find(), save()
	 * etc.
	 * </p>
	 * 
	 * <pre class="code">
	 * // start a transaction (stored in a ThreadLocal)
	 * Ebean.beginTransaction();
	 * try {   
	 *   Order order = Ebean.find(Order.class, 10);
	 *   ...
	 *                                                  
	 *   Ebean.save(order);
	 *                                                  
	 *   Ebean.commitTransaction();
	 *                                              
	 * } finally {
	 *   // rollback if we didn't commit
	 *   // i.e. an exception occurred before commitTransaction().
	 *   Ebean.endTransaction();
	 * }
	 * </pre>
	 * 
	 * <p>
	 * If you want to externalise the transaction management then you should be
	 * able to do this via EbeanServer. Specifically with EbeanServer you can
	 * pass the transaction to the various find() and save() execute() methods.
	 * This gives you the ability to create the transactions yourself externally
	 * from Ebean and pass those transactions through to the various methods
	 * available on EbeanServer.
	 * </p>
	 */
	public static Transaction beginTransaction() {
		return serverMgr.getPrimaryServer().beginTransaction();
	}

	/**
	 * Start a transaction additionally specifying the isolation level.
	 * 
	 * @param isolation the Transaction isolation level
	 *            
	 */
	public static Transaction beginTransaction(TxIsolation isolation) {
		return serverMgr.getPrimaryServer().beginTransaction(isolation);
	}

	/**
	 * Returns the current transaction or null if there is no current
	 * transaction in scope.
	 */
	public static Transaction currentTransaction() {
		return serverMgr.getPrimaryServer().currentTransaction();
	}

	/**
	 * Commit the current transaction.
	 */
	public static void commitTransaction() {
		serverMgr.getPrimaryServer().commitTransaction();
	}

	/**
	 * Rollback the current transaction.
	 */
	public static void rollbackTransaction() {
		serverMgr.getPrimaryServer().rollbackTransaction();
	}

	/**
	 * If the current transaction has already been committed do nothing
	 * otherwise rollback the transaction.
	 * <p>
	 * Useful to put in a finally block to ensure the transaction is ended,
	 * rather than a rollbackTransaction() in each catch block.
	 * </p>
	 * <p>
	 * Code example:
	 * </p>
	 * 
	 * <pre class="code">
	 * Ebean.beginTransaction();
	 * try {
	 * 	// do some fetching and or persisting
	 * 
	 * 	// commit at the end
	 * 	Ebean.commitTransaction();
	 * 
	 * } finally {
	 * 	// if commit didn't occur then rollback the transaction
	 * 	Ebean.endTransaction();
	 * }
	 * </pre>
	 */
	public static void endTransaction() {
		serverMgr.getPrimaryServer().endTransaction();
	}

	/**
	 * Validate a bean.
	 * <p>
	 * This will validate all of the properties on the bean in a recursive
	 * fashion. Typically if cascade save or delete is on then the validation
	 * will cascade those same associations.
	 * </p>
	 * <p>
	 * If no errors are detected then this returns null. Otherwise the returned
	 * InvalidValue holds the errors from all the rules tested. Use
	 * {@link InvalidValue#getErrors()} to get the list of errors that occurred.
	 * </p>
	 * 
	 * @return a InvalidValue holding the errors or null
	 */
	public static InvalidValue validate(Object bean) {
		return serverMgr.getPrimaryServer().validate(bean);
	}

	/**
	 * Validate a bean property.
	 * <p>
	 * If value passed in is null, then the property value from the bean is
	 * used.
	 * </p>
	 * <p>
	 * If no errors are detected an empty array is returned.
	 * </p>
	 * 
	 * @param bean
	 *            the bean used if value is null
	 * @param propertyName
	 *            the property to validate
	 * @param value
	 *            the value to validate. If this is null then the value from the
	 *            bean is used to validate.
	 * @return a InvalidValue holding the errors for this property (returns an
	 *         empty array if there are no errors).
	 */
	public static InvalidValue[] validate(Object bean, String propertyName, Object value) {
		return serverMgr.getPrimaryServer().validate(bean, propertyName, value);
	}

	/**
	 * Return a map of the differences between two objects of the same type.
	 * <p>
	 * When null is passed in for b, then the 'OldValues' of a is used for the
	 * difference comparison.
	 * </p>
	 */
	public static Map<String, ValuePair> diff(Object a, Object b) {
		return serverMgr.getPrimaryServer().diff(a, b);
	}

	/**
	 * Either Insert or Update the bean depending on its state.
	 * <p>
	 * If there is no current transaction one will be created and committed for
	 * you automatically.
	 * </p>
	 * <p>
	 * Save can cascade along relationships. For this to happen you need to
	 * specify a cascade of CascadeType.ALL or CascadeType.PERSIST on the
	 * OneToMany, OneToOne or ManyToMany annotation.
	 * </p>
	 * <p>
	 * In this example below the details property has a CascadeType.ALL set so
	 * saving an order will also save all its details.
	 * </p>
	 * 
	 * <pre class="code">
	 * public class Order {
	 *   ...
	 *   &#064;OneToMany(cascade=CascadeType.ALL, mappedBy=&quot;order&quot;)
	 *   &#064;JoinColumn(name=&quot;order_id&quot;)
	 *   List&lt;OrderDetail&gt; details;
	 *   ...
	 * }
	 * </pre>
	 * 
	 * <p>
	 * When a save cascades via a OneToMany or ManyToMany Ebean will
	 * automatically set the 'parent' object to the 'detail' object. In the
	 * example below in saving the order and cascade saving the order details
	 * the 'parent' order will be set against each order detail when it is
	 * saved.
	 * </p>
	 */
	public static void save(Object bean) throws OptimisticLockException {
		serverMgr.getPrimaryServer().save(bean);
	}

	/**
	 * Save all the beans from an Iterator.
	 */
	public static int save(Iterator<?> iterator) throws OptimisticLockException {

		return serverMgr.getPrimaryServer().save(iterator);
	}

	/**
	 * Delete the bean.
	 * <p>
	 * If there is no current transaction one will be created and committed for
	 * you automatically.
	 * </p>
	 */
	public static void delete(Object bean) throws OptimisticLockException {
		serverMgr.getPrimaryServer().delete(bean);
	}

	/**
	 * Delete all the beans from an Iterator.
	 */
	public static int delete(Iterator<?> it) throws OptimisticLockException {
		return serverMgr.getPrimaryServer().delete(it);
	}

	/**
	 * Refresh the values of a bean.
	 * <p>
	 * Note that this does not refresh any OneToMany or ManyToMany properties.
	 * </p>
	 */
	public static void refresh(Object bean) {
		serverMgr.getPrimaryServer().refresh(bean);
	}

	/**
	 * Refresh a 'many' property of a bean. <code>
	 * <pre class="code">
	 * Order order = ...;
	 * ...
	 * // refresh the order details...
	 * Ebean.refreshMany(order, &quot;details&quot;);
	 * </pre>
	 * 
	 * @param bean
	 *            the entity bean containing the List Set or Map to refresh.
	 * @param manyPropertyName
	 *            the property name of the List Set or Map to refresh.
	 */
	public static void refreshMany(Object bean, String manyPropertyName) {
		serverMgr.getPrimaryServer().refreshMany(bean, manyPropertyName);
	}

	/**
	 * Get a reference object.
	 * <p>
	 * This is sometimes described as a proxy (with lazy loading).
	 * </p>
	 * 
	 * <pre class="code">
	 * Product product = Ebean.getReference(Product.class, 1);
	 * 
	 * // You can get the id without causing a fetch/lazy load
	 * Integer productId = product.getId();
	 * 
	 * // If you try to get any other property a fetch/lazy loading will occur
	 * // This will cause a query to execute...
	 * String name = product.getName();
	 * </pre>
	 * 
	 * @param beanType
	 *            the type of entity bean
	 * @param id
	 *            the id value
	 */
	public static <T> T getReference(Class<T> beanType, Object id) {
		return serverMgr.getPrimaryServer().getReference(beanType, id);
	}

	/**
	 * Sort the list using the sortByClause which can contain a comma delimited
	 * list of property names and keywords asc, desc, nullsHigh and nullsLow.
	 * <ul>
	 * <li>asc - ascending order (which is the default)</li>
	 * <li>desc - Descending order </li>
	 * <li>nullsHigh - Treat null values as high/large values (which is the default)</li>
	 * <li>nullsLow- Treat null values as low/very small values </li>
	 * </ul>
	 * <p>
	 * If you leave off any keywords the defaults are ascending order and treating nulls as high values.
	 * </p>
	 * <p>
	 * Note that the sorting uses a Comparator and Collections.sort(); and does not invoke a DB query.
	 * </p>
	 * <pre class="code">
	 * 
	 * 	// find orders and their customers
	 * 	List&lt;Order&gt; list = Ebean.find(Order.class)
	 * 		.join("customer")
	 * 		.orderBy("id")
	 * 		.findList();
	 * 
	 * 
	 * 	// sort by customer name ascending, then by order shipDate 
	 * 	// ... then by the order status descending
	 * 	Ebean.sort(list, "customer.name, shipDate, status desc");
	 * 
	 * 	// sort by customer name descending (with nulls low) 
	 * 	// ... then by the order id
	 * 	Ebean.sort(list, "customer.name desc nullsLow, id");
	 * 
	 * </pre>
	 * 
	 * @param list the list of entity beans
	 * @param sortByClause the properties to sort the list by
	 */
	public static <T> void sort(List<T> list, String sortByClause){
		serverMgr.getPrimaryServer().sort(list, sortByClause);
	}

	/**
	 * Find a bean using its unique id. This will not use caching.
	 * 
	 * <pre class="code">
	 * // Fetch order 1
	 * Order order = Ebean.find(Order.class, 1);
	 * </pre>
	 * 
	 * <p>
	 * If you want more control over the query then you can use createQuery()
	 * and Query.findUnique();
	 * </p>
	 * 
	 * <pre class="code">
	 * // ... additionally fetching customer, customer shipping address, 
	 * //     order details, and the product associated with each order detail.
	 * // note: only product id and name is fetch (its a &quot;partial object&quot;).
	 * // note: all other objects use &quot;*&quot; and have all their properties fetched.
	 * 
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * query.setId(1);
	 * query.join(&quot;customer&quot;);
	 * query.join(&quot;customer.shippingAddress&quot;);
	 * query.join(&quot;details&quot;);
	 * 
	 * // fetch associated products but only fetch their product id and name
	 * query.join(&quot;details.product&quot;, &quot;name&quot;);
	 * 
	 * // traverse the object graph...
	 * 
	 * Order order = query.findUnique();
	 * Customer customer = order.getCustomer();
	 * Address shippingAddress = customer.getShippingAddress();
	 * List&lt;OrderDetail&gt; details = order.getDetails();
	 * OrderDetail detail0 = details.get(0);
	 * Product product = detail0.getProduct();
	 * String productName = product.getName();
	 * </pre>
	 * 
	 * @param beanType
	 *            the type of entity bean to fetch
	 * @param id
	 *            the id value
	 */
	public static <T> T find(Class<T> beanType, Object id) {
		return serverMgr.getPrimaryServer().find(beanType, id);
	}

	/**
	 * Create a <a href="SqlQuery.html">SqlQuery</a> for executing native sql
	 * query statements.
	 * <p>
	 * Note that you can use raw SQL with entity beans, refer to the SqlSelect
	 * annotation for examples.
	 * </p>
	 */
	public static SqlQuery createSqlQuery() {
		return serverMgr.getPrimaryServer().createSqlQuery();
	}

	/**
	 * Create a named sql query.
	 * <p>
	 * The query statement will be defined in a deployment orm xml file.
	 * </p>
	 * 
	 * @param namedQuery
	 *            the name of the query
	 */
	public static SqlQuery createSqlQuery(String namedQuery) {
		return serverMgr.getPrimaryServer().createSqlQuery(namedQuery);
	}

	/**
	 * Create a sql update for executing native dml statements.
	 * <p>
	 * Use this to execute a Insert Update or Delete statement. The statement
	 * will be native to the database and contain database table and column
	 * names.
	 * </p>
	 * <p>
	 * See {@link SqlUpdate} for example usage.
	 * </p>
	 * <p>
	 * Where possible it would be expected practice to put the statement in a
	 * orm xml file (named query) and use {@link #createSqlQuery(String)}.
	 * </p>
	 */
	public static SqlUpdate createSqlUpdate() {
		return serverMgr.getPrimaryServer().createSqlUpdate();
	}

	/**
	 * Create a named sql update.
	 * <p>
	 * The statement (an Insert Update or Delete statement) will be defined in a
	 * deployment orm xml file.
	 * </p>
	 * 
	 * <pre class="code">
	 * // Use a namedQuery 
	 * UpdateSql update = Ebean.createSqlUpdate(&quot;update.topic.count&quot;);
	 * update.setParameter(&quot;count&quot;, 1);
	 * update.setParameter(&quot;topicId&quot;, 50);
	 * int modifiedCount = update.execute();
	 * </pre>
	 */
	public static SqlUpdate createSqlUpdate(String namedQuery) {
		return serverMgr.getPrimaryServer().createSqlUpdate(namedQuery);
	}

	/**
	 * Return a named Query that will have defined joins, predicates etc.
	 * <p>
	 * The query is created from a statement that will be defined in a
	 * deployment orm xml file or NamedQuery annotations. The query will
	 * typically already define joins, predicates, order by clauses etc so often
	 * you will just need to bind required parameters and then execute the
	 * query.
	 * </p>
	 * 
	 * <pre class="code">
	 * // example
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class, &quot;new.for.customer&quot;);
	 * query.setParameter(&quot;customerId&quot;, 23);
	 * List&lt;Order&gt; newOrders = query.findList();
	 * </pre>
	 * 
	 * @param beanType
	 *            the class of entity to be fetched
	 * @param namedQuery
	 *            the name of the query
	 */
	public static <T> Query<T> createQuery(Class<T> beanType, String namedQuery) {

		return serverMgr.getPrimaryServer().createQuery(beanType, namedQuery);
	}

	/**
	 * Create a named orm update. The update statement (like a named query) is
	 * specified in the
	 * <p>
	 * The orm update differs from the SqlUpdate in that it uses the bean name
	 * and bean property names rather than table and column names.
	 * </p>
	 * <p>
	 * Note that named update statements can be specified in raw sql (with
	 * column and table names) or using bean name and bean property names. This
	 * can be specified with the isSql flag.
	 * </p>
	 * <p>
	 * Example named updates:
	 * </p>
	 * 
	 * <pre class="code">
	 * package app.data;
	 * 
	 * import ...
	 * 
	 * &#064;NamedUpdates(value = {
	 * 	&#064;NamedUpdate(
	 * 		name = &quot;setTitle&quot;, isSql = false, notifyCache = false, 
	 * 		update = &quot;update topic set title = :title, postCount = :postCount where id = :id&quot;),
	 * 	&#064;NamedUpdate(
	 * 		name = &quot;setPostCount&quot;, notifyCache = false, 
	 * 		update = &quot;update f_topic set post_count = :postCount where id = :id&quot;),
	 * 	&#064;NamedUpdate(
	 * 		name = &quot;incrementPostCount&quot;, notifyCache = false, isSql = false,
	 * 		update = &quot;update Topic set postCount = postCount + 1 where id = :id&quot;) 
	 * })
	 * &#064;Entity
	 * &#064;Table(name = &quot;f_topic&quot;)
	 * public class Topic {
	 * ...
	 * </pre>
	 * 
	 * <p>
	 * Example using a named update:
	 * </p>
	 * 
	 * <pre class="code">
	 * Update&lt;Topic&gt; update = Ebean.createUpdate(Topic.class, &quot;setPostCount&quot;);
	 * update.set(&quot;postCount&quot;, 10);
	 * update.set(&quot;id&quot;, 3);
	 * 
	 * int rows = update.execute();
	 * System.out.println(&quot;rows updated: &quot; + rows);
	 * </pre>
	 */
	public static <T> Update<T> createUpdate(Class<T> beanType, String namedUpdate) {

		return serverMgr.getPrimaryServer().createUpdate(beanType, namedUpdate);
	}

	/**
	 * Create a orm update where you will supply the insert/update or delete
	 * statement (rather than using a named one that is already defined using
	 * the &#064;NamedUpdates annotation).
	 * <p>
	 * The orm update differs from the sql update in that it you can use the
	 * bean name and bean property names rather than table and column names.
	 * </p>
	 * <p>
	 * Note that the statement gets translated from bean property names to
	 * database column names etc but you can also specify the statement in sql
	 * if you wish (using the isSql boolean flag on
	 * {@link Update#setUpdate(boolean, String)}.
	 * </p>
	 * <p>
	 * An example:
	 * </p>
	 * 
	 * <pre class="code">
	 * 
	 * boolean isSql = false;
	 * 
	 * // The bean name and properties - &quot;topic&quot;, &quot;postCount&quot; and &quot;id&quot; 
	 * // will be converted into their associated table and column names 
	 * String updStatement = &quot;update topic set postCount = :pc where id = :id&quot;;
	 * 
	 * Update&lt;Topic&gt; update = Ebean.createUpdate(Topic.class);
	 * update.setUpdate(isSql, updStatement);
	 * 
	 * update.set(&quot;pc&quot;, 9);
	 * update.set(&quot;id&quot;, 3);
	 * 
	 * int rows = update.execute();
	 * System.out.println(&quot;rows updated: &quot; + rows);
	 * </pre>
	 */
	public static <T> Update<T> createUpdate(Class<T> beanType) {

		return serverMgr.getPrimaryServer().createUpdate(beanType);
	}

	/**
	 * Create a query for a type of entity bean.
	 * <p>
	 * You can use the methods on the Query object to specify joins, predicates,
	 * order by, limits etc.
	 * </p>
	 * <p>
	 * You then use findList(), findSet(), findMap() and findUnique() to execute
	 * the query and return the collection or bean.
	 * </p>
	 * <p>
	 * Note that a query executed by {@link Query#findList()}
	 * {@link Query#findSet()} etc will execute against the same EbeanServer
	 * from which is was created.
	 * </p>
	 * 
	 * <pre class="code">
	 * // Find order 2 additionally fetching the customer, details and details.product name.
	 * 
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * query.join(&quot;customer&quot;);
	 * query.join(&quot;details&quot;);
	 * query.join(&quot;detail.product&quot;, &quot;name&quot;);
	 * query.setId(2);
	 * 
	 * Order order = query.findUnique();
	 * 
	 * // Find order 2 additionally fetching the customer, details and details.product name.
	 * // Note: same query as above but using the query language
	 * // Note: using a named query would be preferred practice
	 * 
	 * String oql = &quot;find order join customer join details join details.product (name) where id = :orderId &quot;;
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * query.setQuery(oql);
	 * query.setParameter(&quot;orderId&quot;, 2);
	 * 
	 * Order order = query.findUnique();
	 * 
	 * // Using a named query 
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class, &quot;with.details&quot;);
	 * query.setParameter(&quot;orderId&quot;, 2);
	 * 
	 * Order order = query.findUnique();
	 * 
	 * </pre>
	 * 
	 * @param beanType
	 *            the class of entity to be fetched
	 * @return A ORM Query object for this beanType
	 */
	public static <T> Query<T> createQuery(Class<T> beanType) {

		return serverMgr.getPrimaryServer().createQuery(beanType);
	}

	/**
	 * Create a query for a type of entity bean.
	 * <p>
	 * This is actually the same as {@link #createQuery(Class)}. The reason it
	 * exists is that people used to JPA will probably be looking for a
	 * createQuery method (the same as entityManager).
	 * </p>
	 * 
	 * @param beanType
	 *            the type of entity bean to find
	 * @return A ORM Query object for this beanType
	 */
	public static <T> Query<T> find(Class<T> beanType) {

		return serverMgr.getPrimaryServer().find(beanType);
	}

	/**
	 * Execute a Sql Update Delete or Insert statement. This returns the number
	 * of rows that where updated, deleted or inserted. If is executed in batch
	 * then this returns -1. You can get the actual rowCount after commit() from
	 * updateSql.getRowCount().
	 * <p>
	 * If you wish to execute a Sql Select natively then you should use the
	 * FindByNativeSql object.
	 * </p>
	 * <p>
	 * Note that the table modification information is automatically deduced and
	 * you do not need to call the Ebean.externalModification() method when you
	 * use this method.
	 * </p>
	 * <p>
	 * Example:
	 * </p>
	 * 
	 * <pre class="code">
	 * // example that uses 'named' parameters
	 * SqlUpdate update = new SqlUpdate();
	 * update.setSql(&quot;UPDATE f_topic set post_count = :count where id = :id&quot;);
	 * update.setParameter(&quot;id&quot;, 1);
	 * update.setParameter(&quot;count&quot;, 50);
	 * 
	 * int modifiedCount = Ebean.execute(update);
	 * 
	 * String msg = &quot;There where &quot; + modifiedCount + &quot;rows updated&quot;;
	 * </pre>
	 * 
	 * @param sqlUpdate
	 *            the update sql potentially with bind values
	 * @return the number of rows updated or deleted. -1 if executed in batch.
	 * @see SqlUpdate
	 * @see CallableSql
	 * @see Ebean#execute(CallableSql)
	 */
	public static int execute(SqlUpdate sqlUpdate) {
		return serverMgr.getPrimaryServer().execute(sqlUpdate);
	}

	/**
	 * For making calls to stored procedures.
	 * <p>
	 * Example:
	 * </p>
	 * 
	 * <pre class="code">
	 * String sql = &quot;{call sp_order_modify(?,?,?)}&quot;;
	 * 
	 * CallableSql cs = new CallableSql(sql);
	 * cs.setParameter(1, 27);
	 * cs.setParameter(2, &quot;SHIPPED&quot;);
	 * cs.registerOut(3, Types.INTEGER);
	 * 
	 * Ebean.execute(cs);
	 * 
	 * // read the out parameter
	 * Integer returnValue = (Integer) cs.getObject(3);
	 * </pre>
	 * 
	 * @see CallableSql
	 * @see Ebean#execute(SqlUpdate)
	 */
	public static void execute(CallableSql callableSql) {
		serverMgr.getPrimaryServer().execute(callableSql);
	}

	/**
	 * Execute a TxRunnable in a Transaction with an explicit scope.
	 * <p>
	 * The scope can control the transaction type, isolation and rollback
	 * semantics.
	 * </p>
	 * 
	 * <pre class="code">
	 *  // set specific transactional scope settings 
	 * TxScope scope = TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE);
	 * 
	 * Ebean.execute(scope, new TxRunnable() {
	 * 	public void run() {
	 * 		User u1 = Ebean.find(User.class, 1);
	 * 		...
	 * 
	 * 	}
	 * });
	 * </pre>
	 */
	public static void execute(TxScope scope, TxRunnable r) {
		serverMgr.getPrimaryServer().execute(scope, r);
	}

	/**
	 * Execute a TxRunnable in a Transaction with the default scope.
	 * <p>
	 * The default scope runs with REQUIRED and by default will rollback on any
	 * exception (checked or runtime).
	 * </p>
	 * 
	 * <pre class="code">
	 * Ebean.execute(new TxRunnable() {
	 * 	public void run() {
	 * 		User u1 = Ebean.find(User.class, 1);
	 * 		User u2 = Ebean.find(User.class, 2);
	 * 
	 * 		u1.setName(&quot;u1 mod&quot;);
	 * 		u2.setName(&quot;u2 mod&quot;);
	 * 
	 * 		Ebean.save(u1);
	 * 		Ebean.save(u2);
	 * 	}
	 * });
	 * </pre>
	 */
	public static void execute(TxRunnable r) {
		serverMgr.getPrimaryServer().execute(r);
	}

	/**
	 * Execute a TxCallable in a Transaction with an explicit scope.
	 * <p>
	 * The scope can control the transaction type, isolation and rollback
	 * semantics.
	 * </p>
	 * 
	 * <pre class="code">
	 *  // set specific transactional scope settings
	 * TxScope scope = TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE);
	 * 
	 * Ebean.execute(scope, new TxCallable&lt;String&gt;() {
	 * 	public String call() {
	 * 		User u1 = Ebean.find(User.class, 1);
	 * 		...
	 * 
	 * 		return u1.getEmail();
	 * 	}
	 * });
	 * </pre>
	 * 
	 */
	public static <T> T execute(TxScope scope, TxCallable<T> c) {
		return serverMgr.getPrimaryServer().execute(scope, c);
	}

	/**
	 * Execute a TxCallable in a Transaction with the default scope.
	 * <p>
	 * The default scope runs with REQUIRED and by default will rollback on any
	 * exception (checked or runtime).
	 * </p>
	 * <p>
	 * This is basically the same as TxRunnable except that it returns an Object
	 * (and you specify the return type via generics).
	 * </p>
	 * 
	 * <pre class="code">
	 * Ebean.execute(new TxCallable&lt;String&gt;() {
	 * 	public String call() {
	 * 		User u1 = Ebean.find(User.class, 1);
	 * 		User u2 = Ebean.find(User.class, 2);
	 * 
	 * 		u1.setName(&quot;u1 mod&quot;);
	 * 		u2.setName(&quot;u2 mod&quot;);
	 * 
	 * 		Ebean.save(u1);
	 * 		Ebean.save(u2);
	 * 
	 * 		return u1.getEmail();
	 * 	}
	 * });
	 * </pre>
	 */
	public static <T> T execute(TxCallable<T> c) {
		return serverMgr.getPrimaryServer().execute(c);
	}

	/**
	 * Inform Ebean that tables have been modified externally. These could be
	 * the result of from calling a stored procedure, other JDBC calls or
	 * external programs including other frameworks.
	 * <p>
	 * If you use Ebean.execute(UpdateSql) then the table modification
	 * information is automatically deduced and you do not need to call this
	 * method yourself.
	 * </p>
	 * <p>
	 * This information is used to invalidate objects out of the cache and
	 * potentially the lucene text indexes. This information is also
	 * automatically broadcast across the cluster.
	 * </p>
	 * <p>
	 * If there is a transaction then this information is placed into the
	 * current transactions event information. When the transaction is commited
	 * this information is registered (with the transaction manager). If this
	 * transaction is rolled back then none of the transaction event information
	 * registers including the information you put in via this method.
	 * </p>
	 * <p>
	 * If there is NO current transaction when you call this method then this
	 * information is registered immediately (with the transaction manager).
	 * </p>
	 * 
	 * @param tableName
	 *            the name of the table that was modified
	 * @param inserts
	 *            true if rows where inserted into the table
	 * @param updates
	 *            true if rows on the table where updated
	 * @param deletes
	 *            true if rows on the table where deleted
	 */
	public static void externalModification(String tableName, boolean inserts, boolean updates, boolean deletes) {

		serverMgr.getPrimaryServer().externalModification(tableName, inserts, updates, deletes);
	}

}

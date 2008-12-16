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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.OptimisticLockException;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.control.ServerControl;

/**
 * Provides the API for fetching and saving beans to a particular DataSource.
 * <p>
 * Much of the EbeanServer API matches the methods on Ebean and developers using
 * a single database will mostly use Ebean instead due to convenience.
 * </p>
 * <p>
 * There is one EbeanServer per Database (javax.sql.DataSource). One EbeanServer
 * is referred to as the <em>'default'</em> server and that is the one that
 * Ebean methods use.
 * </p>
 * <p>
 * Example: Get a EbeanServer
 * </p>
 * 
 * <pre class="code">
 * // Get access to the Human Resources EbeanServer/Database
 * EbeanServer hrServer = Ebean.getServer(&quot;HR&quot;);
 *                                                               
 *                                                         
 * // fetch contact 3 from the HR database
 * Contact contact = hrServer.find(Contact.class, new Integer(3));
 *                                                               
 * contact.setStatus(&quot;INACTIVE&quot;);
 * ...
 *                                                               
 * // save the contact back to the HR database
 * hrServer.save(contact);                                                          
 * </pre>
 * 
 * <p>
 * EbeanServer provides additional control over the Transactions compared to
 * Ebean.
 * </p>
 * <p>
 * <em>External Transactions:</em> If you wanted to use transactions created
 * externally to eBean then EbeanServer provides additional methods where you
 * can explicitly pass a transaction (that can be created externally).
 * </p>
 * <p>
 * <em>Bypass ThreadLocal Mechanism:</em> If you want to bypass the built in
 * ThreadLocal transaction management you can use the createTransaction()
 * method. Example: a single thread requires more than one transaction.
 * </p>
 * 
 * @see com.avaje.ebean.Ebean
 */
public interface EbeanServer {

	/**
	 * Return ServerControl which provides runtime access to control the logging
	 * and profiling etc.
	 */
	public ServerControl getServerControl();

	/**
	 * Return the name. This is typically the same as the DataSource name. The
	 * 'Primary Server' returns a null for its name.
	 * 
	 * @see Ebean#getServer(String)
	 */
	public String getName();

	/**
	 * Return a map of the differences between two objects of the same type.
	 * <p>
	 * When null is passed in for b, then the 'OldValues' of a is used for the
	 * difference comparison.
	 * </p>
	 */
	public Map<String, ValuePair> diff(Object a, Object b);

	/**
	 * Validate an entity bean.
	 * <p>
	 * The returned InvalidValue holds a tree of InvalidValue's. Typically you
	 * will use {@link InvalidValue#getErrors()}) to get a flat list of all the
	 * validation errors.
	 * </p>
	 */
	public InvalidValue validate(Object bean);

	/**
	 * Validate a single property on an entity bean.
	 * 
	 * @param bean
	 *            the entity bean that owns the property.
	 * @param propertyName
	 *            the name of the property to validate.
	 * @param value
	 *            if the value is null then the value from the bean is used to
	 *            perform the validation.
	 * @return the validation errors or an empty array.
	 */
	public InvalidValue[] validate(Object bean, String propertyName, Object value);

	/**
	 * Create a new empty EntityBean for a given type.
	 * <p>
	 * Available if a developer wishes to simulate an update with full
	 * concurrency checking. Otherwise this method is generally not required.
	 * </p>
	 */
	public EntityBean createEntityBean(Class<?> type);

	/**
	 * Create a named query for an entity bean (refer
	 * {@link Ebean#createQuery(Class, String)})
	 * <p>
	 * The query statement will be defined in a deployment orm xml file.
	 * </p>
	 * 
	 * @see Ebean#createQuery(Class, String)
	 */
	public <T> Query<T> createQuery(Class<T> beanType, String namedQuery);

	/**
	 * Create a query for an entity bean (refer {@link Ebean#createQuery(Class)}).
	 * 
	 * @see Ebean#createQuery(Class)
	 */
	public <T> Query<T> createQuery(Class<T> beanType);

	/**
	 * Create a query for a type of entity bean (the same as {@link EbeanServer#createQuery(Class)}).
	 */
	public <T> Query<T> find(Class<T> beanType);

	/**
	 * Create a named update for an entity bean (refer
	 * {@link Ebean#createUpdate(Class, String)}).
	 */
	public <T> Update<T> createUpdate(Class<T> beanType, String namedUpdate);

	/**
	 * Create a update for an entity bean where you will manually specify the
	 * insert update or delete statement.
	 */
	public <T> Update<T> createUpdate(Class<T> beanType);

	/**
	 * Create a sql query for executing native sql query statements (refer
	 * {@link Ebean#createSqlQuery()}).
	 * 
	 * @see Ebean#createSqlQuery()
	 */
	public SqlQuery createSqlQuery();

	/**
	 * Create a named sql query (refer {@link Ebean#createSqlQuery(String)}).
	 * <p>
	 * The query statement will be defined in a deployment orm xml file.
	 * </p>
	 * 
	 * @see Ebean#createSqlQuery(String)
	 */
	public SqlQuery createSqlQuery(String namedQuery);

	/**
	 * Create a sql update for executing native dml statements (refer
	 * {@link Ebean#createSqlUpdate()}).
	 * 
	 * @see Ebean#createSqlUpdate()
	 */
	public SqlUpdate createSqlUpdate();

	/**
	 * Create a named sql update (refer {@link Ebean#createSqlUpdate(String)}).
	 * <p>
	 * The statement (an Insert Update or Delete statement) will be defined in a
	 * deployment orm xml file.
	 * </p>
	 * 
	 * @see Ebean#createSqlUpdate(String)
	 */
	public SqlUpdate createSqlUpdate(String namedQuery);

	/**
	 * Create a new transaction that is not held in TransactionThreadLocal.
	 * <p>
	 * You will want to do this if you want multiple Transactions in a single
	 * thread or generally use transactions outside of the
	 * TransactionThreadLocal management.
	 * </p>
	 */
	public Transaction createTransaction();

	/**
	 * Create a new transaction additionally specifying the isolation level.
	 * <p>
	 * Note that this transaction is NOT stored in a thread local.
	 * </p>
	 */
	public Transaction createTransaction(TxIsolation isolation);

	/**
	 * Start a new transaction putting it into a ThreadLocal.
	 * 
	 * @see Ebean#beginTransaction()
	 */
	public Transaction beginTransaction();

	/**
	 * Start a transaction additionally specifying the isolation level.
	 */
	public Transaction beginTransaction(TxIsolation isolation);

	/**
	 * Returns the current transaction or null if there is no current
	 * transaction in scope.
	 */
	public Transaction currentTransaction();
	
	/**
	 * Commit the current transaction.
	 * 
	 * @see Ebean#commitTransaction()
	 */
	public void commitTransaction();

	/**
	 * Rollback the current transaction.
	 * 
	 * @see Ebean#rollbackTransaction()
	 */
	public void rollbackTransaction();

	/**
	 * If the current transaction has already been committed do nothing
	 * otherwise rollback the transaction.
	 * <p>
	 * Useful to put in a finally block to ensure the transaction is ended,
	 * rather than a rollbackTransaction() in each catch block.
	 * </p>
	 * <p>
	 * Code example:
	 * 
	 * <pre class="code">
	 * Ebean.startTransaction();
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
	 * 
	 * </p>
	 * 
	 * @see Ebean#endTransaction()
	 */
	public void endTransaction();

	/**
	 * Log a comment to the transaction log of the current transaction.
	 * 
	 * @see Ebean#logComment(String)
	 */
	public void logComment(String msg);

	/**
	 * Refresh the values of a bean.
	 * <p>
	 * Note that this does not refresh any OneToMany or ManyToMany properties.
	 * </p>
	 * 
	 * @see Ebean#refresh(Object)
	 */
	public void refresh(Object bean);

	/**
	 * Refresh a many property of an entity bean.
	 * 
	 * @param bean
	 *            the entity bean containing the 'many' property
	 * @param propertyName
	 *            the 'many' property to be refreshed
	 * 
	 * @see Ebean#refreshMany(Object, String)
	 */
	public void refreshMany(Object bean, String propertyName);

	/**
	 * Find a bean using its unique id.
	 * 
	 * @see Ebean#find(Class, Object)
	 */
	public <T> T find(Class<T> beanType, Object uid);

	/**
	 * Get a reference Object (see {@link Ebean#getReference(Class, Object)}.
	 * <p>
	 * This will not perform a query against the database.
	 * </p>
	 * 
	 * @see Ebean#getReference(Class, Object)
	 */
	public <T> T getReference(Class<T> beanType, Object uid);

	/**
	 * Execute a query returning a list of beans.
	 * <p>
	 * Generally you are able to use {@link Query#findList()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param <T>
	 *            the type of entity bean to fetch.
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the list of fetched beans.
	 * @see Query#findList()
	 */
	public <T> List<T> findList(Query<T> query, Transaction transaction);

	/**
	 * Execute the query returning a set of entity beans.
	 * <p>
	 * Generally you are able to use {@link Query#findSet()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param <T>
	 *            the type of entity bean to fetch.
	 * @param query
	 *            the query to execute
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the set of fetched beans.
	 * @see Query#findSet()
	 */
	public <T> Set<T> findSet(Query<T> query, Transaction transaction);

	/**
	 * Execute the query returning the entity beans in a Map.
	 * <p>
	 * Generally you are able to use {@link Query#findMap()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param <T>
	 *            the type of entity bean to fetch.
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the map of fetched beans.
	 * @see Query#findMap()
	 */
	public <T> Map<?, T> findMap(Query<T> query, Transaction transaction);

	/**
	 * Execute the query returning at most one entity bean. This will throw a
	 * PersistenceException if the query finds more than one result.
	 * <p>
	 * Generally you are able to use {@link Query#findUnique()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param <T>
	 *            the type of entity bean to fetch.
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the list of fetched beans.
	 * @see Query#findUnique()
	 */
	public <T> T findUnique(Query<T> query, Transaction transaction);

	/**
	 * Execute the sql query returning a list of MapBean.
	 * <p>
	 * Generally you are able to use {@link SqlQuery#findList()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the list of fetched MapBean.
	 * @see SqlQuery#findList()
	 */
	public List<MapBean> findList(SqlQuery query, Transaction transaction);

	/**
	 * Execute the sql query returning a set of MapBean.
	 * <p>
	 * Generally you are able to use {@link SqlQuery#findSet()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the set of fetched MapBean.
	 * @see SqlQuery#findSet()
	 */
	public Set<MapBean> findSet(SqlQuery query, Transaction transaction);

	/**
	 * Execute the sql query returning a map of MapBean.
	 * <p>
	 * Generally you are able to use {@link SqlQuery#findMap()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the set of fetched MapBean.
	 * @see SqlQuery#findMap()
	 */
	public Map<?, MapBean> findMap(SqlQuery query, Transaction transaction);

	/**
	 * Execute the sql query returning a single MapBean or null.
	 * <p>
	 * This will throw a PersistenceException if the query found more than one
	 * result.
	 * </p>
	 * <p>
	 * Generally you are able to use {@link SqlQuery#findUnique()} rather than
	 * explicitly calling this method. You could use this method if you wish to
	 * explicitly control the transaction used for the query.
	 * </p>
	 * 
	 * @param query
	 *            the query to execute.
	 * @param transaction
	 *            the transaction to use (can be null).
	 * @return the fetched MapBean or null if none was found.
	 * @see SqlQuery#findUnique()
	 */
	public MapBean findUnique(SqlQuery query, Transaction transaction);

	/**
	 * Persist the bean by either performing an insert or update.
	 * 
	 * @see Ebean#save(Object)
	 */
	public void save(Object bean) throws OptimisticLockException;

	/**
	 * Save all the beans in the iterator.
	 * 
	 * @see Ebean#save(Iterator)
	 */
	public int save(Iterator<?> it) throws OptimisticLockException;

	/**
	 * Delete the bean.
	 * 
	 * @see Ebean#delete(Object)
	 */
	public void delete(Object bean) throws OptimisticLockException;

	/**
	 * Delete all the beans from an Iterator.
	 */
	public int delete(Iterator<?> it) throws OptimisticLockException;

	/**
	 * Execute a SQL Update Delete or Insert statement using the current
	 * transaction. This returns the number of rows that where updated, deleted
	 * or inserted.
	 * <p>
	 * Refer to Ebean.execute(UpdateSql) for full documentation.
	 * </p>
	 * 
	 * @see Ebean#execute(SqlUpdate)
	 */
	public int execute(SqlUpdate updSql);

	/**
	 * Execute a ORM insert update or delete statement using the current
	 * transaction.
	 * <p>
	 * This returns the number of rows that where inserted, updated or deleted.
	 * </p>
	 */
	public int execute(Update<?> update);

	/**
	 * Execute a ORM insert update or delete statement with an explicit
	 * transaction.
	 */
	public int execute(Update<?> update, Transaction t);

	/**
	 * Call a stored procedure.
	 * <p>
	 * Refer to Ebean.execute(CallableSql) for full documentation.
	 * </p>
	 * 
	 * @see Ebean#execute(CallableSql)
	 */
	public void execute(CallableSql callableSql);

	/**
	 * Process committed changes from another framework.
	 * <p>
	 * This notifies this instance of the framework that beans have been
	 * committed externally to it. Either by another framework or clustered
	 * server. It uses this to maintain its cache and lucene indexes
	 * appropriately.
	 * </p>
	 * 
	 * @see Ebean#externalModification(String, boolean, boolean, boolean)
	 */
	public void externalModification(String tableName, boolean inserted, boolean updated, boolean deleted);

	/**
	 * Find a entity bean with an explicit transaction.
	 * 
	 * @param <T>
	 *            the type of entity bean to find
	 * @param beanType
	 *            the type of entity bean to find
	 * @param uid
	 *            the bean id value
	 * @param transaction
	 *            the transaction to use (can be null)
	 */
	public <T> T find(Class<T> beanType, Object uid, Transaction transaction);

	/**
	 * Insert or update a bean with an explicit transaction.
	 */
	public void save(Object bean, Transaction t) throws OptimisticLockException;

	/**
	 * Save all the beans in the iterator with an explicit transaction.
	 */
	public int save(Iterator<?> it, Transaction t) throws OptimisticLockException;

	/**
	 * Delete the bean with an explicit transaction.
	 */
	public void delete(Object bean, Transaction t) throws OptimisticLockException;

	/**
	 * Delete all the beans from an iterator.
	 */
	public int delete(Iterator<?> it, Transaction t) throws OptimisticLockException;

	/**
	 * Execute explicitly passing a transaction.
	 */
	public int execute(SqlUpdate updSql, Transaction t);

	/**
	 * Execute explicitly passing a transaction.
	 */
	public void execute(CallableSql callableSql, Transaction t);

	/**
	 * Execute a TxRunnable in a Transaction with an explicit scope.
	 * <p>
	 * The scope can control the transaction type, isolation and rollback
	 * semantics.
	 * </p>
	 */
	public void execute(TxScope scope, TxRunnable r);

	/**
	 * Execute a TxRunnable in a Transaction with the default scope.
	 * <p>
	 * The default scope runs with REQUIRED and by default will rollback
	 * on any exception (checked or runtime).
	 * </p>
	 */
	public void execute(TxRunnable r);

	/**
	 * Execute a TxCallable in a Transaction with an explicit scope.
	 * <p>
	 * The scope can control the transaction type, isolation and rollback
	 * semantics.
	 * </p>
	 */
	public <T> T execute(TxScope scope, TxCallable<T> c);

	/**
	 * Execute a TxCallable in a Transaction with the default scope.
	 * <p>
	 * The default scope runs with REQUIRED and by default will rollback
	 * on any exception (checked or runtime).
	 * </p>
	 */
	public <T> T execute(TxCallable<T> c);

}

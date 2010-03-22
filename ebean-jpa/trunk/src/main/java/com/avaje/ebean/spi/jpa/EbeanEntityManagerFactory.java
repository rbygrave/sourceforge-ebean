package com.avaje.ebean.springsupport.jpa;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.sql.DataSource;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.Transaction;

//import org.avaje.ebean.spi.spring.EBeanDataSourceFactory;

/**
 * DOCTASK: DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision$
 */
public class EbeanEntityManagerFactory implements EntityManagerFactory {
	// ~ Constants
	// ----------------------------------------------------------------------------------------------------------------

	private static Logger log = Logger.getLogger("jpa.spi");

	// ~ Instance fields
	// ----------------------------------------------------------------------------------------------------------

	private EbeanServer m_server;
	private EbeanEntityManager m_entity_mgr;
	private String m_puname;

	// ~ Constructors
	// -------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new EbeanEntityManagerFactory object.
	 * 
	 * @param persistenceUnitName
	 *            DOCUMENT ME!
	 */
	public EbeanEntityManagerFactory(String persistenceUnitName) {
		log.info("new EbeanEntityManagerFactory(" + persistenceUnitName + ")");
		m_puname = persistenceUnitName;
		m_entity_mgr = new EbeanEntityManager();
	}

	// ~ Methods
	// ------------------------------------------------------------------------------------------------------------------

	/** Dependency injection point */
	public void setDataSource(DataSource v) {
		// new EBeanDataSourceFactory().setDataSource(v);
//		m_data_source = v;
		log.info("m_puname=" + m_puname + ",  ds=" + v);
		startEbean();
	}

	/** Underlying Ebean server instance */
	public EbeanServer getEbeanServer() {
		if (m_server == null) {
			startEbean();
		}
		return m_server;
	}

	private synchronized void startEbean() {
		m_server = Ebean.getServer(this.m_puname);
		log.info("m_server=" + m_server);
		m_entity_mgr = new EbeanEntityManager();
		log.info(String.valueOf(m_server));
	}

	/** @see EntityManagerFactory#close() */
	public void close() {
		log.info(String.valueOf(m_server));
		m_server = null;
	}

	/** @see EntityManagerFactory#createEntityManager() */
	public EntityManager createEntityManager() {
		System.err.println("cem: " + m_entity_mgr);
		return m_entity_mgr;
	}

	/** @see EntityManagerFactory#createEntityManager(Map) */
	public EntityManager createEntityManager(Map map) {
		return m_entity_mgr;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return DOCUMENT ME!
	 */
	public boolean isOpen() {
		return m_server != null;
	}

	// ~ Inner Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * DOCTASK: DOCUMENT ME!
	 * 
	 * @author $author$
	 * @version $Revision$, $Date: 2009-01-24 18:37:30 +0000 (Sat, 24 Jan
	 *          2009) $
	 */
	public class EbeanEntityManager implements EntityManager {
		// ~ Methods
		// --------------------------------------------------------------------------------------------------------------

		/** @see EntityManager#clear() */
		public void clear() {
		}

		/** @see EntityManager#clear() */
		public void close() {
		}

		/** @see EntityManager#contains() */
		public boolean contains(Object entity) {
			return getEbeanServer().equals(entity);
		}

		/** @see EntityManager#contains() */
		public Query createNamedQuery(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query createNativeQuery(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query createNativeQuery(String arg0, Class arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query createNativeQuery(String arg0, String arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query createQuery(String sql) {
			getEbeanServer();
			log.info("new Q: " + sql + "  mserver=" + m_server);
			com.avaje.ebean.Query q = null; // getEbeanServer().createQuery(Course.class);
			q.setQuery("find Course order by title");
			return new QueryWraper2(q);
		}

		/** @see javax.persistence.EntityManager.find(Class<T>,Object) */
		public <T> T find(Class<T> entityClass, Object id) {
			return getEbeanServer().find(entityClass, id);
		}

		/**
		 * {@inheritDoc}
		 */
		public void flush() {
			// TODO Auto-generated method stub
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public Object getDelegate() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public FlushModeType getFlushMode() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param <T>
		 *            DOCUMENT ME!
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public <T> T getReference(Class<T> arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#getTransaction() */
		public EntityTransaction getTransaction() {
			Transaction tran=getEbeanServer().currentTransaction();
			if(tran==null) {
				getEbeanServer().beginTransaction();
			}
			return new EbeanEntityTransaction(tran);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public boolean isOpen() {
			// TODO Auto-generated method stub
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public void joinTransaction() {
			// TODO Auto-generated method stub
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 */
		public void lock(Object entity, LockModeType arg1) {
			// TODO Auto-generated method stub
		}

		/**
		 * Needs some sort of shuffling with persistant value
		 * 
		 * @see EntityManager#merge(Object)
		 */
		public <T> T merge(T entity) {
			return entity;
		}

		/** Implementation of {@link EntityManager#persist(Object)} */
		public void persist(Object entity) {
			log.info("persist " + entity + " on  " + m_server);
			getEbeanServer().save(entity);
		}

		/** @see EntityManager#refresh(Object) */
		public void refresh(Object entity) {
			getEbeanServer().refresh(entity);
		}

		/** @see EntityManager#remove(Object) */
		public void remove(Object entity) {
			getEbeanServer().delete(entity);
		}

		/** @see EntityManager#setFlushMode(FlushModeType) */
		public void setFlushMode(FlushModeType flushMode) {
		}
	}

	/**
	 * DOCTASK: DOCUMENT ME!
	 * 
	 * @author $author$
	 * @version $Revision$, $Date: 2009-01-24 18:37:30 +0000 (Sat, 24 Jan
	 *          2009) $
	 */
	public class QueryWraper implements Query {
		// ~ Instance fields
		// ------------------------------------------------------------------------------------------------------

		SqlQuery m_payload;

		// ~ Constructors
		// ---------------------------------------------------------------------------------------------------------

		/**
		 * Creates a new QueryWraper object.
		 * 
		 * @param qimpl
		 *            DOCUMENT ME!
		 */
		public QueryWraper(SqlQuery qimpl) {
			m_payload = qimpl;
		}

		// ~ Methods
		// --------------------------------------------------------------------------------------------------------------

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public int executeUpdate() {
			// TODO Auto-generated method stub
			return 0;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public List getResultList() {
			return m_payload.findList();
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public Object getSingleResult() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setFirstResult(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setFlushMode(FlushModeType arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setHint(String arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setMaxResults(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(String arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(int arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * @param arg2
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(String arg0, Date arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * @param arg2
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(String arg0, Calendar arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * @param arg2
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(int arg0, Date arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * @param arg2
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(int arg0, Calendar arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * DOCTASK: DOCUMENT ME!
	 * 
	 * @author $author$
	 * @version $Revision$, $Date: 2009-01-24 18:37:30 +0000 (Sat, 24 Jan
	 *          2009) $
	 */
	public class QueryWraper2 implements Query {
		// ~ Instance fields
		// ------------------------------------------------------------------------------------------------------

		private com.avaje.ebean.Query m_payload;

		// ~ Constructors
		// ---------------------------------------------------------------------------------------------------------

		/**
		 * Creates a new QueryWraper2 object.
		 * 
		 * @param q
		 *            DOCUMENT ME!
		 */
		public QueryWraper2(com.avaje.ebean.Query q) {
			m_payload = q;
		}

		// ~ Methods
		// --------------------------------------------------------------------------------------------------------------

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public int executeUpdate() {
			// TODO Auto-generated method stub
			return 0;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public List getResultList() {
			return m_payload.findList();
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @return DOCUMENT ME!
		 */
		public Object getSingleResult() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setFirstResult(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setFlushMode(FlushModeType arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setHint(String arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setMaxResults(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(String arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(int arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @param arg0
		 *            DOCUMENT ME!
		 * @param arg1
		 *            DOCUMENT ME!
		 * @param arg2
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 */
		public Query setParameter(String arg0, Date arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query setParameter(String arg0, Calendar arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query setParameter(int arg0, Date arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		/** @see EntityManager#contains() */
		public Query setParameter(int arg0, Calendar arg1, TemporalType arg2) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
package com.avaje.ebean.server.bean;

import java.util.Iterator;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanQueryRequest;
import com.avaje.ebean.bean.QueryType;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.collection.BeanList;
import com.avaje.ebean.meta.MetaAutoFetchStatistic;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.Statistics;
import com.avaje.ebean.server.core.InternalEbeanServer;

/**
 * Bean Finder for MetaAutoFetchStatistic.
 * <p>
 * This gets the meta data from the AutoFetchManager and creates a copy of that
 * data to give back to the caller in the form of MetaAutoFetchStatistic beans.
 * </p>
 */
public class BFAutoFetchStatisticFinder implements BeanFinder<MetaAutoFetchStatistic> {


	public MetaAutoFetchStatistic find(BeanQueryRequest<MetaAutoFetchStatistic> request) {
		OrmQuery<MetaAutoFetchStatistic> query = (OrmQuery<MetaAutoFetchStatistic>)request.getQuery();
		try {
			String queryPointKey = (String) query.getId();

			InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
			AutoFetchManager manager = server.getAutoFetchManager();

			Statistics stats = manager.getStatistics(queryPointKey);
			if (stats != null) {
				return stats.createPublicMeta();
			} else {
				return null;
			}

		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Only returns Lists at this stage.
	 */
	public BeanCollection<MetaAutoFetchStatistic> findMany(BeanQueryRequest<MetaAutoFetchStatistic> request) {

		QueryType queryType = request.getQueryType();
		if (!queryType.equals(QueryType.LIST)) {
			throw new PersistenceException("Only findList() supported at this stage.");
		}

		InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
		AutoFetchManager manager = server.getAutoFetchManager();

		BeanList<MetaAutoFetchStatistic> list = new BeanList<MetaAutoFetchStatistic>();

		Iterator<Statistics> it = manager.iterateStatistics();
		while (it.hasNext()) {
			Statistics stats = it.next();
			// create a copy for public use
			list.add(stats.createPublicMeta());
		}

		return list;
	}

}

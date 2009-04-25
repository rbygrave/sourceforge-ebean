package com.avaje.ebean.server.bean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.meta.MetaAutoFetchStatistic;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.Statistics;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.deploy.ManyType;

/**
 * Bean Finder for MetaAutoFetchStatistic.
 * <p>
 * This gets the meta data from the AutoFetchManager and creates a copy of that
 * data to give back to the caller in the form of MetaAutoFetchStatistic beans.
 * </p>
 */
public class BFAutoFetchStatisticFinder implements BeanFinder {

	public Class<?>[] registerFor() {
		return new Class<?>[]{MetaAutoFetchStatistic.class};
	}

	public Object find(QueryRequest<?> request) {
		OrmQuery<?> query = request.getQuery();
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
	public Object findMany(QueryRequest<?> request) {

		ManyType manyType = request.getManyType();
		if (!manyType.isList()) {
			throw new PersistenceException("Only findList() supported at this stage.");
		}

		InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
		AutoFetchManager manager = server.getAutoFetchManager();

		List<MetaAutoFetchStatistic> list = new ArrayList<MetaAutoFetchStatistic>();

		Iterator<Statistics> it = manager.iterateStatistics();
		while (it.hasNext()) {
			Statistics stats = it.next();
			// create a copy for public use
			list.add(stats.createPublicMeta());
		}

		return list;
	}

}

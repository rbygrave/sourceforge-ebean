package com.avaje.ebean.server.bean;

import java.util.Iterator;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanQueryRequest;
import com.avaje.ebean.bean.QueryType;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.collection.BeanList;
import com.avaje.ebean.meta.MetaAutoFetchTunedQueryInfo;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.TunedQueryInfo;
import com.avaje.ebean.server.core.InternalEbeanServer;

/**
 * BeanFinder for MetaAutoFetchTunedFetch.
 */
public class BFAutoFetchTunedFetchFinder implements BeanFinder<MetaAutoFetchTunedQueryInfo> {


	public MetaAutoFetchTunedQueryInfo find(BeanQueryRequest<MetaAutoFetchTunedQueryInfo> request) {
		
		OrmQuery<?> query = (OrmQuery<?>)request.getQuery();
		try {
			String queryPointKey = (String)query.getId();
			
			InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
			AutoFetchManager manager = server.getAutoFetchManager();
	
			TunedQueryInfo tunedFetch = manager.getTunedQueryInfo(queryPointKey);
			if (tunedFetch != null){
				return tunedFetch.createPublicMeta();
			} else {
				return null;
			}
			
		} catch (Exception e){
			throw new PersistenceException(e);
		}
	}

	/**
	 * Only returns Lists at this stage.
	 */
	public BeanCollection<MetaAutoFetchTunedQueryInfo> findMany(BeanQueryRequest<MetaAutoFetchTunedQueryInfo> request) {

		QueryType queryType = request.getQueryType();
		if (!queryType.equals(QueryType.LIST)){
			throw new PersistenceException("Only findList() supported at this stage.");
		}
		
		InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
		AutoFetchManager manager = server.getAutoFetchManager();
		
		BeanList<MetaAutoFetchTunedQueryInfo> list = new BeanList<MetaAutoFetchTunedQueryInfo>();
		
		Iterator<TunedQueryInfo> it = manager.iterateTunedQueryInfo();
		while (it.hasNext()) {
			TunedQueryInfo tunedFetch = it.next();
			// create a copy for public use
			list.add(tunedFetch.createPublicMeta());
		}
		
		String orderBy = request.getQuery().getOrderBy();
		if (orderBy == null){
			orderBy = "beanType, origQueryPlanHash";
		}
		server.sort(list, orderBy);


		return list;
	}

}

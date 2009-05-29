package com.avaje.ebean.server.bean;

import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanQueryRequest;
import com.avaje.ebean.bean.QueryType;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.collection.BeanList;
import com.avaje.ebean.meta.MetaQueryStatistic;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.query.CQueryPlan;

/**
 * BeanFinder for MetaQueryStatistic.
 */
public class BFQueryStatisticFinder implements BeanFinder<MetaQueryStatistic> {

	
	public MetaQueryStatistic find(BeanQueryRequest<MetaQueryStatistic> request) {
		throw new RuntimeException("Not Supported yet");
	}

	/**
	 * Only returns Lists at this stage.
	 */
	public BeanCollection<MetaQueryStatistic> findMany(BeanQueryRequest<MetaQueryStatistic> request) {

		QueryType queryType = request.getQueryType();
		if (!queryType.equals(QueryType.LIST)){
			throw new PersistenceException("Only findList() supported at this stage.");
		}
		
		BeanList<MetaQueryStatistic> list = new BeanList<MetaQueryStatistic>();
		
		InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
		build(list, server);
		
		String orderBy = request.getQuery().getOrderBy();
		if (orderBy == null){
			orderBy = "beanType, origQueryPlanHash, autofetchTuned";
		}
		server.sort(list, orderBy);

		return list;
	}

	private void build(List<MetaQueryStatistic> list, InternalEbeanServer server) {

		for (BeanDescriptor<?> desc : server.getBeanDescriptors()) {
			desc.clearQueryStatistics();			
			build(list, desc);
		}		
	}
	
	private void build(List<MetaQueryStatistic> list, BeanDescriptor<?> desc) {

		Iterator<CQueryPlan> it = desc.queryPlans();
		while (it.hasNext()) {
			CQueryPlan queryPlan = (CQueryPlan) it.next();
			list.add(queryPlan.createMetaQueryStatistic(desc.getFullName()));
		}
	}
	
}

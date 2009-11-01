package com.avaje.ebean.server.bean;

import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Query;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.common.BeanList;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.meta.MetaQueryStatistic;
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

		Query.Type queryType = request.getQuery().getType();
		if (!queryType.equals(Query.Type.LIST)){
			throw new PersistenceException("Only findList() supported at this stage.");
		}
		
		BeanList<MetaQueryStatistic> list = new BeanList<MetaQueryStatistic>();
		
		SpiEbeanServer server = (SpiEbeanServer) request.getEbeanServer();
		build(list, server);
		
		String orderBy = request.getQuery().order().toStringFormat();
		if (orderBy == null){
			orderBy = "beanType, origQueryPlanHash, autofetchTuned";
		}
		server.sort(list, orderBy);

		return list;
	}

	private void build(List<MetaQueryStatistic> list, SpiEbeanServer server) {

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

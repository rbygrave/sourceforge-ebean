package com.avaje.ebean.server.bean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.meta.MetaQueryStatistic;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.ManyType;
import com.avaje.ebean.server.query.CQueryPlan;

/**
 * BeanFinder for MetaQueryStatistic.
 */
public class BFQueryStatisticFinder implements BeanFinder {


	public Class<?>[] registerFor() {
		return new Class<?>[]{MetaQueryStatistic.class};
	}
	
	public Object find(QueryRequest<?> request) {
		throw new RuntimeException("Not Supported yet");
	}

	/**
	 * Only returns Lists at this stage.
	 */
	public Object findMany(QueryRequest<?> request) {

		ManyType manyType = request.getManyType();
		if (!manyType.isList()){
			throw new PersistenceException("Only findList() supported at this stage.");
		}
		
		List<MetaQueryStatistic> list = new ArrayList<MetaQueryStatistic>();
		
		InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
		build(list, server);

		return list;
	}

	private void build(List<MetaQueryStatistic> list, InternalEbeanServer server) {

		Iterator<BeanDescriptor<?>> it = server.descriptors();
		while (it.hasNext()) {
			BeanDescriptor<?> desc = (BeanDescriptor<?>) it.next();
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

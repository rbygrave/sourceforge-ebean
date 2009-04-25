package com.avaje.ebean.server.bean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.meta.MetaAutoFetchTunedQueryInfo;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.TunedQueryInfo;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.deploy.ManyType;

/**
 * BeanFinder for MetaAutoFetchTunedFetch.
 */
public class BFAutoFetchTunedFetchFinder implements BeanFinder {

	public Class<?>[] registerFor() {
		return new Class<?>[]{MetaAutoFetchTunedQueryInfo.class};
	}


	public Object find(QueryRequest<?> request) {
		OrmQuery<?> query = request.getQuery();
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
	public Object findMany(QueryRequest<?> request) {

		ManyType manyType = request.getManyType();
		if (!manyType.isList()){
			throw new PersistenceException("Only findList() supported at this stage.");
		}
		
		InternalEbeanServer server = (InternalEbeanServer) request.getEbeanServer();
		AutoFetchManager manager = server.getAutoFetchManager();
		
		List<MetaAutoFetchTunedQueryInfo> list = new ArrayList<MetaAutoFetchTunedQueryInfo>();
		
		Iterator<TunedQueryInfo> it = manager.iterateTunedQueryInfo();
		while (it.hasNext()) {
			TunedQueryInfo tunedFetch = it.next();
			// create a copy for public use
			list.add(tunedFetch.createPublicMeta());
		}

		return list;
	}

}

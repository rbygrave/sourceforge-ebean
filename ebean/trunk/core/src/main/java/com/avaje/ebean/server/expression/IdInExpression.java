package com.avaje.ebean.server.expression;

import java.util.List;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpression;
import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.id.IdBinder;
import com.avaje.ebean.util.DefaultExpressionRequest;


/**
 * Slightly redundant as Query.setId() ultimately also does the same job.
 */
class IdInExpression implements SpiExpression {

	private static final long serialVersionUID = 1L;

	private final List<?> idList;
	
	IdInExpression(List<?> idList) {
		this.idList = idList;
	}

	/**
	 * Always returns false.
	 */
	public boolean containsMany(BeanDescriptor<?> desc) {
		return false;
	}

	public void addBindValues(SpiExpressionRequest request) {
		
		// Bind the Id values including EmbeddedId and multiple Id 
		
		DefaultExpressionRequest r = (DefaultExpressionRequest)request;
		BeanDescriptor<?> descriptor = r.getBeanDescriptor();
		IdBinder idBinder = descriptor.getIdBinder();
		
		for (int i = 0; i < idList.size(); i++) {
			idBinder.addIdInBindValue(request, idList.get(i));
		}	
	}

	public void addSql(SpiExpressionRequest request) {
		
		DefaultExpressionRequest r = (DefaultExpressionRequest)request;
		BeanDescriptor<?> descriptor = r.getBeanDescriptor();
		IdBinder idBinder = descriptor.getIdBinder();
		
		request.append(descriptor.getIdBinderInLHSSql());
		request.append(" in (");
		for (int i = 0; i < idList.size(); i++) {
			if (i > 0){
				request.append(",");				
			}
			idBinder.addIdInValueSql(request);
		}
		request.append(") ");
	}

	/**
	 * No properties so this is just a unique static number.
	 */
	public int queryAutoFetchHash() {
		// this number is unique for a given bean type
		// which is all that is required
		return IdInExpression.class.getName().hashCode();
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		return idList.hashCode();
	}
	
	
}

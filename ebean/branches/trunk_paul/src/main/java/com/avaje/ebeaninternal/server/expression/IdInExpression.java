package com.avaje.ebeaninternal.server.expression;

import java.util.List;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.id.IdBinder;
import com.avaje.ebeaninternal.util.DefaultExpressionRequest;


/**
 * Slightly redundant as Query.setId() ultimately also does the same job.
 */
class IdInExpression implements SpiExpression {

	private static final long serialVersionUID = 1L;

	private final List<?> idList;
	
	IdInExpression(List<?> idList) {
		this.idList = idList;
	}

	public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {
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
	 * Incorporates the number of Id values to bind.
	 */
	public int queryAutoFetchHash() {
		// this number is unique for a given bean type
		// which is all that is required
		int hc = IdInExpression.class.getName().hashCode();
		hc = hc * 31 + idList.size();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		return idList.hashCode();
	}
	
	
}

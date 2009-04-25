package com.avaje.ebean.server.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.expression.InternalExpressionList;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.DeployPropertyParser;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.util.BindParamsParser;
import com.avaje.ebean.util.BindParams;
import com.avaje.ebean.util.DefaultExpressionRequest;
import com.avaje.ebean.util.BindParams.OrderedList;

/**
 * Compile Query Predicates.
 * <p>
 * This includes the where and having expressions which can be made up of
 * Strings with named parameters or Expression objects.
 * </p>
 * <p>
 * This builds the appropriate bits of where and having clauses and binds the
 * named parameters and expression values into the prepared statement.
 * </p>
 */
public class CQueryPredicates {

	static final Logger logger = Logger.getLogger(CQueryPredicates.class.getName());

	final Binder binder;

	final QueryRequest<?> request;

	final OrmQuery<?> query;

	final Object idValue;

	boolean rowNumberIncluded;

	/**
	 * Flag set if this is a SqlSelect type query.
	 */
	boolean rawSql;

	/**
	 * Named bind parameters.
	 */
	BindParams bindParams;

	/**
	 * Named bind parameters for the having clause.
	 */
	OrderedList havingNamedParams;

	/**
	 * Bind values from the where expressions.
	 */
	ArrayList<Object> whereExprBindValues;

	/**
	 * SQL generated from the where expressions.
	 */
	String whereExprSql;

	/**
	 * SQL generated from where with named parameters.
	 */
	String whereRawSql;

	/**
	 * Bind values for having expression.
	 */
	ArrayList<Object> havingExprBindValues;

	/**
	 * SQL generated from the having expression.
	 */
	String havingExprSql;

	/**
	 * SQL generated from having with named parameters.
	 */
	String havingRawSql;

	String dbHaving;
	
	String logicalHaving;

	/**
	 * The combined where clause (named parameters + expressions)
	 */
	String logicalWhere;

	/**
	 * logicalWhere with property names converted to db columns.
	 */
	String dbWhere;

	/**
	 * The order by clause.
	 */
	String logicalOrderBy;

	String dbOrderBy;

	/**
	 * Includes from where and order by clauses.
	 */
	Set<String> predicateIncludes;
	

	DeployPropertyParser deployParser;

	public CQueryPredicates(Binder binder, QueryRequest<?> request, DeployPropertyParser deployParser) {
		this.binder = binder;
		this.request = request;
		this.query = request.getQuery();
		this.deployParser = deployParser;
		bindParams = query.getBindParams();
		idValue = query.getId();
	}

	public String bind(PreparedStatement pstmt) throws SQLException {

		StringBuilder bindLog = new StringBuilder();

		int index = 0;

		if (idValue != null) {
			// this is a find by id type query...
			index = request.getBeanDescriptor().bindId(pstmt, index, idValue);
			bindLog.append(idValue);
		}

		if (bindParams != null) {
			// bind named and positioned parameters...
			binder.bind(bindParams, index, pstmt, bindLog);
		}

		if (bindParams != null) {
			index = index + bindParams.size();
		}

		if (whereExprBindValues != null) {

			for (int i = 0; i < whereExprBindValues.size(); i++) {
				Object bindValue = whereExprBindValues.get(i);
				binder.bindObject(++index, bindValue, pstmt);
				if (i > 0 || idValue != null) {
					bindLog.append(", ");
				}
				bindLog.append(bindValue);
			}
		}

		if (havingNamedParams != null) {
			// bind named parameters in having...
			bindLog.append(" havingNamed ");
			binder.bind(havingNamedParams.list(), index, pstmt, bindLog);
			index = index + havingNamedParams.size();
		}

		if (havingExprBindValues != null) {
			// bind having expression...
			bindLog.append(" having ");
			for (int i = 0; i < havingExprBindValues.size(); i++) {
				Object bindValue = havingExprBindValues.get(i);
				binder.bindObject(++index, bindValue, pstmt);
				if (i > 0) {
					bindLog.append(", ");
				}
				bindLog.append(bindValue);
			}
		}

		return bindLog.toString();
	}

	private void buildBindHavingRawSql(boolean buildSql) {
		if (buildSql || bindParams != null) {
			// having clause with named parameters...
			havingRawSql = query.getAdditionalHaving();
			if (havingRawSql != null && bindParams != null) {
				// convert and order named parameters if required
				havingNamedParams = BindParamsParser.parseNamedParams(bindParams, havingRawSql);
				havingRawSql = havingNamedParams.getPreparedSql();
			}
		} else {
			// we can skip...
		}
	}

	/**
	 * Convert named parameters into an OrderedList.
	 */
	private void buildBindWhereRawSql(boolean buildSql) {
		if (buildSql || bindParams != null) {
			whereRawSql = buildWhereRawSql();
			if (bindParams != null) {
				// convert and order named parameters if required
				whereRawSql = BindParamsParser.parse(bindParams, whereRawSql);
			}
		} else {
			// we can skip this...
		}
	}

	private String buildWhereRawSql() {
		// this is the where part of a OQL query which
		// may contain bind parameters...
		String whereRaw = query.getWhere();
		if (whereRaw == null) {
			whereRaw = "";
		}
		// add any additional stuff to the where clause
		String additionalWhere = query.getAdditionalWhere();
		if (additionalWhere != null) {
			whereRaw += additionalWhere;
		}
		return whereRaw;
	}

	/**
	 * This combines the sql from named/positioned parameters and expressions.
	 */
	public void prepare(boolean buildSql) {

		buildBindWhereRawSql(buildSql);
		buildBindHavingRawSql(buildSql);

		InternalExpressionList<?> whereExp = query.getWhereExpressions();
		
		DefaultExpressionRequest whereExpReq = new DefaultExpressionRequest(request);
		
		if (whereExp != null) {
			whereExprBindValues = whereExp.buildBindValues(whereExpReq);
			if (buildSql) {
				whereExprSql = whereExp.buildSql(whereExpReq);
			}
		}

		// having expression
		InternalExpressionList<?> havingExpr = query.getHavingExpressions();

		DefaultExpressionRequest havingExpReq = new DefaultExpressionRequest(request);

		if (havingExpr != null) {
			havingExprBindValues = havingExpr.buildBindValues(havingExpReq);
			if (buildSql) {
				havingExprSql = havingExpr.buildSql(havingExpReq);
			}
		}

		if (buildSql) {
			parsePropertiesToDbColumns();
		}
	}
	
	/**
	 * Parse/Convert property names to database columns in the where and order
	 * by clauses etc.
	 */
	private void parsePropertiesToDbColumns() {

		// property name to column name parser...
		if (deployParser == null){
			deployParser = request.getBeanManager().createParser();
		}
		
		logicalWhere = deriveLogicalWhere();
		if (logicalWhere != null) {
			dbWhere = deployParser.parse(logicalWhere);
		}
		
		logicalHaving = deriveLogicalHaving();
		if (logicalHaving != null) {
			dbHaving = deployParser.parse(logicalHaving);
		}

		// order by is dependent on the manyProperty (if there is one)
		logicalOrderBy = deriveOrderByWithMany(request.getManyProperty());
		if (logicalOrderBy != null) {
			dbOrderBy = deployParser.parse(logicalOrderBy);
		}

		predicateIncludes = deployParser.getIncludes();
	}

	/**
	 * Used in logging to the transaction log.
	 */
	public String getLogicalWhere() {
		return logicalWhere;
	}

	private String deriveLogicalWhere() {
		if (whereRawSql == null || whereRawSql.trim().length() == 0) {
			return whereExprSql;

		} else if (whereExprSql == null) {
			return whereRawSql;

		} else {
			return whereRawSql + " and "+ whereExprSql;
		}
	}
	
	private String deriveLogicalHaving() {
		if (havingRawSql == null || havingRawSql.trim().length() == 0) {
			return havingExprSql;

		} else if (havingExprSql == null) {
			return havingRawSql;

		} else {
			return havingRawSql + " and "+ havingExprSql;
		}
	}

	/**
	 * There is a many property so we need to make sure the ordering is
	 * appropriate.
	 */
	private String deriveOrderByWithMany(BeanPropertyAssocMany<?> manyProp) {

		if (manyProp == null) {
			return query.getOrderBy();
		}

		String orderBy = query.getOrderBy();

		BeanDescriptor<?> desc = request.getBeanDescriptor();
		String orderById = desc.getDefaultOrderBy();
		
		if (orderBy == null) {
			orderBy = orderById;
		} 
		
		// check for default ordering on the many property...
		String manyOrderBy = manyProp.getFetchOrderBy();
		if (manyOrderBy != null) {
			// FIXME: Bug: assuming only one column in manyOrderBy
			// Need to prefix many.getName() to all the column names
			// in the order by but not the ASC DESC keywords
			orderBy = orderBy + " , " + manyProp.getName() + "." + manyOrderBy;
		}
		
		if (request.isFindById()) {
			// only one master bean so should be fine...
			return orderBy;
		}
		
		if (orderBy.startsWith(orderById)){
			return orderBy;
		}
		
		// more than one top level row may be returned so
		// we need to make sure their is an order by on the
		// top level first (to ensure master/detail construction).

		int manyPos = orderBy.indexOf(manyProp.getName());
		int idPos = orderBy.indexOf(" "+orderById);
		
		if (manyPos == -1){
			// no ordering of the many... so fine.
			return orderBy;
		}
		if (idPos > -1 && idPos < manyPos){
			// its all ok, id property appears before a many property
		 
		} else {
			if (idPos > manyPos) {
				// there was an error with the order by... 
				String msg = "A Query on [" + desc
						+ "] includes a join to a 'many' association [" + manyProp.getName();
				msg += "] with an incorrect orderBy [" + orderBy + "]. The id property ["+orderById+"]";
				msg += " must come before the many property ["+manyProp.getName()+"] in the orderBy.";
				msg += " Ebean has automatically modified the orderBy clause to do this.";
				
				logger.log(Level.WARNING, msg);
			}		
			
			// the id needs to come before the manyPropName
			orderBy = orderBy.substring(0,manyPos)+orderById+", "+orderBy.substring(manyPos);
		}

		return orderBy;
	}
		
	/**
	 * Return the bind values for the where expression.
	 */
	public ArrayList<Object> getWhereExprBindValues() {
		return whereExprBindValues;
	}

	/**
	 * Return the db column version of the combined where clause.
	 */
	public String getDbHaving() {
		return dbHaving;
	}


	/**
	 * Return the db column version of the combined where clause.
	 */
	public String getDbWhere() {
		return dbWhere;
	}

	/**
	 * Return the db column version of the order by clause.
	 */
	public String getDbOrderBy() {
		return dbOrderBy;
	}

	/**
	 * Return the includes required for the where and order by clause.
	 */
	public Set<String> getPredicateIncludes() {
		return predicateIncludes;
	}

	/**
	 * Returns true if a ROW_NUMBER column is used. e.g. using firstRow with
	 * Oracle.
	 */
	public boolean isRowNumberIncluded() {
		return rowNumberIncluded;
	}

	/**
	 * Set when the sql also includes a ROW_NUMBER column.
	 */
	public void setRowNumberIncluded(boolean isRowNumberIncluded) {
		this.rowNumberIncluded = isRowNumberIncluded;
	}

	/**
	 * The where sql with named bind parameters converted to ?.
	 */
	public String getWhereRawSql() {
		return whereRawSql;
	}

	/**
	 * The where sql from the expression objects.
	 */
	public String getWhereExpressionSql() {
		return whereExprSql;
	}

	/**
	 * The having sql with named bind parameters converted to ?.
	 */
	public String getHavingRawSql() {
		return havingRawSql;
	}

	/**
	 * The having sql from the expression objects.
	 */
	public String getHavingExpressionSql() {
		return havingExprSql;
	}

	public String getLogWhereSql() {
		if (rawSql) {
			return "";
		} else {
			String logPred = getLogicalWhere();
			if (logPred == null) {
				return "";
			} else if (logPred.length() > 400) {
				logPred = logPred.substring(0, 400) + " ...";
			}
			return logPred;
		}
	}
}

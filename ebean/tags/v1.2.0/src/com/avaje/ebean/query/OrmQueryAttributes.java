package com.avaje.ebean.query;

import java.io.Serializable;

/**
 * Extra attributes namely maxRows, firstRows, where and orderBy.
 */
public class OrmQueryAttributes implements Serializable {
	
	private static final long serialVersionUID = 1495756503548932780L;

	int maxRows;

	int firstRow;

	String where;

	String orderBy;
	
	/**
	 * Return a copy of the OrmQueryAttributes.
	 */
	public OrmQueryAttributes copy() {
		OrmQueryAttributes copy = new OrmQueryAttributes();
		copy.maxRows = maxRows;
		copy.firstRow = firstRow;
		copy.where = where;
		copy.orderBy = orderBy;
		return copy;
	}
	
	public int queryPlanHash() {
		int hc = (firstRow == 0 ? 0 : firstRow);
		hc = hc * 31 + (maxRows == 0 ? 0 : maxRows);
		hc = hc * 31 + (orderBy == null ? 0 : orderBy.hashCode());
		hc = hc * 31 + (where == null ? 0 : where.hashCode());
		return hc;
	}
		
	public void reset() {
		maxRows = 0;
		firstRow = 0;
		where = null;
		orderBy = null;
	}
	
	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String where) {
		this.where = where;
	}

	public int getFirstRow() {
		return firstRow;
	}

	public void setFirstRow(int firstRow) {
		this.firstRow = firstRow;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * Return true if firstRow or maxRows has been set.
	 */
	public boolean hasMaxRowsOrFirstRow() {
		return maxRows > 0 || firstRow > 0;
	}
}
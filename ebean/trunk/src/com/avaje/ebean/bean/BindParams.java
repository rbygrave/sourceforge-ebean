/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Parameters used for binding to a statement.
 * <p>
 * Used by FindByNativeSql and UpdateSql to support ordered and named
 * parameters. Note that you can use either ordered OR named parameters.
 * </p>
 */
public class BindParams implements Serializable {

	static final long serialVersionUID = 4541081933302086285L;

	ArrayList<Param> positionedParameters = new ArrayList<Param>();

	HashMap<String, Param> namedParameters = new HashMap<String, Param>();

	/**
	 * This is the sql. For named parameters this is the sql after the named
	 * parameters have been replaced with question mark place holders and the
	 * parameters have been ordered by addNamedParamInOrder().
	 */
	String preparedSql;

	/**
	 * Return a deep copy of the BindParams.
	 */
	public BindParams copy() {
		BindParams copy = new BindParams();
		for (Param p : positionedParameters) {
			copy.positionedParameters.add(p.copy());
		}
		Iterator<Entry<String, Param>> it = namedParameters.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Param> entry = (Map.Entry<String, Param>) it.next();
			copy.namedParameters.put(entry.getKey(), entry.getValue().copy());
		}
		return copy;
	}
	
	public int queryBindHash() {
		int hc = namedParameters.hashCode();
		for (int i = 0; i < positionedParameters.size(); i++) {
			hc = hc * 31 + positionedParameters.get(i).hashCode();
		}
		return hc;
	}
	
	public int hashCode() {
		int hc = getClass().hashCode();
		hc = hc * 31 + namedParameters.hashCode();
		for (int i = 0; i < positionedParameters.size(); i++) {
			hc = hc * 31 + positionedParameters.get(i).hashCode();
		}
		hc = hc * 31 + (preparedSql == null ? 0 : preparedSql.hashCode());
		return hc;
	}

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o instanceof BindParams) {
			return hashCode() == o.hashCode();
		}
		return false;
	}

	/**
	 * Return true if there are no bind parameters.
	 */
	public boolean isEmpty() {
		return positionedParameters.isEmpty() && namedParameters.isEmpty();
	}

	public int size() {
		return positionedParameters.size();
	}

	/**
	 * Return true if named parameters are being used and they have not yet been
	 * ordered. The sql needs to be prepared (named replaced with ?) and the
	 * parameters ordered.
	 */
	public boolean requiresNamedParamsPrepare() {
		return !namedParameters.isEmpty() && positionedParameters.isEmpty();
	}

	/**
	 * Set a null parameter using position.
	 */
	public void setNullParameter(int position, int jdbcType) {
		Param p = getParam(position);
		p.setInNullType(jdbcType);
	}

	/**
	 * Set an In Out parameter using position.
	 */
	public void setParameter(int position, Object value, int outType) {
		Param p = getParam(position);
		p.setInValue(value);
		p.setOutType(outType);
	}

	/**
	 * Using position set the In value of a parameter. Note that for nulls you
	 * must use setNullParameter.
	 */
	public void setParameter(int position, Object value) {
		Param p = getParam(position);
		p.setInValue(value);
	}

	/**
	 * Register the parameter as an Out parameter using position.
	 */
	public void registerOut(int position, int outType) {
		Param p = getParam(position);
		p.setOutType(outType);
	}

	private Param getParam(String name) {
		Param p = (Param) namedParameters.get(name);
		if (p == null) {
			p = new Param();
			namedParameters.put(name, p);
		}
		return p;
	}

	private Param getParam(int position) {
		int more = position - positionedParameters.size();
		if (more > 0) {
			for (int i = 0; i < more; i++) {
				positionedParameters.add(new Param());
			}
		}
		return (Param) positionedParameters.get(position - 1);
	}

	/**
	 * Set a named In Out parameter.
	 */
	public void setParameter(String name, Object value, int outType) {
		Param p = getParam(name);
		p.setInValue(value);
		p.setOutType(outType);
	}

	/**
	 * Set a named In parameter that is null.
	 */
	public void setNullParameter(String name, int jdbcType) {
		Param p = getParam(name);
		p.setInNullType(jdbcType);
	}

	/**
	 * Set a named In parameter that is not null.
	 */
	public void setParameter(String name, Object value) {
		Param p = getParam(name);
		p.setInValue(value);
	}

	/**
	 * Register the named parameter as an Out parameter.
	 */
	public void registerOut(String name, int outType) {
		Param p = getParam(name);
		p.setOutType(outType);
	}

	/**
	 * Return the Parameter for a given position.
	 */
	public Param getParameter(int position) {
		// Used to read Out value by CallableSql
		return getParam(position);
	}

	/**
	 * Return the named parameter.
	 */
	public Param getParameter(String name) {
		return getParam(name);
	}

	/**
	 * Return the values of ordered parameters.
	 */
	public List<Param> positionedParameters() {
		return positionedParameters;
	}

	/**
	 * Set the sql with named parameters replaced with place holder ?.
	 */
	public void setPreparedSql(String preparedSql) {
		this.preparedSql = preparedSql;
	}

	/**
	 * Return the sql with ? place holders (named parameters have been processed
	 * and ordered).
	 */
	public String getPreparedSql() {
		return preparedSql;
	}

	/**
	 * The bind parameters in the correct binding order.
	 * <p>
	 * This is the result of converting sql with named parameters
	 * into sql with ? and ordered parameters.
	 * </p>
	 */
	public static final class OrderedList {
		
		final List<Param> paramList;
		
		final StringBuilder preparedSql;

		public OrderedList() {
			this(new ArrayList<Param>());
		}
		
		public OrderedList(List<Param> paramList) {
			this.paramList = paramList;
			this.preparedSql = new StringBuilder();
		}
		
		/**
		 * Add a parameter in the correct binding order.
		 */
		public void add(Param param) {
			paramList.add(param);
		}
		
		/**
		 * Return the number of bind parameters in this list.
		 */
		public int size() {
			return paramList.size();
		}
		
		/**
		 * Returns the ordered list of bind parameters.
		 */
		public List<Param> list() {
			return paramList;
		}
		
		/**
		 * Append parsedSql that has named parameters converted into ?.
		 */
		public void appendSql(String parsedSql) {
			preparedSql.append(parsedSql);
		}
		
		public String getPreparedSql() {
			return preparedSql.toString();
		}
	}
	
	/**
	 * A In Out capable parameter for the CallableStatement.
	 */
	public static final class Param {

		boolean isInParam;

		boolean isOutParam;

		int type;

		Object inValue;

		Object outValue;

		int textLocation;

		/**
		 * Construct a Parameter.
		 */
		public Param() {
		}

		/**
		 * Create a deep copy of the Param.
		 */
		public Param copy() {
			Param copy = new Param();
			copy.isInParam = isInParam;
			copy.isOutParam = isOutParam;
			copy.type = type;
			copy.inValue = inValue;
			copy.outValue = outValue;
			return copy;
		}
		
		public int hashCode() {
			int hc = getClass().hashCode();
			hc = hc * 31 + (isInParam ? 0 : 1);
			hc = hc * 31 + (isOutParam ? 0 : 1);
			hc = hc * 31 + (type);
			hc = hc * 31 + (inValue == null ? 0 : inValue.hashCode());			
			return hc;
		}

		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (o instanceof Param) {
				return hashCode() == o.hashCode();
			}
			return false;
		}
		
		/**
		 * Return true if this is an In parameter that needs to be bound before
		 * execution.
		 */
		public boolean isInParam() {
			return isInParam;
		}

		/**
		 * Return true if this is an out parameter that needs to be registered
		 * before execution.
		 */
		public boolean isOutParam() {
			return isOutParam;
		}

		/**
		 * Return the jdbc type of this parameter. Used for registering Out
		 * parameters and setting NULL In parameters.
		 */
		public int getType() {
			return type;
		}

		/**
		 * Set the Out parameter type.
		 */
		public void setOutType(int type) {
			this.type = type;
			this.isOutParam = true;
		}

		/**
		 * Set the In value.
		 */
		public void setInValue(Object in) {
			this.inValue = in;
			this.isInParam = true;
		}

		/**
		 * Specify that the In parameter is NULL and the specific type that it
		 * is.
		 */
		public void setInNullType(int type) {
			this.type = type;
			this.inValue = null;
			this.isInParam = true;
		}

		/**
		 * Return the OUT value that was retrieved. This value is set after
		 * CallableStatement was executed.
		 */
		public Object getOutValue() {
			return outValue;
		}

		/**
		 * Return the In value. If this is null, then the type should be used to
		 * specify the type of the null.
		 */
		public Object getInValue() {
			return inValue;
		}

		/**
		 * Set the OUT value returned by a CallableStatement after it has
		 * executed.
		 */
		public void setOutValue(Object out) {
			this.outValue = out;
		}

		/**
		 * Return the location this parameter was found in the sql text.
		 */
		public int getTextLocation() {
			return textLocation;
		}

		/**
		 * Set the location in the sql text this parameter was located. This is
		 * used to control order for named parameters.
		 */
		public void setTextLocation(int textLocation) {
			this.textLocation = textLocation;
		}

	}
}

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
package com.avaje.ebeaninternal.server.el;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.lib.util.StringHelper;
import com.avaje.ebeaninternal.server.type.ScalarType;


/**
 * A ElGetValue based on a chain of properties.
 * <p>
 * Used to get the value for an compound expression like customer.name or
 * customer.shippingAddress.city etc.
 * </p>
 * <p>
 * Note that if any element in the chain returns null, then null is returned and
 * no further processing of the chain occurs.
 * </p>
 */
public class ElPropertyChain implements ElPropertyValue {

	private final String prefix;

	private final String placeHolder;
    private final String placeHolderEncrypted;
	
	private final String name;
	
	private final String expression;

	private final boolean containsMany;
	
	private final ElPropertyValue[] chain;

	private final boolean assocOneId;
	private final int last;
	private final BeanProperty lastBeanProperty;
	private final ScalarType<?> scalarType;
	
	private final ElPropertyValue lastElPropertyValue;
	
	public ElPropertyChain(boolean containsMany, boolean embedded, String expression, ElPropertyValue[] chain) {
		
		this.containsMany = containsMany;
		this.chain = chain;
		this.expression = expression;
		int dotPos = expression.lastIndexOf('.');
		if (dotPos > -1){
			this.name = expression.substring(dotPos+1);
			if (embedded){
				int embPos = expression.lastIndexOf('.',dotPos-1);				
				this.prefix = embPos == -1 ? null : expression.substring(0, embPos);
				
			} else {
				this.prefix = expression.substring(0, dotPos);
			}
		} else {
			this.prefix = null;
			this.name = expression;
		}		

		this.assocOneId = chain[chain.length-1].isAssocOneId();
		
		this.last = chain.length-1;
		this.lastBeanProperty = chain[chain.length-1].getBeanProperty();
		if (lastBeanProperty != null){
		    this.scalarType = lastBeanProperty.getScalarType();
		} else {
		    // case for nested compound type (non-scalar)
		    this.scalarType = null;
		}
		this.lastElPropertyValue = chain[chain.length-1];
		this.placeHolder = getElPlaceHolder(prefix, lastElPropertyValue, false);
        this.placeHolderEncrypted = getElPlaceHolder(prefix, lastElPropertyValue, true);
	}

	private String getElPlaceHolder(String prefix, ElPropertyValue lastElPropertyValue, boolean encrypted) {
	    if (prefix == null){
	        return lastElPropertyValue.getElPlaceholder(encrypted);
	    }
	    
	    String el = lastElPropertyValue.getElPlaceholder(encrypted);
	    
	    if (!el.contains("${}")){
	        // typically a secondary table property
            return StringHelper.replaceString(el, "${", "${"+prefix+".");
	    } else {
	        return StringHelper.replaceString(el, ROOT_ELPREFIX, "${"+prefix+"}");
	    }
	}
		
	/**
	 * Full ElGetValue support.
	 */
	public boolean isDeployOnly() {
		return false;
	}

	
	public boolean containsMany() {
		return containsMany;
	}

	public String getElPrefix() {
		return prefix;
	}

	public String getName() {
		return name;
	}
	
	public String getElPlaceholder(boolean encrypted) {
	    return encrypted ? placeHolderEncrypted : placeHolder;
	}
	
	public boolean isDbEncrypted() {
        return lastElPropertyValue.isDbEncrypted();	    
	}
	
	public boolean isLocalEncrypted() {
        return lastElPropertyValue.isLocalEncrypted();
    }

    public Object[] getAssocOneIdValues(Object bean) {
		// Don't navigate the object graph as bean 
		// is assumed to be the appropriate type
		return lastElPropertyValue.getAssocOneIdValues(bean);
	}

	public String getAssocOneIdExpr(String prefix, String operator) {
		return lastElPropertyValue.getAssocOneIdExpr(expression, operator);
	}

	public boolean isAssocOneId() {
		return assocOneId;
	}

	public String getDbColumn() {
		return lastElPropertyValue.getDbColumn();
	}
	
	public BeanProperty getBeanProperty() {
		return lastBeanProperty; 
	}

	
	public boolean isDateTimeCapable() {
		return scalarType != null && scalarType.isDateTimeCapable();
	}

	public Object parseDateTime(long systemTimeMillis) {
		return scalarType.parseDateTime(systemTimeMillis);
	}

	public StringParser getStringParser() {
		return scalarType;
	}
	
	public Object elConvertType(Object value){
		// just convert using the last one in the chain
		return lastElPropertyValue.elConvertType(value);
	}
	
	public Object elGetValue(Object bean) {

		for (int i = 0; i < chain.length; i++) {
			bean = chain[i].elGetValue(bean);
			if (bean == null) {
				return null;
			}
		}

		return bean;
	}

	public Object elGetReference(Object bean) {
		
		Object prevBean = bean;
		for (int i = 0; i < last; i++) {
			// always return non null prevBean
			prevBean = chain[i].elGetReference(prevBean);
		}
		// try the last step in the chain
		bean = chain[last].elGetValue(prevBean);
		
		return bean;
	}
	

	public void elSetLoaded(Object bean) {
		
		for (int i = 0; i < last; i++) {
			bean = chain[i].elGetValue(bean);
			if (bean == null){
				break;
			}
		}				
		if (bean != null){
			((EntityBean)bean)._ebean_getIntercept().setLoaded();
		}
	}
	
	public void elSetReference(Object bean) {

		for (int i = 0; i < last; i++) {
			bean = chain[i].elGetValue(bean);
			if (bean == null){
				break;
			}
		}				
		if (bean != null){
			((EntityBean)bean)._ebean_getIntercept().setReference();
		}
	}
	
	public void elSetValue(Object bean, Object value, boolean populate, boolean reference){

		Object prevBean = bean;
		if (populate){
			for (int i = 0; i < last; i++) {
				// always return non null prevBean
				prevBean = chain[i].elGetReference(prevBean);
			}	
		} else {
			for (int i = 0; i < last; i++) {
				// always return non null prevBean
				prevBean = chain[i].elGetValue(prevBean);
				if (prevBean == null){
					break;
				}
			}				
		}
		if (prevBean != null){
		    if (lastBeanProperty != null){
		        // last chain element maps to a real scalar property
    			lastBeanProperty.setValueIntercept(prevBean, value);
    			if (reference){
    				((EntityBean)prevBean)._ebean_getIntercept().setReference();
    			}
		    } else {
		        // a non-scalar property of a Compound value object
		        lastElPropertyValue.elSetValue(prevBean, value, populate, reference);
		    }
		}
	}

	
}

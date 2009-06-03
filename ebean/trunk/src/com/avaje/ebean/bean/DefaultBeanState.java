package com.avaje.ebean.bean;

import java.beans.PropertyChangeListener;
import java.util.Set;

import com.avaje.ebean.BeanState;

public class DefaultBeanState implements BeanState {

	final EntityBean entityBean;
	
	final EntityBeanIntercept intercept;
	
	public DefaultBeanState(EntityBean  entityBean){
		this.entityBean = entityBean;
		this.intercept = entityBean._ebean_getIntercept();
	}
		
	public boolean isNew() {
		return intercept.isNew();
	}

	public boolean isNewOrDirty() {
		return intercept.isNewOrDirty();
	}

	public boolean isDirty() {
		return intercept.isDirty();
	}
	
	public Set<String> getLoadedProps() {
		return intercept.getLoadedProps();
	}
	
	public boolean isReadOnly() {
		return intercept.isReadOnly();
	}
	
	public void setReadOnly(boolean readOnly){
		intercept.setReadOnly(readOnly);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		entityBean.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		entityBean.removePropertyChangeListener(listener);
	}
}

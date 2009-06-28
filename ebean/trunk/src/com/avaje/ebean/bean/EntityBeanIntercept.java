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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Set;

import javax.persistence.PersistenceException;

import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.io.SerializeControl;

/**
 * This is the object added to every entity bean using byte code enhancement.
 * <p>
 * This provides the mechanisms to support deferred fetching of reference beans
 * and oldValues generation for concurrency checking.
 * </p>
 */
public class EntityBeanIntercept implements Cloneable, Serializable {

	private static final long serialVersionUID = -3664031775464862645L;
	
	transient NodeUsageCollector nodeUsageCollector;

	/**
	 * The actual entity bean that 'owns' this intercept.
	 */
	private EntityBean owner;

	/**
	 * The parent bean by relationship (1-1 or 1-M).
	 */
	private Object parentBean;

	/**
	 * true if the bean properties have been loaded. false if it is a reference
	 * bean (will lazy load etc).
	 */
	private boolean loaded;

	/**
	 * Set true when loaded or reference. 
	 * Used to bypass interception when created by user code.
	 */
	private boolean intercepting;

	/**
	 * If true calling setters throws an exception.
	 */
	private boolean readOnly;

	/**
	 * The bean as it was before it was modified. Null if no non-transient
	 * setters have been called.
	 */
	private Object oldValues;

	/**
	 * Used when a bean is partially filled.
	 */
	private Set<String> loadedProps;

//	/**
//	 * The EbeanServer this bean came from.
//	 */
//	protected String serverName;

	private InternalEbean internalEbean;
	
	private String lazyLoadProperty;

	transient private PropertyChangeSupport pcs;
	
	/**
	 * Create a intercept with a given entity.
	 * <p>
	 * Refer to agent ProxyConstructor.
	 * </p>
	 */
	public EntityBeanIntercept(Object owner) {
		this.owner = (EntityBean)owner;
	}
	
	/**
	 * Return the 'owning' entity bean.
	 */
	public EntityBean getOwner() {
		return owner;
	}
	
	public String toString() {
		if (!loaded) {
			return "Reference...";
		}
		return "OldValues: " + oldValues;
	}
	
	/**
	 * Add a property change listener for this entity bean.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener){
		if (pcs == null){
			pcs = new PropertyChangeSupport(owner);
		}
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Add a property change listener for this entity bean for a specific property.
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener){
		if (pcs == null){
			pcs = new PropertyChangeSupport(owner);
		}
		pcs.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Remove a property change listener for this entity bean.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener){
		if (pcs != null){
			pcs.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Remove a property change listener for this entity bean for a specific property.
	 */
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener){
		if (pcs != null){
			pcs.removePropertyChangeListener(propertyName, listener);
		}
	}
	
	/**
	 * Turn on profile collection.
	 */
	public void setNodeUsageCollector(NodeUsageCollector usageCollector) {
		this.nodeUsageCollector = usageCollector;
	}

//	/**
//	 * Return the name of the associated EbeanServer.
//	 */
//	public String getServerName() {
//		return serverName;
//	}
//
//	/**
//	 * Set the name of the associated EbeanServer.
//	 */
//	public void setServerName(String serverName) {
//		this.serverName = serverName;
//	}
	
	public void setInternalEbean(InternalEbean internalEbean) {
		this.internalEbean = internalEbean;
	}

	/**
	 * Return the parent bean (by relationship).
	 */
	public Object getParentBean() {
		return parentBean;
	}

	/**
	 * Special case for a OneToOne, Set the parent bean (by relationship). This
	 * is the owner of a 1-1.
	 */
	public void setParentBean(Object parentBean) {
		this.parentBean = parentBean;
	}

	/**
	 * Return true if this bean has been directly modified
	 * (it has oldValues) or if any embedded beans are either
	 * new or dirty (and hence need saving).
	 */
	public boolean isDirty() {
		if (oldValues != null){
			return true;
		}
		// need to check all the embedded beans
		return owner._ebean_isEmbeddedNewOrDirty();
	}

	/**
	 * Return true if this entity bean is new and not yet saved.
	 */
	public boolean isNew() {
		return !intercepting && !loaded;
	}
	
	/**
	 * Return true if the entity bean is new or dirty (and should be saved).
	 */
	public boolean isNewOrDirty() {
		return isNew() || isDirty();
	}
	
	/**
	 * Return true if the entity is a reference.
	 */
	public boolean isReference() {
		return intercepting && !loaded;
	}

	/**
	 * Set this as a reference object.
	 */
	public void setReference() {
		this.loaded = false;
		this.intercepting = true;
	}
	
	/**
	 * Return the old values used for ConcurrencyMode.ALL.
	 */
	public Object getOldValues() {
		return oldValues;
	}

	/**
	 * Return true if the bean should be treated as readOnly. If a setter method
	 * is called when it is readOnly an Exception is thrown.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Set the readOnly status. If readOnly then calls to setter methods through
	 * an exception.
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	/**
	 * Return true if the bean currently has interception on.
	 * <p>
	 * With interception on the bean will invoke lazy loading and dirty checking.
	 * </p>
	 */
	public boolean isIntercepting() {
		return intercepting;
	}

	/**
	 * Turn interception off or on.
	 * <p>
	 * This is to support custom serialization mechanisms that just read all
	 * the properties on the bean.
	 * </p>
	 * 
	 */
	public void setIntercepting(boolean intercepting) {
		this.intercepting = intercepting;
	}

	/**
	 * Return true if the entity has been loaded.
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Set the loaded state to true.
	 * <p>
	 * Calls to setter methods after the bean is loaded can result in 'Old
	 * Values' being created to support ConcurrencyMode.ALL
	 * </p>
	 * <p>
	 * Worth noting that this is also set after a insert/update. By doing so
	 * it 'resets' the bean for making further changes and saving again.
	 * </p>
	 */
	public void setLoaded() {
		this.loaded = true;
		this.oldValues = null;
		this.intercepting = true;
		this.owner._ebean_setEmbeddedLoaded();
	}

	/**
	 * Set the loaded status for the embedded bean.
	 */
	public void setEmbeddedLoaded(Object embeddedBean) {
		if (embeddedBean instanceof EntityBean){
			EntityBean eb = (EntityBean)embeddedBean;
			eb._ebean_getIntercept().setLoaded();
		}
	}
	
	/**
	 * Return true if the embedded bean is new or dirty and hence needs saving.
	 */
	public boolean isEmbeddedNewOrDirty(Object embeddedBean) {
		
		if (embeddedBean == null){
			// if it was previously set then the owning bean would 
			// have oldValues containing the previous embedded bean
			return false;
		}
		if (embeddedBean instanceof EntityBean){
			return ((EntityBean)embeddedBean)._ebean_getIntercept().isNewOrDirty();
			
		} else {
			// non-enhanced so must assume it is new and needs to be saved
			return true;
		}
	}
	
	/**
	 * Set the property names for a partially loaded bean.
	 * 
	 * @param loadedPropertyNames
	 *            the names of the loaded properties
	 */
	public void setLoadedProps(Set<String> loadedPropertyNames) {
		this.loadedProps = loadedPropertyNames;
	}

	/**
	 * Return the set of property names for a partially loaded bean.
	 */
	public Set<String> getLoadedProps() {
		return loadedProps;
	}
	
	/**
	 * Return the property read or write that triggered the lazy load.
	 */
	public String getLazyLoadProperty() {
		return lazyLoadProperty;
	}

	/**
	 * Load the bean when it is a reference.
	 */
	protected void loadBean(String loadProperty) {

		lazyLoadProperty = loadProperty;

		if (nodeUsageCollector != null){
			nodeUsageCollector.setLoadProperty(lazyLoadProperty);
		}

		//InternalEbean eb = (InternalEbean)Ebean.getServer(serverName);
		internalEbean.lazyLoadBean(owner, nodeUsageCollector);

		// the refresh always loads all properties
		loadedProps = null;
		loaded = true;
	}

	/**
	 * Create a copy of the bean as it is now. This is the original or 'old
	 * values' prior to any modification. This is used to perform concurrency
	 * testing.
	 */
	protected void createOldValues() {
		
		oldValues = owner._ebean_createCopy();
		
		if (nodeUsageCollector != null){
			nodeUsageCollector.setModified();
		}
	}

	/**
	 * This is ONLY used for subclass entity beans.
	 * <p>
	 * This is not used when entity bean classes are enhanced 
	 * via javaagent or ant etc - only when a subclass is generated.
	 * </p>
	 * Returns a Serializable instance that is either the 'byte code generated'
	 * object or a 'Vanilla' copy of this bean depending on
	 * SerializeControl.isVanillaBeans().
	 */
	public Object writeReplaceIntercept() throws ObjectStreamException {

		if (!SerializeControl.isVanillaBeans()) {
			return owner;
		}

		// creates a plain vanilla object and
		// copies the values from the owner
		return owner._ebean_createCopy();
	}

	/**
	 * Helper method to check if two objects are equal.
	 */
	@SuppressWarnings("unchecked")
	protected boolean areEqual(Object obj1, Object obj2) {
		if (obj1 == null) {
			return (obj2 == null);
		}
		if (obj2 == null) {
			return false;
		}
		if (obj1 == obj2) {
			return true;
		}
		if (obj1 instanceof BigDecimal) {
			// Use comparable for BigDecimal as equals
			// uses scale in comparison...
			if (obj2 instanceof BigDecimal) {
				Comparable com1 = (Comparable) obj1;
				return (com1.compareTo(obj2) == 0);

			} else {
				return false;
			}

		} 
		if (obj1 instanceof URL){
			// use the string format to determine if dirty
			return obj1.toString().equals(obj2.toString());
		}
		return obj1.equals(obj2);
	}

	/**
	 * Method that is called prior to a getter method on the actual entity.
	 * <p>
	 * This checks if the bean is a reference and should be loaded.
	 * </p>
	 */
	public void preGetter(String propertyName) {
		if (!intercepting){
			return;
		}
		if (nodeUsageCollector != null && loaded){
			nodeUsageCollector.addUsed(propertyName);
		}
		
		if (!loaded) {
			loadBean(propertyName);
		} else if (loadedProps != null && !loadedProps.contains(propertyName)) {
			loadBean(propertyName);
		}
	}

	/**
	 * Called for "enhancement" postSetter processing.
	 * This is around a PUTFIELD so no need to check the newValue afterwards.
	 */
	public void postSetter(PropertyChangeEvent event){
		if (pcs != null && event != null){
			pcs.firePropertyChange(event);
		}
	}

	/**
	 * Called for "subclassed" postSetter processing. 
	 * Here the newValue has to be re-fetched (and passed into this method)
	 * in case there is code inside the setter that further mutates the value.
	 */
	public void postSetter(PropertyChangeEvent event, Object newValue){
		if (pcs != null && event != null){
			if (newValue != null && newValue.equals(event.getNewValue())){
				pcs.firePropertyChange(event);
			} else {
				pcs.firePropertyChange(event.getPropertyName(), event.getOldValue(), newValue);
			}
		}
	}

	
	/**
	 * Return true if a modification check should be performed. That is, return
	 * true if we need to compare the new and old values to see if they have
	 * changed.
	 */
	public boolean preSetterIsModifyCheck() {
		if (!intercepting){
			return false;
		}
		if (readOnly) {
			throw new PersistenceException("This bean is readOnly");
		}
		return (loaded && oldValues == null);
	}

	/**
	 * OneToMany and ManyToMany don't have any interception so just check for PropertyChangeSupport.
	 */
	public PropertyChangeEvent preSetterMany(boolean interceptField, String propertyName, Object oldValue, Object newValue) {
				
		// skip setter interception on many's
		if (pcs != null){
			return new PropertyChangeEvent(owner, propertyName, oldValue, newValue);
		} else {
			return null;
		}		
	}

	
	/**
	 * Check to see if the values are not equal. If they are not equal then
	 * create the old values for use with ConcurrencyMode.ALL.
	 */
	public PropertyChangeEvent preSetter(boolean interceptField, String propertyName, Object oldValue, Object newValue) {
		
		if (pcs == null && (!interceptField || !preSetterIsModifyCheck())){
			// skip propertyChangeSupport && creating oldValues when value has changed
			return null;
		}
		
		if (newValue instanceof BeanCollection<?> || oldValue instanceof BeanCollection<?>){
			throw new RuntimeException("Should not get this now");
//			// skip setter interception on many's
//			if (pcs != null){
//				return new PropertyChangeEvent(owner, propertyName, oldValue, newValue);
//			} else {
//				return null;
//			}
		}
		
		boolean changed = !areEqual(oldValue, newValue);

		if (interceptField && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, oldValue, newValue);
		}
		
		return null; 
	}

	/**
	 * Check for primitive boolean.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, boolean oldValue, boolean newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}

		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
		}
		
		return null; 
	}
	
	/**
	 * Check for primitive int.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, int oldValue, int newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * long.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, long oldValue, long newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Long.valueOf(oldValue), Long.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * double.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, double oldValue, double newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Double.valueOf(oldValue), Double.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * float.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, float oldValue, float newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Float.valueOf(oldValue), Float.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * short.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, short oldValue, short newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Short.valueOf(oldValue), Short.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * char.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, char oldValue, char newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Character.valueOf(oldValue), Character.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * char.
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, byte oldValue, byte newValue) {
		
		boolean changed = oldValue != newValue;

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, Byte.valueOf(oldValue), Byte.valueOf(newValue));
		}
		return null; 
	}

	/**
	 * char[].
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, char[] oldValue, char[] newValue) {
		
		boolean changed = !areEqual(oldValue, newValue);

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, oldValue, newValue);
		}
		return null; 
	}

	/**
	 * byte[].
	 */
	public PropertyChangeEvent preSetter(boolean intercept, String propertyName, byte[] oldValue, byte[] newValue) {
		
		boolean changed = !areEqual(oldValue, newValue);

		if (intercept && changed && preSetterIsModifyCheck()){
			createOldValues();			
		}
		
		if (changed && pcs != null){
			return new PropertyChangeEvent(owner, propertyName, oldValue, newValue);
		}
		return null; 
	}

}

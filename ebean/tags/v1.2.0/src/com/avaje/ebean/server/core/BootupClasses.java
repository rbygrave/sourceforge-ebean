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
package com.avaje.ebean.server.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.avaje.ebean.bean.BeanPersistController;
import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanPersistListener;
import com.avaje.ebean.server.type.ScalarType;

/**
 * Interesting classes for a EbeanServer such as Embeddable, Entity,
 * ScalarTypes, Finders, Listeners and Controllers.
 */
public class BootupClasses {

	ArrayList<Class<?>> embeddableList = new ArrayList<Class<?>>();

	ArrayList<Class<?>> entityList = new ArrayList<Class<?>>();

	ArrayList<Class<?>> scalarTypeList = new ArrayList<Class<?>>();

	ArrayList<Class<?>> beanControllerList = new ArrayList<Class<?>>();

	ArrayList<Class<?>> beanFinderList = new ArrayList<Class<?>>();

	ArrayList<Class<?>> beanListenerList = new ArrayList<Class<?>>();

	public BootupClasses(){
	}
	
	public BootupClasses(ArrayList<Class<?>> list){
		process(list.iterator());
	}
	
	private BootupClasses(BootupClasses parent){
		this.embeddableList.addAll(parent.embeddableList);
		this.entityList.addAll(parent.entityList);
		this.scalarTypeList.addAll(parent.scalarTypeList);
		this.beanControllerList.addAll(parent.beanControllerList);
		this.beanFinderList.addAll(parent.beanFinderList);
		this.beanListenerList.addAll(parent.beanListenerList);
	}
	
	private void process(Iterator<Class<?>> it){
		while (it.hasNext()) {
			Class<?> cls = it.next();
			isMatch(cls);
		}
	}
	
	/**
	 * Create a copy of this object so that classes can be added to it.
	 */
	public BootupClasses createCopy() {
		return new BootupClasses(this);
	}
	
	/**
	 * Return the list of Embeddable classes.
	 */
	public ArrayList<Class<?>> getEmbeddables() {
		return embeddableList;
	}

	/**
	 * Return the list of entity classes.
	 */
	public ArrayList<Class<?>> getEntities() {
		return entityList;
	}

	/**
	 * Return the list of ScalarTypes found.
	 */
	public ArrayList<Class<?>> getScalarTypes() {
		return scalarTypeList;
	}

	/**
	 * Return the list of BeanControllers found.
	 */
	public ArrayList<Class<?>> getBeanControllers() {
		return beanControllerList;
	}

	/**
	 * Return the list of BeanFinders found.
	 */
	public ArrayList<Class<?>> getBeanFinders() {
		return beanFinderList;
	}

	/**
	 * Return the list of BeanListeners found.
	 */
	public ArrayList<Class<?>> getBeanListeners() {
		return beanListenerList;
	}

	public void add(Iterator<Class<?>> it) {
		while (it.hasNext()) {
			Class<?> clazz = it.next();
			isMatch(clazz);
		}
	}
	
	public boolean isMatch(Class<?> cls) {

		if (isEmbeddable(cls)) {
			embeddableList.add(cls);

		} else if (isEntity(cls)) {
			entityList.add(cls);

		} else if (isInterestingInterface(cls)) {
			return true;

		} else {
			return false;
		}

		return true;
	}

	/**
	 * Look for interesting interfaces.
	 * <p>
	 * This includes ScalarType, BeanController, BeanFinder and BeanListener.
	 * </p>
	 */
	private boolean isInterestingInterface(Class<?> cls) {

		boolean interesting = false;

		if (BeanPersistController.class.isAssignableFrom(cls)){
			beanControllerList.add(cls);
			interesting = true;
		}
		
		if (ScalarType.class.isAssignableFrom(cls)) {
			scalarTypeList.add(cls);
			interesting = true;
		}
		
		if (BeanFinder.class.isAssignableFrom(cls)) {
			beanFinderList.add(cls);
			interesting = true;
		}

		if (BeanPersistListener.class.isAssignableFrom(cls)) {
			beanListenerList.add(cls);
			interesting = true;
		}

		return interesting;
	}

	private boolean isEntity(Class<?> cls) {

		Annotation ann = cls.getAnnotation(Entity.class);
		if (ann != null) {
			return true;
		}
		ann = cls.getAnnotation(Table.class);
		if (ann != null) {
			return true;
		}
		return false;
	}

	private boolean isEmbeddable(Class<?> cls) {

		Annotation ann = cls.getAnnotation(Embeddable.class);
		if (ann != null) {
			return true;
		}
		return false;
	}
}

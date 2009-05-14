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
package com.avaje.ebean.server.deploy.parse;

import java.lang.annotation.Annotation;
import java.sql.Types;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Inheritance;

import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Builds the InheritInfo deployment information.
 */
public class DeployInheritInfoBuilder {

	final Map<Class<?>, DeployInheritInfo> deployMap = new LinkedHashMap<Class<?>, DeployInheritInfo>();

	final Map<Class<?>, InheritInfo> finalMap = new LinkedHashMap<Class<?>, InheritInfo>();

	final PluginProperties properties;

	/**
	 * Create the InheritInfoDeploy.
	 */
	public DeployInheritInfoBuilder(PluginProperties properties) {
		this.properties = properties;
		initialise();
	}

	public void process(DeployBeanDescriptor<?> desc) {
		InheritInfo inheritInfo = finalMap.get(desc.getBeanType());
		desc.setInheritInfo(inheritInfo);
	}

	private void initialise() {
		BootupClasses bootupClasses = properties.getBootupClasses();
		List<Class<?>> entityList = bootupClasses.getEntities();

		findInheritClasses(entityList);
		buildDeployTree();
		buildFinalTree();
	}

	private void findInheritClasses(List<Class<?>> entityList) {

		// go through each class and initialise the info object...
		Iterator<Class<?>> it = entityList.iterator();
		while (it.hasNext()) {
			Class<?> cls = (Class<?>) it.next();
			if (isInheritanceClass(cls)) {
				DeployInheritInfo info = createInfo(cls);
				deployMap.put(cls, info);
			}
		}
	}

	private void buildDeployTree() {
		Iterator<DeployInheritInfo> it = deployMap.values().iterator();
		while (it.hasNext()) {
			DeployInheritInfo info = it.next();
			if (!info.isRoot()) {
				DeployInheritInfo parent = getInfo(info.getParent());
				parent.addChild(info);
			}
		}
	}

	private void buildFinalTree() {

		Iterator<DeployInheritInfo> it = deployMap.values().iterator();
		while (it.hasNext()) {
			DeployInheritInfo deploy = it.next();
			if (deploy.isRoot()) {
				// build tree top down...
				createFinalInfo(null, null, deploy);

			}
		}
	}

	private InheritInfo createFinalInfo(InheritInfo root, InheritInfo parent,
			DeployInheritInfo deploy) {

		InheritInfo node = new InheritInfo(root, parent, deploy);
		if (parent != null) {
			parent.addChild(node);
		}
		finalMap.put(node.getType(), node);

		if (root == null) {
			root = node;
		}

		// buildFinalChildren(root, child, deploy);

		Iterator<DeployInheritInfo> it = deploy.children();

		while (it.hasNext()) {
			DeployInheritInfo childDeploy = it.next();

			createFinalInfo(root, node, childDeploy);
		}

		return node;
	}

	/**
	 * Build the InheritInfo for a given class.
	 */
	private DeployInheritInfo getInfo(Class<?> cls) {
		return deployMap.get(cls);
	}

	private DeployInheritInfo createInfo(Class<?> cls) {

		DeployInheritInfo info = new DeployInheritInfo(cls);

		Class<?> parent = findParent(cls);
		if (parent != null) {
			info.setParent(parent);
		} else {
			// its the root of inheritance tree...
		}

		Inheritance ia = (Inheritance) cls.getAnnotation(Inheritance.class);
		if (ia != null) {
			ia.strategy();
		}
		DiscriminatorColumn da = (DiscriminatorColumn) cls.getAnnotation(DiscriminatorColumn.class);
		if (da != null) {
			info.setDiscriminatorColumn(da.name());
			DiscriminatorType discriminatorType = da.discriminatorType();
			if (discriminatorType.equals(DiscriminatorType.INTEGER)){
				info.setDiscriminatorType(Types.INTEGER);				
			} else {
				info.setDiscriminatorType(Types.VARCHAR);
			}
		}

		DiscriminatorValue dv = (DiscriminatorValue) cls.getAnnotation(DiscriminatorValue.class);
		if (dv != null) {
			info.setDiscriminatorValue(dv.value());
		}
		
		return info;
	}

	private Class<?> findParent(Class<?> cls) {
		Class<?> superCls = cls.getSuperclass();
		if (isInheritanceClass(superCls)) {
			return superCls;
		} else {
			return null;
		}
	}

	private boolean isInheritanceClass(Class<?> cls) {
		if (cls.equals(Object.class)) {
			return false;
		}
		Annotation a = cls.getAnnotation(Inheritance.class);
		if (a != null) {
			return true;
		}
		// search up the inheritance heirarchy
		return isInheritanceClass(cls.getSuperclass());
	}

}

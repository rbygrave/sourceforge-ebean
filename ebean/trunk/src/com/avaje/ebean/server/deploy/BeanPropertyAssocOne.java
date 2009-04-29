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
package com.avaje.ebean.server.deploy;

import java.sql.SQLException;

import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.server.core.TransactionContextClass;
import com.avaje.ebean.server.deploy.id.IdBinder;
import com.avaje.ebean.server.deploy.id.ImportedId;
import com.avaje.ebean.server.deploy.jointree.JoinNode;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;

/**
 * Property mapped to a joined bean.
 */
public class BeanPropertyAssocOne<T> extends BeanPropertyAssoc<T> {

	final boolean oneToOne;

	final boolean oneToOneExported;

	final boolean embeddedVersion;

	final boolean importedPrimaryKey;
	
	final LocalHelp localHelp;

	final BeanProperty[] embeddedProps;
	
	/**
	 * The information for Imported foreign Keys.
	 */
	ImportedId importedId;


	/**
	 * Create the property.
	 */
	public BeanPropertyAssocOne(BeanDescriptorOwner owner, BeanDescriptor<?> descriptor,
			DeployBeanPropertyAssocOne<T> deploy) {

		super(owner, descriptor, deploy);

		importedPrimaryKey = deploy.isImportedPrimaryKey();
		oneToOne = deploy.isOneToOne();
		oneToOneExported = deploy.isOneToOneExported();

		if (embedded) {
			// Overriding of the columns and use table alias of owning BeanDescriptor
			BeanEmbeddedMeta overrideMeta = BeanEmbeddedMetaFactory.create(owner, deploy, descriptor);
			embeddedProps = overrideMeta.getProperties();
			if (id){
				embeddedVersion = false;
			} else {
				embeddedVersion = overrideMeta.isEmbeddedVersion();
			}
		} else {
			embeddedProps = null;
			embeddedVersion = false;
		}
		localHelp = createHelp(embedded, oneToOneExported);
	}
	
	@Override
	public void initialise() {
		super.initialise();
		if (!isTransient){
			if (!embedded && !oneToOneExported) {				
				importedId = createImportedId(this, targetDescriptor, tableJoin);
			}
		}
	}

	@Override
	public boolean isValueLoaded(Object value) {
		if (value instanceof EntityBean) {
			return ((EntityBean) value)._ebean_getIntercept().isLoaded();
		}
		return true;
	}

	@Override
	public InvalidValue validateCascade(Object value) {

		BeanDescriptor<?> target = getTargetDescriptor();
		return target.validate(true, value);
	}
	
	private boolean hasChangedEmbedded(Object bean, Object oldValues) {
		
		Object embValue = getValue(oldValues);
		if (embValue instanceof EntityBean){
			// the embedded bean .. has its own old values
			EntityBeanIntercept ebi = ((EntityBean)embValue)._ebean_getIntercept();
			return ebi.isDirty();
		}
		if (embValue == null){
			return getValue(bean) != null;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean hasChanged(Object bean, Object oldValues) {
		if (embedded){
			return hasChangedEmbedded(bean, oldValues);
		}
		Object value = getValue(bean);
		Object oldVal = getValue(oldValues);
		if (oneToOneExported) {
			// FKey on other side
			return false;
		} else {
			return importedId.hasChanged(value, oldVal);
		}
	}
	
	/**
	 * Return meta data for the deployment of the embedded bean specific to this
	 * property.
	 */
	public BeanProperty[] getProperties() {
		return embeddedProps;
	}

	/**
	 * Return true if this a OneToOne property. Otherwise assumed ManyToOne.
	 */
	public boolean isOneToOne() {
		return oneToOne;
	}

	/**
	 * Return true if this is the exported side of a OneToOne.
	 */
	public boolean isOneToOneExported() {
		return oneToOneExported;
	}

	/**
	 * Returns true if the associated bean has version properties.
	 */
	public boolean isEmbeddedVersion() {
		return embeddedVersion;
	}

	/**
	 * If true this bean maps to the primary key.
	 */
	public boolean isImportedPrimaryKey() {
		return importedPrimaryKey;
	}

	/**
	 * Same as getPropertyType(). Return the type of the bean this property
	 * represents.
	 */
	public Class<?> getTargetType() {
		return getPropertyType();
	}

	/**
	 * Create a vanilla bean of the target type to be used as an embeddedId
	 * value.
	 */
	public Object createEmbeddedId() {
		return getTargetDescriptor().createVanillaBean();
	}
	
	public ImportedId getImportedId() {
		return importedId;
	}

	@Override
	public void appendSelect(DbSqlContext ctx) {
		localHelp.appendSelect(ctx);
	}

	@Override
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
		localHelp.appendFrom(ctx, forceOuterJoin);
	}

	@Override
	public Object readSet(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {
		boolean assignable = (type == null || owningType.isAssignableFrom(type));
		return localHelp.readSet(ctx, bean, assignable);
	}

	private LocalHelp createHelp(boolean embedded, boolean oneToOneExported) {
		if (embedded) {
			return new Embedded();
		} else if (oneToOneExported) {
			return new ReferenceExported();
		} else {
			return new Reference();
		}
	}

	/**
	 * Local interface to handle Embedded, Reference and Reference Exported
	 * cases.
	 */
	private abstract class LocalHelp {

		JoinNode getJoinNode(DbSqlContext ctx) {
			JoinNode parentNode = ctx.peekJoinNode();
			JoinNode node = parentNode.findChild(name);
			if (node == null) {
				String m = "Error with 1-1 exported on " + descriptor;
				m += "." + name + ". JoinNode not found?";
				throw new RuntimeException(m);
			} else {
				return node;
			}
		}
		
		abstract Object readSet(DbReadContext ctx, Object bean, boolean assignAble)
				throws SQLException;

		abstract void appendSelect(DbSqlContext ctx);

		abstract void appendFrom(DbSqlContext ctx, boolean forceOuterJoin);
	}

	private final class Embedded extends LocalHelp {

		@Override
		Object readSet(DbReadContext ctx, Object bean, boolean assignable) throws SQLException {

			EntityBean embeddedBean = targetDescriptor.createEntityBean();

			boolean notNull = false;
			for (int i = 0; i < embeddedProps.length; i++) {
				Object value = embeddedProps[i].readSet(ctx, embeddedBean, null);
				if (value != null) {
					notNull = true;
				}
			}
			if (notNull && assignable) {
				// set back to the parent bean
				setValue(bean, embeddedBean);
				
				// make sure it is intercepting setters etc
				embeddedBean._ebean_getIntercept().setLoaded();
				return embeddedBean;
				
			} else {
				return null;
			}
		}

		@Override
		void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
		}

		@Override
		void appendSelect(DbSqlContext ctx) {
			ctx.setUseColumnAlias(true);
			for (int i = 0; i < embeddedProps.length; i++) {
				embeddedProps[i].appendSelect(ctx);
			}
			ctx.setUseColumnAlias(false);
		}
	}

	/**
	 * For imported reference - this is the common case.
	 */
	private final class Reference extends LocalHelp {

		/**
		 * Read and set a Reference bean.
		 */
		@Override
		Object readSet(DbReadContext ctx, Object bean, boolean assignable) throws SQLException {

			BeanDescriptor<?> rowDescriptor = null;
			Class<?> rowType = targetType;
			if (targetInheritInfo != null){
				// read discriminator to determine the type
				InheritInfo rowInheritInfo = targetInheritInfo.readType(ctx);
				rowType = rowInheritInfo.getType();
				rowDescriptor = rowInheritInfo.getBeanDescriptor();
			}
			
			// read the foreign key column(s)
			Object id = targetIdBinder.read(ctx);
			if (id == null || bean == null || !assignable) {
				return null;
			}

			// check transaction context to see if it already exists
			TransactionContextClass classContext = ctx.getClassContext(rowType);
			EntityBean existing = classContext.get(id);

			if (existing != null) {
				setValue(bean, existing);
				return existing;

			} else {
				// create a lazy loading reference/proxy 
				Object parent = null;
				EntityBean ref;
				if (targetInheritInfo != null){
					// for inheritance heirarchy create the 
					// correct type for this row...
					ref = rowDescriptor.createReference(id, parent, null);	
				} else {
					ref = targetDescriptor.createReference(id, parent, null);					
				}
				if (ctx.isAutoFetchProfiling()){
					// add profiling to this reference bean
					ctx.profileReference(ref._ebean_getIntercept(), getName());
				}
				
				setValue(bean, ref);
				classContext.put(id, ref);
				return ref;
			}
		}

		@Override
		void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
			if (targetInheritInfo != null){
				// add join to support the discriminator column
				JoinNode node = getJoinNode(ctx);
				node.addJoin(forceOuterJoin, ctx);
			}
		}

		/**
		 * Append columns for foreign key columns.
		 */
		@Override
		void appendSelect(DbSqlContext ctx) {
			
			if (targetInheritInfo != null){
				// add discriminator column
				JoinNode node = getJoinNode(ctx);
				String tableAlias = node.getTableAlias();
				ctx.appendColumn(tableAlias, targetInheritInfo.getDiscriminatorColumn());
			}
			//ctx.setWithColumnAlias(true);
			importedId.sqlAppend(ctx);
			//ctx.setWithColumnAlias(false);
		}
	}

	/**
	 * For OneToOne exported reference - not so common.
	 */
	private final class ReferenceExported extends LocalHelp {

		/**
		 * Read and set a Reference bean.
		 */
		@Override
		Object readSet(DbReadContext ctx, Object bean, boolean assignable) throws SQLException {

			//TODO: Support for Inheritance heirarchy on exported OneToOne ?
			IdBinder idBinder = targetDescriptor.getIdBinder();
			Object id = idBinder.read(ctx);
			if (id == null || bean == null || !assignable) {
				return null;
			}

			TransactionContextClass classContext = ctx.getClassContext(targetType);
			EntityBean existing = classContext.get(id);

			if (existing != null) {
				setValue(bean, existing);
				return existing;

			} else {
				Object parent = null;
				EntityBean ref = targetDescriptor.createReference(id, parent, null);
				setValue(bean, ref);
				classContext.put(id, ref);
				return ref;
			}
		}

		/**
		 * Append columns for foreign key columns.
		 */
		@Override
		void appendSelect(DbSqlContext ctx) {
			
			JoinNode node = getJoinNode(ctx);

			// set appropriate tableAlias for 
			// the exported id columns
			String tableAlias = node.getTableAlias();
			ctx.pushTableAlias(tableAlias);

			IdBinder idBinder = targetDescriptor.getIdBinder();
			idBinder.appendSelect(ctx);

			ctx.popTableAlias();
		}

		@Override
		void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {

			JoinNode node = getJoinNode(ctx);
			node.addJoin(forceOuterJoin, ctx);
		}

	}
}

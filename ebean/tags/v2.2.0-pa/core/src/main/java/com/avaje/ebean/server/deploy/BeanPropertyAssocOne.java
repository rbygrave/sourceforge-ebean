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
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.server.core.ReferenceOptions;
import com.avaje.ebean.server.deploy.id.IdBinder;
import com.avaje.ebean.server.deploy.id.ImportedId;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.query.SqlBeanLoad;

/**
 * Property mapped to a joined bean.
 */
public class BeanPropertyAssocOne<T> extends BeanPropertyAssoc<T> {

	private final boolean oneToOne;

	private final boolean oneToOneExported;

	private final boolean embeddedVersion;

	private final boolean importedPrimaryKey;

	private final LocalHelp localHelp;

	private final BeanProperty[] embeddedProps;

	/**
	 * The information for Imported foreign Keys.
	 */
	private ImportedId importedId;


	/**
	 * Create based on deploy information of an EmbeddedId.
	 */
	public BeanPropertyAssocOne(BeanDescriptorMap owner, DeployBeanPropertyAssocOne<T> deploy) {
		this(owner, null, deploy);
	}
	
	/**
	 * Create the property.
	 */
	public BeanPropertyAssocOne(BeanDescriptorMap owner, BeanDescriptor<?> descriptor,
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

	public void addFkey() {
		if (importedId != null){
			importedId.addFkeys(name);
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
			return ((EntityBean)embValue)._ebean_getIntercept().isNewOrDirty();
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
			if (value == null) {
				return oldVal != null;
			} else if (oldValues == null){
				return true;
			}

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
	 * Return the Id values from the given bean.
	 */
	@Override
	public Object[] getAssocOneIdValues(Object bean) {
		return targetDescriptor.getIdBinder().getIdValues(bean);
	}

	/**
	 * Return the Id expression to add to where clause etc.
	 */
	public String getAssocOneIdExpr(String prefix, String operator) {
		return targetDescriptor.getIdBinder().getAssocOneIdExpr(prefix, operator);
	}

	@Override
	public boolean isAssocOneId() {
		return !embedded;
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
		if (!isTransient){
			localHelp.appendSelect(ctx);
		}
	}

	@Override
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
		if (!isTransient){
			localHelp.appendFrom(ctx, forceOuterJoin);
		}
	}

	@Override
	public Object readSet(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {
		boolean assignable = (type == null || owningType.isAssignableFrom(type));
		return localHelp.readSet(ctx, bean, assignable);
	}

	/**
	 * Read the data from the resultSet effectively ignoring it and returning null.
	 */
	@Override
	public Object read(DbReadContext ctx, int parentState) throws SQLException {
		// just read the resultSet incrementing the column index
		// pass in null for the bean so any data read is ignored
		return localHelp.read(ctx, parentState);
	}
	
	public void loadIgnore(SqlBeanLoad sqlBeanLoad) {
		localHelp.loadIgnore(sqlBeanLoad);
	}
	
	@Override
	public void load(SqlBeanLoad sqlBeanLoad) throws SQLException {
		sqlBeanLoad.load(this);
	}

	private LocalHelp createHelp(boolean embedded, boolean oneToOneExported) {
		if (embedded) {
			return new Embedded();
		} else if (oneToOneExported) {
			return new ReferenceExported();
		} else {
			return new Reference(this);
		}
	}

	/**
	 * Local interface to handle Embedded, Reference and Reference Exported
	 * cases.
	 */
	private abstract class LocalHelp {

		abstract void loadIgnore(SqlBeanLoad sqlBeanLoad);
		
		abstract Object read(DbReadContext ctx, int parentState) throws SQLException;
		
		abstract Object readSet(DbReadContext ctx, Object bean, boolean assignAble)
				throws SQLException;

		abstract void appendSelect(DbSqlContext ctx);

		abstract void appendFrom(DbSqlContext ctx, boolean forceOuterJoin);
	}

	private final class Embedded extends LocalHelp {

		void loadIgnore(SqlBeanLoad sqlBeanLoad) {
			sqlBeanLoad.loadIgnore(embeddedProps.length);
		}
		
		@Override
		Object readSet(DbReadContext ctx, Object bean, boolean assignable) throws SQLException {
			Object dbVal = read(ctx, 0);
			if (bean != null && assignable){
				// set back to the parent bean
				setValue(bean, dbVal);
				// propagate sharedInstance and readOnly state
				((EntityBean)bean)._ebean_getIntercept().propagateState((EntityBean)dbVal);

				// Handled by the EntityBean itself now (since 1.2)
				// make sure it is intercepting setters etc
				//embeddedBean._ebean_getIntercept().setLoaded();
				return dbVal;

			} else {
				return null;
			}
		}
		
		Object read(DbReadContext ctx, int parentState) throws SQLException {
			
			EntityBean embeddedBean = targetDescriptor.createEntityBean();

			boolean notNull = false;
			for (int i = 0; i < embeddedProps.length; i++) {
				Object value = embeddedProps[i].readSet(ctx, embeddedBean, null);
				if (value != null) {
					notNull = true;
				}
			}
			if (notNull) {
				if (parentState != 0){
					embeddedBean._ebean_getIntercept().propagateParentState(parentState);
				}
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
			for (int i = 0; i < embeddedProps.length; i++) {
				embeddedProps[i].appendSelect(ctx);
			}
		}
	}

	/**
	 * For imported reference - this is the common case.
	 */
	private final class Reference extends LocalHelp {

		private final BeanPropertyAssocOne<?> beanProp;

		Reference(BeanPropertyAssocOne<?> beanProp){
			this.beanProp = beanProp;
		}
		void loadIgnore(SqlBeanLoad sqlBeanLoad) {
			sqlBeanLoad.loadIgnore(targetIdBinder.getPropertyCount());
		}
		
		Object readSet(DbReadContext ctx, Object bean, boolean assignable) throws SQLException {
			Object val = read(ctx, 0);
			if (bean != null && assignable){
				setValue(bean, val);
				// propagate sharedInstance and readOnly state
				((EntityBean)bean)._ebean_getIntercept().propagateState((EntityBean)val);
			}
			return val;
		}
		
		/**
		 * Read and set a Reference bean.
		 */
		@Override
		Object read(DbReadContext ctx, int parentState) throws SQLException {

			BeanDescriptor<?> rowDescriptor = null;
			Class<?> rowType = targetType;
			if (targetInheritInfo != null){
				// read discriminator to determine the type
				InheritInfo rowInheritInfo = targetInheritInfo.readType(ctx);
				if (rowInheritInfo != null){
					rowType = rowInheritInfo.getType();
					rowDescriptor = rowInheritInfo.getBeanDescriptor();
				}
			}

			// read the foreign key column(s)
			Object id = targetIdBinder.read(ctx);
			if (id == null) {// || bean == null || !assignable) {
				return null;
			}

			// check transaction context to see if it already exists
			Object existing = ctx.getPersistenceContext().get(rowType, id);

			if (existing != null) {
				//setValue(bean, existing);
				return existing;

			} else {
				// parent always null for this case (but here to document)
				Object parent = null;
				Object ref = null;

				ReferenceOptions options = ctx.getReferenceOptionsFor(beanProp);
				if (options != null && options.isUseCache()) {
					ref = targetDescriptor.cacheGet(id);
					if (ref != null && !options.isReadOnly()){
						// create a copy as the user may mutate it
						ref = targetDescriptor.createCopy(ref);
					}
				}

				boolean createReference = false;
				if (ref == null){
					// create a lazy loading reference/proxy
					createReference = true;
					if (targetInheritInfo != null){
						// for inheritance hierarchy create the correct type for this row...
						ref = rowDescriptor.createReference(id, parent, options);
					} else {
						ref = targetDescriptor.createReference(id, parent, options);
					}
				}

				Object existingBean = ctx.getPersistenceContext().putIfAbsent(id, ref);
				if (existingBean != null){
					// advanced case when we use multiple concurrent threads to
					// build a single object graph, and another thread has since
					// loaded a matching bean so we will use that instead.
					ref = existingBean;
					createReference = false;
				}

				EntityBeanIntercept ebi = ((EntityBean)ref)._ebean_getIntercept();

				if (createReference){
					ebi.propagateParentState(parentState);					
					ctx.register(name, ebi);
				}

				return ref;
			}
		}

		@Override
		void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
			if (targetInheritInfo != null){
				// add join to support the discriminator column
				String relativePrefix = ctx.getRelativePrefix(name);
				tableJoin.addJoin(forceOuterJoin, relativePrefix, ctx);
			}
		}

		/**
		 * Append columns for foreign key columns.
		 */
		@Override
		void appendSelect(DbSqlContext ctx) {

			if (targetInheritInfo != null){
				// add discriminator column
				String relativePrefix = ctx.getRelativePrefix(getName());
				String tableAlias = ctx.getTableAlias(relativePrefix);
				ctx.appendColumn(tableAlias, targetInheritInfo.getDiscriminatorColumn());
			}
			importedId.sqlAppend(ctx);
		}
	}

	/**
	 * For OneToOne exported reference - not so common.
	 */
	private final class ReferenceExported extends LocalHelp {

		
		@Override
		void loadIgnore(SqlBeanLoad sqlBeanLoad) {
			sqlBeanLoad.loadIgnore(targetDescriptor.getIdBinder().getPropertyCount());
		}

		/**
		 * Read and set a Reference bean.
		 */
		@Override
		Object readSet(DbReadContext ctx, Object bean, boolean assignable) throws SQLException {

			Object dbVal = read(ctx, 0);
			if (bean != null && assignable){
				setValue(bean, dbVal);
				// propagate sharedInstance and readOnly state
				((EntityBean)bean)._ebean_getIntercept().propagateState((EntityBean)dbVal);

			}
			return dbVal;
		}
		
		Object read(DbReadContext ctx, int parentState) throws SQLException {
			
			//TODO: Support for Inheritance hierarchy on exported OneToOne ?
			IdBinder idBinder = targetDescriptor.getIdBinder();
			Object id = idBinder.read(ctx);
			if (id == null) {// || bean == null || !assignable) {
				return null;
			}

			PersistenceContext persistCtx = ctx.getPersistenceContext();
			Object existing = persistCtx.get(targetType, id);

			if (existing != null) {
				//setValue(bean, existing);
				return existing;

			} else {
				Object parent = null;
				Object ref = targetDescriptor.createReference(id, parent, null);
				if (parentState != 0){
					((EntityBean)ref)._ebean_getIntercept().propagateParentState(parentState);
				}
				persistCtx.put(id, ref);
				return ref;
			}
		}

		/**
		 * Append columns for foreign key columns.
		 */
		@Override
		void appendSelect(DbSqlContext ctx) {

			// set appropriate tableAlias for
			// the exported id columns

			String relativePrefix = ctx.getRelativePrefix(getName());
			ctx.pushTableAlias(relativePrefix);

			IdBinder idBinder = targetDescriptor.getIdBinder();
			idBinder.appendSelect(ctx);

			ctx.popTableAlias();
		}

		@Override
		void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {

			String relativePrefix = ctx.getRelativePrefix(getName());
			tableJoin.addJoin(forceOuterJoin, relativePrefix, ctx);
		}
	}
}

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
package org.avaje.ebean.server.deploy;

import java.util.ArrayList;
import java.util.logging.Level;

import org.avaje.ebean.bean.EntityBean;
import org.avaje.ebean.server.deploy.id.IdBinder;
import org.avaje.ebean.server.deploy.id.ImportedId;
import org.avaje.ebean.server.deploy.id.ImportedIdEmbedded;
import org.avaje.ebean.server.deploy.id.ImportedIdMultiple;
import org.avaje.ebean.server.deploy.id.ImportedIdSimple;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import org.avaje.lib.util.StringHelper;

/**
 * Abstract base for properties mapped to an associated bean, list, set or map.
 */
public abstract class BeanPropertyAssoc extends BeanProperty {

	/**
	 * The descriptor of the target. This MUST be lazy loaded so as to avoid a
	 * dependency loop between BeanDescriptors.
	 */
	BeanDescriptor targetDescriptor;
	
	IdBinder targetIdBinder;
	
	InheritInfo targetInheritInfo;
	
	/**
	 * Persist settings.
	 */
	final BeanCascadeInfo cascadeInfo;

	/**
	 * Join between the beans.
	 */
	final TableJoin tableJoin;

	/**
	 * The type of the joined bean.
	 */
	final Class<?> targetType;

	/**
	 * The join table information.
	 */
	final BeanTable beanTable;

	/**
	 * Whether the associated join type should be an outer join.
	 */
	final boolean isOuterJoin;
	
	String extraWhere;

	String targetTableAlias;
	
	boolean saveRecurseSkippable;
	
	boolean deleteRecurseSkippable;

	/**
	 * Construct the property.
	 */
	public BeanPropertyAssoc(BeanDescriptorOwner owner, BeanDescriptor descriptor,
			DeployBeanPropertyAssoc deploy) {
		super(owner, descriptor, deploy);
		this.extraWhere = deploy.getExtraWhere();
		this.isOuterJoin = deploy.isOuterJoin();
		this.beanTable = deploy.getBeanTable();

		this.tableJoin = new TableJoin(deploy.getTableJoin(), null);

		this.targetType = deploy.getTargetType();
		this.cascadeInfo = deploy.getCascadeInfo();
	}

	/**
	 * Initialise post construction.
	 */
	@Override
	public void initialise() {
		// this *MUST* execute after the BeanDescriptor is
		// put into the map to stop infinite recursion
		if (!isTransient){
			targetDescriptor = descriptor.getBeanDescriptor(targetType);
			targetIdBinder = targetDescriptor.getIdBinder();
			targetInheritInfo = targetDescriptor.getInheritInfo();
			
			saveRecurseSkippable = targetDescriptor.isSaveRecurseSkippable();
			deleteRecurseSkippable = targetDescriptor.isDeleteRecurseSkippable();
			
			cascadeValidate = cascadeInfo.isValidate();
			
			targetTableAlias = targetDescriptor.getBaseTableAlias();
			if (extraWhere != null){
				extraWhere = StringHelper.replaceString(extraWhere, "${ta}", targetTableAlias);
			}
		}
	}

	/**
	 * Return false.
	 */
	public boolean isScalar() {
		return false;
	}
	
	/**
	 * Return the BeanDescriptor of the target.
	 */
	public BeanDescriptor getTargetDescriptor() {
		return targetDescriptor;
	}

	public boolean isSaveRecurseSkippable(Object bean) {
		if (!saveRecurseSkippable){
			return false;
		}
		if (bean instanceof EntityBean){
			return ((EntityBean)bean)._ebean_getIntercept().isDirty();
		} else {
			// we don't know so we say no
			return false;
		}
	}
	
	/**
	 * Return true if save can be skipped for unmodified bean(s) of this
	 * property.
	 * <p>
	 * That is, if a bean of this property is unmodified we don't need to
	 * saveRecurse because none of its associated beans have cascade save set to
	 * true.
	 * </p>
	 */
	public boolean isSaveRecurseSkippable() {
		return saveRecurseSkippable;
	}

	/**
	 * Similar to isSaveRecurseSkippable but in terms of delete.
	 */
	public boolean isDeleteRecurseSkippable() {
		return deleteRecurseSkippable;
	}

	/**
	 * Return true if the unique id properties are all not null for this bean.
	 */
	public boolean hasId(Object bean) {

		BeanDescriptor targetDesc = getTargetDescriptor();

		BeanProperty[] uids = targetDesc.propertiesId();
		for (int i = 0; i < uids.length; i++) {

			Object value = uids[i].getValue(bean);
			if (value == null) {
				return false;
			}
		}
		// all the unique properties are non-null
		return true;
	}

	/**
	 * Return the type of the target.
	 * <p>
	 * This is the class of the associated bean, or beans contained in a list,
	 * set or map.
	 * </p>
	 */
	public Class<?> getTargetType() {
		return targetType;
	}

	/**
	 * Return an extra clause to add to the query for loading or joining
	 * to this bean type.
	 */
	public String getExtraWhere() {
		return extraWhere;
	}

	/**
	 * Return if this association should use an Outer join.
	 */
	public boolean isOuterJoin() {
		return isOuterJoin;
	}

	/**
	 * return the join to use for the bean.
	 */
	public TableJoin getTableJoin() {
		return tableJoin;
	}

	/**
	 * Return the BeanTable for this association.
	 * <p>
	 * This has the table name which is used to determine the relationship for
	 * this association.
	 * </p>
	 */
	public BeanTable getBeanTable() {
		return beanTable;
	}

	/**
	 * Get the persist info.
	 */
	public BeanCascadeInfo getCascadeInfo() {
		return cascadeInfo;
	}
	
	/**
	 * Build the list of imported property. Matches BeanProperty from the target
	 * descriptor back to local database columns in the TableJoin.
	 */
	protected ImportedId createImportedId(BeanPropertyAssoc owner, BeanDescriptor target, TableJoin join) {

		BeanProperty[] props = target.propertiesId();
		
		if (descriptor.isSqlSelectBased()){
			String dbColumn = owner.getDbColumn();
			return new ImportedIdSimple(owner, dbColumn, props[0]);
		}
		
		TableJoinColumn[] cols = join.columns();


		if (props.length == 1) {
			if (!props[0].isEmbedded()) {
				// simple single scalar id
				if (cols.length != 1){
					String msg = "No Imported Id column for ["+props[0]+"] in table ["+join.getTable()+"]";
					logger.log(Level.SEVERE, msg);
					return null;
				} else {
					return createImportedScalar(owner, cols[0], props);
				}
			} else {
				// embedded id
				BeanPropertyAssocOne embProp = (BeanPropertyAssocOne)props[0];
				BeanProperty[] embBaseProps = embProp.getTargetDescriptor().propertiesBaseScalar();
				ImportedIdSimple[] scalars = createImportedList(owner, cols, embBaseProps);
				
				return new ImportedIdEmbedded(owner, embProp, scalars);
			}
			
		} else {
			// Concatenated key that is not embedded
			ImportedIdSimple[] scalars = createImportedList(owner, cols, props);
			return new ImportedIdMultiple(owner, scalars);
		}	
	}
	
	private ImportedIdSimple[] createImportedList(BeanPropertyAssoc owner, TableJoinColumn[] cols, BeanProperty[] props) {
		
		ArrayList<ImportedIdSimple> list = new ArrayList<ImportedIdSimple>();

		for (int i = 0; i < cols.length; i++) {
			list.add(createImportedScalar(owner, cols[i], props));
		}

		return (ImportedIdSimple[]) list.toArray(new ImportedIdSimple[list.size()]);
	}
	
	private ImportedIdSimple createImportedScalar(BeanPropertyAssoc owner, TableJoinColumn col, BeanProperty[] props) {

		String matchColumn = col.getForeignDbColumn();
		String localColumn = col.getLocalDbColumn();

		for (int j = 0; j < props.length; j++) {
			if (props[j].getDbColumn().equalsIgnoreCase(matchColumn)) {
				return new ImportedIdSimple(owner, localColumn, props[j]);
			}
		}
		String msg = "matchColumn[" + matchColumn + "] localColumn[" + localColumn + "]";
		throw new RuntimeException("Matching property not found? " + msg);
	}
}

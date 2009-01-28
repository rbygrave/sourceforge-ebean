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
package org.avaje.ebean.server.naming;

import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.deploy.BeanProperty;
import org.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import org.avaje.lib.util.FactoryHelper;
import org.avaje.lib.util.StringHelper;
import org.avaje.ebean.server.plugin.PluginProperties;

/**
 * Converts from database column names with underscores.
 */
public class NamingConvention {

    /**
     * refer to getForeignKeyColumn();
     */
    protected String foreignKeySuffix = "Id";

    /**
     * CamelCase to underscore converter.
     */
    protected PropertyNamingConvention propertyNamingConvention;

    /**
     * Naming conventions for db sequences.
     */
    protected SequenceNaming seqNaming;

    /**
     * The plugin.
     */
    protected PluginProperties properties;

    boolean mapBeanLowerCase = true;
    
    boolean mapBeanPropertyName = false;
    
    /**
     * Create the NamingConvention.
     */
    public NamingConvention(PluginProperties properties) {
        this.properties = properties;
        this.propertyNamingConvention = createPropertyNamingConvention(properties);
        this.seqNaming = new SequenceNaming(properties);
        
        String fks = properties.getProperty("namingconvention.foreignkey.suffix", null);
        if (fks != null){
            foreignKeySuffix = fks;
        }
        
        String lc = properties.getProperty("namingconvention.mapbean.lowercase", "true");
        mapBeanLowerCase = lc.equalsIgnoreCase("true");

        String pn = properties.getProperty("namingconvention.mapbean.propertyname", "false");
        mapBeanPropertyName = pn.equalsIgnoreCase("true");

    }

    /**
     * Create the PropertyNamingConvention implementation.
     */
    private PropertyNamingConvention createPropertyNamingConvention(PluginProperties properties) {
    	 
    	String implName = properties.getProperty("namingconvention.property", null);
    	if (implName != null){
    		return (PropertyNamingConvention)FactoryHelper.create(implName);
    	}
    	
    	// use the default implementation
    	boolean forceUpperCase = properties.getPropertyBoolean("namingconvention.property.forceuppercase", false);
    	
    	UnderscoreCamelCase defaultImpl = new UnderscoreCamelCase();
    	defaultImpl.setForceUpperCase(forceUpperCase);
    	return defaultImpl;
    }
    
    
    /**
     * Returns a SQL Select statement that returns the last inserted Identity/Sequence value.
     * <p>
     * This is for DB/JDBC that does NOT support getGeneratedKeys. For example MS SQL Server 2000
     * this would return something like "select @@IDENTITY".
     * </p>
     */
    public String getSelectLastInsertedId(BeanDescriptor desc) {
    	
        String idColumnName = null;
        BeanProperty[] ids = desc.propertiesId();
        if (ids.length == 1){
        	idColumnName = ids[0].getDbColumn();
        }
        
        return getSelectLastInsertedId(desc.getBaseTable(), idColumnName);
    }
    
    protected String getSelectLastInsertedId(String tableName, String idColumnName) {
    	
        String sqlSelect = properties.getProperty("namingconvention.selectLastInsertedId", null);

        if (sqlSelect != null){
	        sqlSelect = StringHelper.replaceString(sqlSelect, "{table}", tableName);
	        if (idColumnName != null){
	        	sqlSelect = StringHelper.replaceString(sqlSelect, "{column}", idColumnName);
	        }
        }
        
        return sqlSelect;
    }
    
    /**
     * Returns the table name for a given Class when the @Table annotation
     * has not set the table name.
     */
    public String tableNameFromClass(Class<?> beanClass){
    	
    	String clsName = beanClass.getName();
		int dp = clsName.lastIndexOf('.');
		if (dp != -1){
			clsName = clsName.substring(dp+1);
		}
		
		return clsName;
    }
    
    /**
     * Return the column name given the property name.
     * For example camelCase to underscore.
     */
    public String toColumn(String beanPropertyName) {
        return propertyNamingConvention.toColumn(beanPropertyName);
    }

    /**
     * Return the property name from the column name.
     * For example underscore to camelCase.
     */
    public String toPropertyName(String dbColumnName) {
        return propertyNamingConvention.toPropertyName(dbColumnName);
    }

    /**
     * Return the sequence name given the descriptor. Normally the name would be
     * based on the table name and perhaps the unique property.
     */
    public String getSequenceNextval(DeployBeanDescriptor desc) {
        return seqNaming.getNextVal(desc);
    }
    
    /**
     * Used to dynamically find a foreign key when more than one exists to
     * choose from.
     * <p>
     * For example say Customer has billingAddress and shippingAddress. The
     * foreign key column name for billingAddress would be determined as
     * billing_address_id (where id is the foreignKeySuffix and
     * toColumnFromProperty() converts camel case to underscore.
     * </p>
     */
    public String getForeignKeyColumn(String propertyName) {
        String joinedProp = propertyName + foreignKeySuffix;

        return toColumn(joinedProp);
    }

    /**
     * Used by code generator to determine the logical property name
     * for a given foreign key column.
     */
    public String getForeignKeyProperty(String dbForeignKeyColumn) {
    	if (dbForeignKeyColumn.endsWith(foreignKeySuffix)){
    		int endIndex = dbForeignKeyColumn.length() - foreignKeySuffix.length();
    		dbForeignKeyColumn = dbForeignKeyColumn.substring(0,endIndex);
    		
    	} else {
    		int lastUnderscore = dbForeignKeyColumn.lastIndexOf('_');
    		if (lastUnderscore > -1){
    			dbForeignKeyColumn = dbForeignKeyColumn.substring(0, lastUnderscore);
    		}
    	}

    	return toPropertyName(dbForeignKeyColumn);
    }
    
    /**
     * Converts db column names to property names for MapBeans.
     * <p>
     * This defaults to lower casing the db column name. 
     * </p>
     */
    public String mapPropertyFromColumn(String dbColumnName) {
        if (mapBeanLowerCase){
            return dbColumnName.toLowerCase();
        }
        if (mapBeanPropertyName){
            return toPropertyName(dbColumnName);
        }
        return dbColumnName;
    }
}

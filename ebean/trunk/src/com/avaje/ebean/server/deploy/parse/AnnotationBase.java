package com.avaje.ebean.server.deploy.parse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.Table;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.naming.NamingConvention;
import com.avaje.ebean.config.naming.TableName;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;

/**
 * Provides some base methods for processing deployment annotations.
 */
public abstract class AnnotationBase {

	protected final DatabasePlatform databasePlatform;
	protected final NamingConvention namingConvention;
	protected final DeployUtil util;
	
	protected AnnotationBase(DeployUtil util) {
		this.util = util;
		this.databasePlatform = util.getDbPlatform();
		this.namingConvention = util.getNamingConvention();
	}
	
    /**
     * read the deployment annotations.
     */
    public abstract void parse();
    
	/**
	 * Checks string is null or empty .
	 */
	protected boolean isEmpty(String s) {
		if (s == null || s.trim().length() == 0) {
			return true;
		}
		return false;
	}
	
	   
    /**
     * Return the annotation for the property.
     * <p>
     * Looks first at the field and then at the getter method.
     * </p>
     */
	protected <T extends Annotation> T get(DeployBeanProperty prop, Class<T> annClass) {
        T a = null;
        Field field = prop.getField();
        if (field != null){
        	a = field.getAnnotation(annClass);
        }
        if (a == null) {
            Method m = prop.getReadMethod();
            if (m != null) {
                a = m.getAnnotation(annClass);
            }
        }
        return a;
    }
    
    /**
	 * Return the annotation for the property.
	 * <p>
	 * Looks first at the field and then at the getter method. then at class level.
	 * </p>
	 */
	protected <T extends Annotation> T find(DeployBeanProperty prop, Class<T> annClass) {
		T a = get(prop, annClass);
		if (a == null) {
			a = prop.getOwningType().getAnnotation(annClass);
		}
		return a;
	}
	
	/**
	 * Returns the table name for a given entity bean.
	 * <p>
	 * This first checks for the @Table annotation and if not present
	 * uses the naming convention to define the table name.
	 * </p>
	 */
	public TableName getTableName(Class<?> beanClass) {

		TableName tableName = getTableNameFromAnnotation(beanClass);
		if (tableName == null){
			tableName = namingConvention.getTableNameFromClass(beanClass);
		}
		return tableName;
	}
	
	/**
	 * Gets the table name from annotation.
	 */
	private TableName getTableNameFromAnnotation(Class<?> beanClass) {
		
		final Table t = findTableAnnotation(beanClass);

		// Take the annotation if defined
		if (t != null && !isEmpty(t.name())){
			// Note: empty catalog and schema are converted to null
			// Only need to convert quoted identifiers from annotations
			return new TableName(quoteIdentifiers(t.catalog()),
				quoteIdentifiers(t.schema()),
				quoteIdentifiers(t.name()));
		}

		// No annotation
		return null;	
	}
	
	/**
	 * Search recursively for an @Table in the class hierarchy.
	 */
	private Table findTableAnnotation(Class<?> cls) {
		if (cls.equals(Object.class)){
			return null;
		}
		Table table = cls.getAnnotation(Table.class);
		if (table != null){
			return table;
		}
		return findTableAnnotation(cls.getSuperclass());
	}
	
	/**
	 * Replace back ticks (if they are used) with database platform specific
	 * quoted identifiers.
	 */
	private String quoteIdentifiers(String s) {
		return databasePlatform.convertQuotedIdentifiers(s);
	}
}

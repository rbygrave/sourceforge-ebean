/**
 * Copyright (C) 2009  Robin Bygrave
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
package com.avaje.ebean.config.naming;

import java.lang.reflect.Field;

/**
 * Converts between Camel Case and Underscore based names.
 */
public class UnderscoreNamingConvention extends AbstractNamingConvention {

	/** Force toUnderscore to return in upper case. */
    private boolean forceUpperCase = false;

    /** The digits compressed. */
    private boolean digitsCompressed = true;

	/**
	 * Create with a given sequence format.
	 *
	 * @param sequenceFormat the sequence format
	 */
	public UnderscoreNamingConvention(String sequenceFormat) {
		super(sequenceFormat);
	}

	/**
	 * Create with a sequence format of "{table}_seq".
	 */
	public UnderscoreNamingConvention() {
		super();
	}

	/**
	 * Returns the last part of the class name.
	 *
	 * @param beanClass the bean class
	 *
	 * @return the table name from class
	 */
	public TableName getTableNameFromClass(Class<?> beanClass) {
		return new TableName(getCatalog(), getSchema(),
			toUnderscoreFromCamel(beanClass.getSimpleName()));
	}

	/**
	 * Converts Camel case property name to underscore based column name.
	 *
	 * @param field the field
	 *
	 * @return the column from property
	 */
	public String getColumnFromProperty(Field field) {
		// Get annotation
		String columnName = getColumnFromAnnotation(field);

		if (columnName == null){
			// no annotation - convert the field name to an underscored name
			columnName = toUnderscoreFromCamel(field.getName());;
		}
		return columnName;
	}

	/**
	 * Converts underscore based column name to Camel case property name.
	 *
	 * @param beanClass the bean class
	 * @param dbColumnName the db column name
	 *
	 * @return the property from column
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName) {
		return toCamelFromUnderscore(dbColumnName);
	}

	/**
	 * Checks if is force upper case.
	 *
	 * @return the forceUpperCase
	 */
	public boolean isForceUpperCase() {
		return forceUpperCase;
	}

	/**
	 * Sets the force upper case.
	 *
	 * @param forceUpperCase the forceUpperCase to set
	 */
	public void setForceUpperCase(boolean forceUpperCase) {
		this.forceUpperCase = forceUpperCase;
	}

	/**
	 * Checks if is digits compressed.
	 *
	 * @return the digitsCompressed
	 */
	public boolean isDigitsCompressed() {
		return digitsCompressed;
	}

	/**
	 * Sets the digits compressed.
	 *
	 * @param digitsCompressed the digitsCompressed to set
	 */
	public void setDigitsCompressed(boolean digitsCompressed) {
		this.digitsCompressed = digitsCompressed;
	}



    /**
     * To underscore from camel.
     *
     * @param camelCase the camel case
     *
     * @return the string
     */
    protected String toUnderscoreFromCamel(String camelCase){

        int lastUpper = -1;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isDigit(c)) {
                if (i > lastUpper+1 && !digitsCompressed){
                    sb.append("_");
                }
                sb.append(c);
                lastUpper = i;

            } else if (Character.isUpperCase(c)) {
                if (i > lastUpper+1){
                    sb.append("_");
                }
                sb.append(Character.toLowerCase(c));
                lastUpper = i;

            } else {
                sb.append(c);
            }
        }
        String ret = sb.toString();
        if (forceUpperCase){
            ret = ret.toUpperCase();
        }
        return ret;
    }

    /**
     * To camel from underscore.
     *
     * @param underscore the underscore
     *
     * @return the string
     */
    protected String toCamelFromUnderscore(String underscore){

        StringBuffer result = new StringBuffer();
        String[] vals = underscore.split("_");

        for (int i = 0; i < vals.length; i++) {
            String lower = vals[i].toLowerCase();
            if (i > 0){
                char c = Character.toUpperCase(lower.charAt(0));
                result.append(c);
                result.append(lower.substring(1));
            } else {
                result.append(lower);
            }
        }

        return result.toString();
    }
}

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
package org.avaje.ebean.server.persist;

import java.sql.Types;

import org.avaje.ebean.server.core.PersistRequest;
import org.avaje.ebean.server.deploy.BeanProperty;

/**
 * Object used in SQL DML generation to determine which columns should be
 * excluded from parts of the DML.
 * <p>
 * Provides for exclusion of audit columns, user defined datatypes and Blob type
 * columns from parts of the DML statements.
 * </p>
 */
public class Include implements Constant {

    
    protected boolean excludeClobBlobFromInsertUpdate = false;

    protected static final String Y = "Y";

    protected static final String TRUE = "TRUE";
    
    public boolean in(int inCode, BeanProperty prop, PersistRequest request) {
        switch (inCode) {
        case IN_INSERT:
            return inInsertClause(prop, request);
        case IN_UPDATE_SET:
            return inUpdateClause(prop, request);
        case IN_UPDATE_WHERE:
            return inUpdateWhereClause(prop, request);
        case IN_DELETE_WHERE:
            return inDeleteWhereClause(prop, request);

        default:
            throw new RuntimeException("Invalid inCode ["+inCode+"]");
        }
    }
    
    /**
     * returns true if this property should be excluded from the WHERE clause of
     * a delete statement.
     * <p>
     * That is, Blob type data and External DataTypes are not included in any
     * where clauses (for update or delete). Additionally you can specify the
     * EXCLUDE_FROM_DELETE_WHERE property in the bean deployment file for this
     * property.
     * </p>
     */
    public boolean inDeleteWhereClause(BeanProperty prop, PersistRequest request) {

        return inWhereClause(prop, BeanProperty.EXCLUDE_FROM_DELETE_WHERE, request);
    }

    /**
     * returns true if this property should be excluded from the WHERE clause of
     * a update statement.
     * <p>
     * That is, Blob type data and External DataTypes are not included in any
     * where clauses (for update or delete). Additionally you can specify the
     * EXCLUDE_FROM_UPDATE_WHERE property in the entity deployment file for this
     * property.
     * </p>
     */
    public boolean inUpdateWhereClause(BeanProperty prop, PersistRequest request) {

        return inWhereClause(prop, BeanProperty.EXCLUDE_FROM_UPDATE_WHERE, request);
    }

    private boolean inWhereClause(BeanProperty prop, String extraProperty,
            PersistRequest request) {

        if (!prop.isDbWrite()) {
            // always exclude non-dbwrite columns from where clauses...
            return false;
        }
        //if (prop.isUniqueId()) {
        //    // always include the unique id columns
        //    return true;
        //}
//        if (request.isConcurrencyModeNone()) {
//            return false;
//        }
//        if (request.isConcurrencyModeVersion()) {
//            return prop.isVersionColumn();
//        }

        int columnDataType = prop.getDbType();
        if ((columnDataType == Types.LONGVARCHAR) || (columnDataType == Types.CLOB)
                || (columnDataType == Types.LONGVARBINARY) || (columnDataType == Types.BLOB)
                || (columnDataType == Types.BINARY)) {
            // dp("no where clause for lob/efile datatypes... ");

            return false;
        }

        String excludeFromWhere = prop.getExtraAttribute(extraProperty);
        if (excludeFromWhere != null) {
            if (TRUE.equalsIgnoreCase(excludeFromWhere) || Y.equalsIgnoreCase(excludeFromWhere)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return true if this property should be excluded from the insert
     * statement.
     */
    public boolean inInsertClause(BeanProperty prop, PersistRequest request) {
        return inInsertUpdateClause(prop, BeanProperty.EXCLUDE_FROM_INSERT, request, false);
    }

    /**
     * Return true if this property should be excluded from the update
     * statement. Note that this doesn't include the where part of the update
     * statement.
     */
    public boolean inUpdateClause(BeanProperty prop, PersistRequest request) {
        return inInsertUpdateClause(prop, BeanProperty.EXCLUDE_FROM_UPDATE, request, true);
    }

    private boolean inInsertUpdateClause(BeanProperty prop, String extraProperty,
            PersistRequest request, boolean isUpdate) {

        if (!prop.isDbWrite()) {
            return false;
        }
        //if (isUpdate && prop.isUniqueId()){
        //    return false;
        //}
        
        String excludeFrom = prop.getExtraAttribute(extraProperty);
        if (excludeFrom != null) {
            if (TRUE.equalsIgnoreCase(excludeFrom) || Y.equalsIgnoreCase(excludeFrom)) {
                return false;
            }
        }
        if (excludeClobBlobFromInsertUpdate) {
            int columnDataType = prop.getDbType();
            if ((columnDataType == Types.CLOB) || (columnDataType == Types.BLOB)) {
                // dp("no external types in Update/Insert clause for ... ");

                return false;
            }
        }

        return true;
    }

    /**
     * set whether to exclude Clob and Blobs from insert update statements.
     * <p>
     * This is the case for Oracle9 where these are executed as separate
     * sql statements.
     * </p>
     */
    public void setExcludeClobBlobFromInsertUpdate(boolean excludeThem) {
        excludeClobBlobFromInsertUpdate = excludeThem;
    }


}

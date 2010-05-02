/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebean;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RawSql {

    private final Sql sql;

    private final ColumnMapping columnMapping;

    protected RawSql(Sql sql, ColumnMapping columnMapping) {
        this.sql = sql;
        this.columnMapping = columnMapping;
    }

    public Sql getSql() {
        return sql;
    }

    public ColumnMapping getColumnMapping() {
        return columnMapping;
    }

    public int queryHash() {
        return 31 * sql.queryHash() + columnMapping.queryHash();
    }
    
    /**
     * Represents the sql part of the query. For parsed RawSql the sql is broken
     * up so that Ebean can insert extra WHERE and HAVING expressions into the
     * SQL.
     */
    public static final class Sql {

        private final boolean parsed;

        private final String unparsedSql;

        private final String preFrom;

        private final String preWhere;

        private final boolean andWhereExpr;

        private final String preHaving;

        private final boolean andHavingExpr;

        private final String orderBy;

        private final int queryHashCode;
        
        /**
         * Construct for unparsed SQL.
         */
        protected Sql(String unparsedSql) {
            this.queryHashCode = unparsedSql.hashCode();
            this.parsed = false;
            this.unparsedSql = unparsedSql;
            this.preFrom = null;
            this.preHaving = null;
            this.preWhere = null;
            this.andHavingExpr = false;
            this.andWhereExpr = false;
            this.orderBy = null;
        }

        /**
         * Construct for parsed SQL.
         */
        protected Sql(int queryHashCode, String preFrom, String preWhere, boolean andWhereExpr, String preHaving, boolean andHavingExpr,
                String orderBy) {

            this.queryHashCode = queryHashCode;
            this.parsed = true;
            this.unparsedSql = null;
            this.preFrom = preFrom;
            this.preHaving = preHaving;
            this.preWhere = preWhere;
            this.andHavingExpr = andHavingExpr;
            this.andWhereExpr = andWhereExpr;
            this.orderBy = orderBy;
        }
        
        public int queryHash() {
            return queryHashCode;
        }

        public String toString() {
            if (!parsed) {
                return "unparsed[" + unparsedSql + "]";
            }
            return "select[" + preFrom + "] preWhere[" + preWhere + "] preHaving[" + preHaving + "] orderBy[" + orderBy
                    + "]";
        }

        /**
         * Return true if the SQL is left completely unmodified.
         * <p>
         * This means Ebean can't add WHERE or HAVING expressions into the query
         * - it will be left completely unmodified.
         * </p>
         */
        public boolean isParsed() {
            return parsed;
        }

        public String getUnparsedSql() {
            return unparsedSql;
        }

        public String getPreFrom() {
            return preFrom;
        }

        public String getPreWhere() {
            return preWhere;
        }

        public boolean isAndWhereExpr() {
            return andWhereExpr;
        }

        public String getPreHaving() {
            return preHaving;
        }

        public boolean isAndHavingExpr() {
            return andHavingExpr;
        }

        public String getOrderBy() {
            return orderBy;
        }

    }

    /**
     * Defines the column mapping for raw sql DB columns to bean properties.
     */
    public static final class ColumnMapping {

        private final LinkedHashMap<String, Column> dbColumnMap;

        private final Map<String, String> propertyMap;
        private final Map<String, Column> propertyColumnMap;
        
        private final boolean parsed;
        
        private final boolean immutable;
        
        private final int queryHashCode;
        
        /**
         * Construct from parsed sql where the columns have been identified.
         */
        protected ColumnMapping(List<Column> columns) {
            this.queryHashCode = 0;
            this.immutable = false;
            this.parsed = true;
            this.propertyMap = null;
            this.propertyColumnMap = null;
            this.dbColumnMap = new LinkedHashMap<String, Column>();
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
                dbColumnMap.put(c.getDbColumn(), c);
            }
        }

        /**
         * Construct for unparsed sql.
         */
        protected ColumnMapping() {
            this.queryHashCode = 0;
            this.immutable = false;
            this.parsed = false;
            this.propertyMap = null;
            this.propertyColumnMap = null;
            this.dbColumnMap = new LinkedHashMap<String, Column>();
        }

        /**
         * Construct an immutable ColumnMapping based on collected information.
         */
        protected ColumnMapping(boolean parsed, LinkedHashMap<String, Column> dbColumnMap) {
            this.immutable = true;
            this.parsed = parsed;
            this.dbColumnMap = dbColumnMap;
            
            int hc = ColumnMapping.class.getName().hashCode();
            
            HashMap<String, Column> pcMap = new HashMap<String, Column>();
            HashMap<String, String> pMap = new HashMap<String, String>();
            
            for (Column c: dbColumnMap.values()) {
                pMap.put(c.getPropertyName(), c.getDbColumn());
                pcMap.put(c.getPropertyName(), c);
                
                hc = 31*hc + c.getPropertyName() == null ? 0 : c.getPropertyName().hashCode();
                hc = 31*hc + c.getDbColumn() == null ? 0 : c.getDbColumn().hashCode();
            }
            this.propertyMap = Collections.unmodifiableMap(pMap);
            this.propertyColumnMap = Collections.unmodifiableMap(pcMap);
            this.queryHashCode = hc;
        }
        
        /**
         * Creates an immutable copy of this ColumnMapping.
         * 
         * @throws IllegalStateException
         *             when a propertyName has not been defined for a column.
         */
        protected ColumnMapping createImmutableCopy() {
            
            for (Column c : dbColumnMap.values()) {
                c.checkMapping();
            }
            
            return new ColumnMapping(parsed, dbColumnMap);
        }

        protected void columnMapping(String dbColumn, String propertyName) {

            if (immutable){
                throw new IllegalStateException("Should never happen");
            }
            if (!parsed) {
                int pos = dbColumnMap.size();
                dbColumnMap.put(dbColumn, new Column(pos, dbColumn, null, propertyName));
            } else {
                Column column = dbColumnMap.get(dbColumn);
                if (column == null) {
                    String msg = "DB Column [" + dbColumn + "] not found in mapping. Expecting one of ["+ dbColumnMap.keySet() + "]";
                    throw new IllegalArgumentException(msg);
                }
                column.setPropertyName(propertyName);
            }
        }
        
        
        public int queryHash() {
            if (queryHashCode == 0){
                throw new RuntimeException("Bug: queryHashCode == 0");
            }
            return queryHashCode;
        }
        
        /**
         * Returns true if the Columns where supplied by parsing the sql select
         * clause.
         * <p>
         * In the case where the columns where parsed then we can do extra
         * checks on the column mapping such as, is the column a valid one in
         * the sql and whether all the columns in the sql have been mapped.
         * </p>
         */
        public boolean isParsed() {
            return parsed;
        }
        
        public int size() {
            return dbColumnMap.size();
        }
        
        protected Map<String,Column> mapping() {
            return dbColumnMap;
        }

        public Map<String,String> getMapping(){
            return propertyMap;
        }

        public int getIndexPosition(String property){
            Column c = propertyColumnMap.get(property);
            return c == null ? -1 : c.getIndexPos();
        }
        
        public Iterator<Column> getColumns() {
            return dbColumnMap.values().iterator();
        }
        
        public static class Column {

            private final int indexPos;
            private final String dbColumn;

            private final String dbAlias;

            private String propertyName;

            public Column(int indexPos, String dbColumn, String dbAlias) {
                this(indexPos, dbColumn, dbAlias, null);
            }

            private Column(int indexPos, String dbColumn, String dbAlias, String propertyName) {
                this.indexPos = indexPos;
                this.dbColumn = dbColumn;
                this.dbAlias = dbAlias;
                if (propertyName == null && dbAlias != null){
                    this.propertyName = dbAlias;                    
                } else {
                    this.propertyName = propertyName;
                }
            }

            private void checkMapping() {
                if (propertyName == null){
                    String msg = "No propertyName defined (Column mapping) for dbColumn ["+dbColumn+"]";
                    throw new IllegalStateException(msg);
                }
            }
            
            public String toString() {
                return dbColumn + "->" + propertyName;
            }

            public int getIndexPos() {
                return indexPos;
            }

            public String getDbColumn() {
                return dbColumn;
            }

            public String getDbAlias() {
                return dbAlias;
            }

            public String getPropertyName() {
                return propertyName;
            }

            private void setPropertyName(String propertyName) {
                this.propertyName = propertyName;
            }

        }
    }
}

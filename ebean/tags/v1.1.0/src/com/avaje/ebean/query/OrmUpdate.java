package com.avaje.ebean.query;

import com.avaje.ebean.Update;
import com.avaje.ebean.util.BindParams;

/**
 * Internal extension to the Update interface.
 */
public interface OrmUpdate<T> extends Update<T> {

	/**
	 * The type of the update request.
	 */
	enum OrmUpdateType {
		INSERT{
			public String toString() {
				return "Insert";
			}
		},
		UPDATE{
			public String toString() {
				return "Update";
			}
		}, 
		DELETE{
			public String toString() {
				return "Delete";
			}
		},
		UNKNOWN{
			public String toString() {
				return "Unknown";
			}
			
		};
	}
	
	/**
	 * Return the type of bean being updated.
	 */
	public Class<?> getBeanType();

	/**
	 * Return the type of this - insert, update or delete.
	 */
	public OrmUpdateType getOrmUpdateType();
	
	/**
	 * Return the name of the table being modified.
	 */
	public String getBaseTable();
	
	/**
	 * Return true if this is sql false if it needs to be translated into sql by converting bean types and property names
	 * to database tables and columns.
	 */
	public boolean isSql();
	
	/**
	 * Return the update statement. This could be either sql or an orm update with bean types and property names.
	 */
	public String getUpdateStatement();
	
	/**
	 * Return the timeout in seconds.
	 */
	public int getTimeout();
	
	/**
	 * Return true if the cache should be notified to invalidate objects.
	 */
	public boolean isNotifyCache();
	
	/**
	 * Return the bind parameters.
	 */
	public BindParams getBindParams();
	
	/**
	 * Set the generated sql used.
	 */
	public void setGeneratedSql(String sql);
}

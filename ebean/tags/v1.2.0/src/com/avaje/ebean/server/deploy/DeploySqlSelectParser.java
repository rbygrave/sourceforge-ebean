package com.avaje.ebean.server.deploy;

import com.avaje.ebean.NamingConvention;
import com.avaje.ebean.annotation.SqlSelect;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.plugin.PluginDbConfig;

/**
 * Parser used to handling sql-select queries and creating DeploySqlSelect objects.
 */
public class DeploySqlSelectParser {

	public static final String $_AND_HAVING = "${andHaving}";

	public static final String $_HAVING = "${having}";

	public static final String $_AND_WHERE = "${andWhere}";

	public static final String $_WHERE = "${where}";

	final NamingConvention namingConvention;

	public DeploySqlSelectParser(PluginDbConfig dbConfig) {
		this.namingConvention = dbConfig.getNamingConvention();
	}

	public static Meta createMeta(DeployBeanDescriptor<?> desc, SqlSelect sqlSelect) {
		Meta meta = Meta.create(sqlSelect);
		handleExtend(desc, meta);
		return meta;
	}

	public static Meta createMeta(DeployBeanDescriptor<?> desc, String name, String extend, String query, boolean debug,
			String where, String having, String columnMapping) {

		Meta meta = Meta.create(name, extend, query, debug, where, having, columnMapping);
		handleExtend(desc, meta);
		
		return meta;
		
	}
	
	public DeploySqlSelect parse(DeployBeanDescriptor<?> deployDesc, Meta sqlSelectMeta) {

		return new DefaultDeploySqlSelectParser(namingConvention, deployDesc, sqlSelectMeta).parse();
	}

	private static void handleExtend(DeployBeanDescriptor<?> deployDesc, Meta sqlSelectMeta) {
		
		String extend = sqlSelectMeta.extend;
		if (extend != null){
			DeployNamedQuery parentQuery = deployDesc.getNamedQueries().get(extend);
			if (parentQuery == null) {
				throw new RuntimeException("parent query ["+extend+"] not found for sql-select "+sqlSelectMeta.name);
			}
			DeploySqlSelect parentSqlSelect = parentQuery.getSqlSelect();
			// prepend query from parent
			String parentSql = parentSqlSelect.getQuery();
			
			sqlSelectMeta.extendQuery(parentSql);
			sqlSelectMeta.extendColumnMapping(parentSqlSelect.getColumnMapping());
		}
	}
	
	/**
	 * Meta data for a sql-select object.
	 * <p>
	 * Created from SqlSelect annotation or xml deployment.
	 * </p>
	 */
	public static class Meta {

		String name;
		String extend;
		String query;
		boolean debug;
		String where;
		String having;
		String columnMapping;

		/**
		 * Prepend sql from the parent query that this query 'extends'.
		 */
		public void extendQuery(String parentSql) {
			if (query == null) {
				query = parentSql;
			} else {
				query = parentSql + " " + query;
			}
		}

		public void extendColumnMapping(String parentColumnMapping) {
			if (columnMapping == null){
				columnMapping = parentColumnMapping;
			}
		}
		

		private static Meta create(SqlSelect sqlSelect) {
			Meta meta = new Meta();
			meta.debug = sqlSelect.debug();
			meta.name = sqlSelect.name();
			meta.extend = toNull(sqlSelect.extend());
			meta.having = toNull(sqlSelect.having());
			meta.where = toNull(sqlSelect.where());
			meta.columnMapping = toNull(sqlSelect.columnMapping());
			meta.query = toNull(sqlSelect.query());
			return meta;
		}

		private static Meta create(String name, String extend, String query, boolean debug,
				String where, String having, String columnMapping) {

			Meta meta = new Meta();
			meta.name = name;
			meta.extend = extend;
			meta.query = query;
			meta.debug = debug;
			meta.having = having;
			meta.where = where;
			meta.columnMapping = columnMapping;

			return meta;
		}
		

		private static String toNull(String s) {
			if (s != null && s.equals("")){
				return null;
			} else {
				return s;
			}
		}
	}
}

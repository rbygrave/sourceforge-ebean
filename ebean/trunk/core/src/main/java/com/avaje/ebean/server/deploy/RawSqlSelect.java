package com.avaje.ebean.server.deploy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.query.CQueryPredicates;
import com.avaje.ebean.server.query.SqlTree;
import com.avaje.ebean.server.query.SqlTreeNode;
import com.avaje.ebean.server.query.SqlTreeNodeRoot;
import com.avaje.ebean.server.query.SqlTreeProperties;

/**
 * Represents a SqlSelect raw sql query.
 */
public class RawSqlSelect {

	private static final Logger logger = Logger.getLogger(RawSqlSelect.class.getName());

	private final BeanDescriptor<?> desc;
	
	private final RawSqlColumnInfo[] selectColumns;

	private final String preWhereExprSql;

	private final boolean andWhereExpr;

	private final String preHavingExprSql;

	private final boolean andHavingExpr;

	private final String orderBySql;

	private final String whereClause;

	private final String havingClause;

	private final String query;

	private final String columnMapping;

	private final String name;
	
	private final SqlTree sqlTree;
	
	private boolean withId;

	private final String tableAlias;
	
	public RawSqlSelect(BeanDescriptor<?> desc, List<RawSqlColumnInfo> selectColumns,
			String tableAlias, String preWhereExprSql, boolean andWhereExpr, String preHavingExprSql,
			boolean andHavingExpr, String orderBySql, RawSqlMeta meta) {

		this.desc = desc;
		this.tableAlias = tableAlias;
		this.selectColumns = selectColumns.toArray(new RawSqlColumnInfo[selectColumns.size()]);
		this.preHavingExprSql = preHavingExprSql;
		this.preWhereExprSql = preWhereExprSql;
		this.andHavingExpr = andHavingExpr;
		this.andWhereExpr = andWhereExpr;
		this.orderBySql = orderBySql;
		this.name = meta.getName();
		this.whereClause = meta.getWhere();
		this.havingClause = meta.getHaving();
		this.query = meta.getQuery();
		this.columnMapping = meta.getColumnMapping();
		
		this.sqlTree = initialise(desc);
	}

	/**
	 * Find foreign keys for assoc one types and build SqlTree.
	 */
	private SqlTree initialise(BeanDescriptor<?> owner){
		
		try {
//			List<PropertyDeploy> fkAdditions = new ArrayList<PropertyDeploy>();
//			
//			Iterator<PropertyDeploy> it = deployPropMap.values().iterator();
//			while (it.hasNext()) {
//				PropertyDeploy propertyDeploy = it.next();
//				if (propertyDeploy.isForeignKey()){
//					
//					String logicalFk = propertyDeploy.getLogical();
//					BeanPropertyAssocOne<?> property = (BeanPropertyAssocOne<?>)owner.getBeanProperty(logicalFk);
//					IdBinder idBinder = property.getTargetDescriptor().getIdBinder();
//					if (!idBinder.isComplexId()){
//						BeanProperty[] ids = idBinder.getProperties();
//						
//						String logicalFkImported = logicalFk+"."+ ids[0].getName();
//						PropertyDeploy fkDeploy = propertyDeploy.createFkey(logicalFkImported);
//						fkAdditions.add(fkDeploy);
//					}
//				}			
//			}
//			
//			for (PropertyDeploy fkDeploy : fkAdditions) {
//				if (logger.isLoggable(Level.FINER)){
//					String m = "... adding foreign key  on "+owner+" query "+name+" "+fkDeploy;
//					logger.finer(m);
//				}
//				deployPropMap.put(fkDeploy.getLogical(), fkDeploy);
//			}
			
			return buildSqlTree(owner);
			
		} catch (Exception e){
			String m = "Bug? initialising query "+name+" on "+owner;
			throw new RuntimeException(m, e);
		}
	}

	public String getTableAlias() {
		return tableAlias;
	}

	/**
	 * Build the SqlTree for this query.
	 * <p>
	 * Most commonly this is just a simple list of properties - aka flat, but it
	 * could be a real object graph tree for more complex scenarios.
	 * </p>
	 */
	private SqlTree buildSqlTree(BeanDescriptor<?> desc){


		SqlTree sqlTree = new SqlTree();
		sqlTree.setSummary("[" + desc.getFullName() + "]");

		LinkedHashSet<String> includedProps = new LinkedHashSet<String>();
		SqlTreeProperties selectProps = new SqlTreeProperties();

		for (int i = 0; i < selectColumns.length; i++) {

			RawSqlColumnInfo columnInfo = selectColumns[i];
			String propName = columnInfo.getPropertyName();
			BeanProperty beanProperty = desc.getBeanProperty(propName);
			if (beanProperty != null) {
				if (beanProperty.isId()){
					if (i > 0){
						String m = "With "+desc+" query:"+name+" the ID is not the first column in the select. It must be...";
						throw new PersistenceException(m);
					} else {
						withId = true;
					}
				} else {
					includedProps.add(beanProperty.getName());
					selectProps.add(beanProperty);
				}
				
				
			} else {
				String m = "Mapping for " + desc.getFullName();
				m += " query["+name+"] column[" + columnInfo + "] index[" + i;
				m += "] not matched to bean property?";
				logger.log(Level.SEVERE, m);
			}
		}

		selectProps.setIncludedProperties(includedProps);
		SqlTreeNode sqlRoot = new SqlTreeNodeRoot(false, desc, selectProps, null, withId, null);
		sqlTree.setRootNode(sqlRoot);

		return sqlTree;
	}

	/**
	 * Build the full SQL Select statement for the request.
	 */
	public String buildSql(String orderBy, CQueryPredicates predicates, OrmQueryRequest<?> request) {


		StringBuilder sb = new StringBuilder();
		sb.append(preWhereExprSql);
		sb.append(" ");

		String dynamicWhere = null;
		if (request.getQuery().getId() != null) {
			// need to convert this as well. This avoids the
			// assumption that id has its proper dbColumn assigned
			// which may change if using multiple raw sql statements
			// against the same bean.
			BeanDescriptor<?> descriptor = request.getBeanDescriptor();
			//FIXME: I think this is broken... needs to be logical 
			// and then parsed for RawSqlSelect...
			dynamicWhere = descriptor.getBindIdSql();
		}

		String dbWhere = predicates.getDbWhere();
		if (dbWhere != null && dbWhere.length() > 0) {
			if (dynamicWhere == null) {
				dynamicWhere = dbWhere;
			} else {
				dynamicWhere += " and " + dbWhere;
			}
		}

		if (dynamicWhere != null) {
			if (andWhereExpr) {
				sb.append(" and ");
			} else {
				sb.append(" where ");
			}
			sb.append(dynamicWhere);
			sb.append(" ");
		}

		if (preHavingExprSql != null) {
			sb.append(preHavingExprSql);
			sb.append(" ");
		}

		String dbHaving = predicates.getDbHaving();
		
		if (dbHaving != null) {
			if (andHavingExpr) {
				sb.append(" and ");
			} else {
				sb.append(" having ");
			}
			sb.append(dbHaving);
			sb.append(" ");
		}

		if (orderBy != null) {
			sb.append(" order by ").append(orderBy);
		}

		return sb.toString();
	}

	public String getOrderBy(CQueryPredicates predicates) {
		String orderBy = predicates.getDbOrderBy();
		if (orderBy != null) {
			return orderBy;
		} else {
			return orderBySql;			
		}
	}
	
	
	public String getName() {
		return name;
	}

	public SqlTree getSqlTree() {
		return sqlTree;
	}

	public boolean isWithId() {
		return withId;
	}

	public String getQuery() {
		return query;
	}

	public String getColumnMapping() {
		return columnMapping;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public String getHavingClause() {
		return havingClause;
	}

	public String toString() {
		return Arrays.toString(selectColumns);
	}

	public DeployPropertyParser createDeployPropertyParser() {
		return new DeployPropertyParser(desc);
	}
	
}

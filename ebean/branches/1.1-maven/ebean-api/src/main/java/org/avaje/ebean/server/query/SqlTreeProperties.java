/**
 * 
 */
package org.avaje.ebean.server.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.avaje.ebean.server.deploy.BeanProperty;
import org.avaje.ebean.server.deploy.TableJoin;

/**
 * The select properties for a node in the SqlTree.
 */
public class SqlTreeProperties {

	/**
	 * The included Properties that will be used by EntityBeanIntercept
	 * to determine lazy loading on partial objects.
	 */
	Set<String> includedProps;

	/**
	 * True if this node of the tree should have read only entity beans.
	 */
	boolean readOnly;

	/**
	 * set to false if the id field is not included.
	 */
	boolean includeId = true;

	TableJoin[] tableJoins = new TableJoin[0];

	/**
	 * The bean properties in order.
	 */
	List<BeanProperty> propsList = new ArrayList<BeanProperty>();

	public SqlTreeProperties() {

	}

	public void add(BeanProperty[] props) {
		for (BeanProperty beanProperty : props) {
			propsList.add(beanProperty);
		}
	}

	public void add(BeanProperty prop) {
		propsList.add(prop);
	}
	
	public BeanProperty[] getProps() {
		return propsList.toArray(new BeanProperty[propsList.size()]);
	}

	public boolean isIncludeId() {
		return includeId;
	}

	public void setIncludeId(boolean includeId) {
		this.includeId = includeId;
	}

	public boolean isPartialObject() {
		return includedProps != null;
	}
	
	public Set<String> getIncludedProperties() {
		return includedProps;
	}

	public void setIncludedProperties(Set<String> includedProps) {
		this.includedProps = includedProps;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public TableJoin[] getTableJoins() {
		return tableJoins;
	}

	public void setTableJoins(TableJoin[] tableJoins) {
		this.tableJoins = tableJoins;
	}

}
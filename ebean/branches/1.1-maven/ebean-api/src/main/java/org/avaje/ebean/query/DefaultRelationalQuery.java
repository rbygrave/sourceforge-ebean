package org.avaje.ebean.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.avaje.ebean.EbeanServer;
import org.avaje.ebean.MapBean;
import org.avaje.ebean.SqlQuery;
import org.avaje.ebean.SqlQueryListener;
import org.avaje.ebean.util.BindParams;

/**
 * Default implementation of SQuery - SQL Query.
 */
public class DefaultRelationalQuery implements RelationalQuery {

	private static final long serialVersionUID = -1098305779779591069L;

	transient EbeanServer server;

    transient SqlQueryListener queryListener;

	/**
	 * Sql Query statement.
	 */
	String query;
		
	int firstRow;
	
	int maxRows;

	int timeout;
	
    /**
	 * The rows after which the fetch continues in a bg thread.
	 */
	int backgroundFetchAfter;

	/**
	 * Used to increase the initial capacity of the list set or map being
	 * fetched. Useful if fetching a large amount of data into a Map or Set to
	 * reduce rehashing.
	 */
	int initialCapacity;

	/**
	 * The property used to get the key value for a Map.
	 */
	String mapKey;
	    
    /**
     * Bind parameters when using the query language.
     */
    BindParams bindParams = new BindParams();
    
    String baseTable;
    
	/**
	 * Additional supply a query detail object.
	 */
	public DefaultRelationalQuery(EbeanServer server, String query) {
		this.server = server;
		this.query = query;
	}
    
	public DefaultRelationalQuery setQuery(String query) {
		this.query = query;
		return this;
	}
	
	public List<MapBean> findList() {
		return server.findList(this, null);
	}	
    
	public Set<MapBean> findSet() {
		return server.findSet(this, null);
	}
	
	public Map<?,MapBean> findMap() {
		return server.findMap(this, null);
	}
	
	public MapBean findUnique() {
		return server.findUnique(this, null);
	}
	
    public SqlQuery setBaseTable(String baseTable) {
		this.baseTable = baseTable;
		return this;
	}
    
	public String getBaseTable() {
		return baseTable;
	}

	public DefaultRelationalQuery setParameter(int position, Object value) {
        bindParams.setParameter(position, value);
        return this;
    }
    
    public DefaultRelationalQuery setParameter(String paramName, Object value) {
    	 bindParams.setParameter(paramName, value);
        return this;
    }
    
    public DefaultRelationalQuery set(int position, Object value) {
        return setParameter(position, value);
    }
    
    public DefaultRelationalQuery set(String name, Object value) {
    	 return setParameter(name, value);
    }
    
    public DefaultRelationalQuery bind(int position, Object value) {
    	 return setParameter(position, value);
    }
    
    public DefaultRelationalQuery bind(String name, Object value) {
    	 return setParameter(name, value);
    }

    /**
     * Return the findListener is one has been set.
     */
    public SqlQueryListener getListener() {
		return queryListener;
	}

    /**
     * Set a listener. This is designed for large fetches
     * where lots are rows are to be processed and instead of
     * returning all the rows they are processed one at a time.
     * <p>
     * Note that the returning List Set or Map will be empty.
     * </p>
     */
	public DefaultRelationalQuery setListener(SqlQueryListener queryListener) {
		this.queryListener = queryListener;
		return this;
	}
    
    public String toString() {
    	return "SqlQuery ["+query+"]";
    }

	public int getFirstRow() {
		return firstRow;
	}

	public DefaultRelationalQuery setFirstRow(int firstRow) {
		this.firstRow = firstRow;
		return this;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public DefaultRelationalQuery setMaxRows(int maxRows) {
		this.maxRows = maxRows;
		return this;
	}

	public String getMapKey() {
		return mapKey;
	}

	public DefaultRelationalQuery setMapKey(String mapKey) {
		this.mapKey = mapKey;
		return this;
	}

	public int getBackgroundFetchAfter() {
		return backgroundFetchAfter;
	}

	public DefaultRelationalQuery setBackgroundFetchAfter(int backgroundFetchAfter) {
		this.backgroundFetchAfter = backgroundFetchAfter;
		return this;
	}

	public int getInitialCapacity() {
		return initialCapacity;
	}
	
	public int getTimeout() {
		return timeout;
	}

	public DefaultRelationalQuery setInitialCapacity(int initialCapacity) {
		this.initialCapacity = initialCapacity;
		return this;
	}
	
	public DefaultRelationalQuery setTimeout(int secs) {
		this.timeout = secs;
		return this;
	}

	public BindParams getBindParams() {
		return bindParams;
	}

	
	public String getQuery() {
		return query;
	}    
}

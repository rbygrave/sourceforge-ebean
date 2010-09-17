package com.avaje.ebeaninternal.server.query;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.avaje.ebeaninternal.server.deploy.parse.SqlReservedWords;

/**
 * Special Map of the logical property joins to table alias.
 * 
 * @author rbygrave
 */
public class SqlTreeAlias {

	private static final String alphabet = "abcdefghijklmnopqrstuvwxy";
	
	private TreeSet<String> joinProps = new TreeSet<String>();

	private TreeSet<String> manyWhereJoinProps = new TreeSet<String>();

	private HashMap<String,String> aliasMap = new HashMap<String,String>();

    private HashMap<String, String> manyWhereAliasMap = new HashMap<String, String>();

	private final String rootTableAlias;
	
	public SqlTreeAlias(String rootTableAlias) {
		this.rootTableAlias = rootTableAlias;
	}

	/**
	 * Add joins to support where predicates 
	 * @param manyWhereJoins
	 */
    public void addManyWhereJoins(Set<String> manyWhereJoins) {
        if (manyWhereJoins != null){
            for (String include : manyWhereJoins) {
                addPropertyJoin(include, manyWhereJoinProps);
            }
        }
    }
   
	/**
	 * Add joins.
	 */
	public void addJoin(Set<String> propJoins) {
		if (propJoins != null){
			for (String propJoin : propJoins) {
				addPropertyJoin(propJoin, joinProps);
			}
		}
	}
	
	private void addPropertyJoin(String include, TreeSet<String> set){
		set.add(include);
		String[] split = SplitName.split(include);
		if (split[0] != null){
			addPropertyJoin(split[0], set);
		}
	}
	
	/**
	 * Build a set of table alias for the given bean and fetch
	 * joined properties.
	 */
	public void buildAlias() {
				
		Iterator<String> i = joinProps.iterator();
		while (i.hasNext()) {
			calcAlias(i.next());
		}

        i = manyWhereJoinProps.iterator();
        while (i.hasNext()) {
            calcAliasManyWhere(i.next());
        }
	}
	
	private String calcAlias(String prefix) {
		
		String[] split = SplitName.split(prefix);
		String attempt = parentAlias(split[0])+split[1].charAt(0);
		String alias = nextAlias(attempt);
		aliasMap.put(prefix, alias);
		return alias;
	}

    private String calcAliasManyWhere(String prefix) {

        String[] split = SplitName.split(prefix);
        // "x" for "extra join" to help identify them
        String attempt = "x"+ split[1].charAt(0);
        String alias = nextAlias(attempt);
        
        manyWhereAliasMap.put(prefix, alias);
        return alias;
    }
	
	private String parentAlias(String s) {
		if (s == null){
			return rootTableAlias;
		} else {
			return aliasMap.get(s);
		}
	}
	
	/**
	 * Return the table alias for a given property name.
	 */
	public String getTableAlias(String prefix){
		if (prefix == null){
			return rootTableAlias;
		} else {
			String s = aliasMap.get(prefix);
			if (s == null){
				return calcAlias(prefix);
			}
			return s;
		}
	}

    /**
     * Return an alias using "Many where joins".
     */
    public String getTableAliasManyWhere(String prefix) {
        if (prefix == null){
            return rootTableAlias;
        } 
        String s = manyWhereAliasMap.get(prefix);
        if (s == null){
            s = aliasMap.get(prefix);
        }
        if (s == null) {
            String msg = "Could not determine table alias for [" + prefix + "] manyMap["
                + manyWhereAliasMap + "] aliasMap[" + aliasMap + "]";
            throw new RuntimeException(msg);
        }
        return s;
    }

    /**
     * Parse for where clauses that uses "Many where joins"
     */
    public String parseWhere(String clause) {
        clause = parseRootAlias(clause);
        clause = parseAliasMap(clause, manyWhereAliasMap);
        return parseAliasMap(clause, aliasMap);
    }

    /**
     * Parse without using any extra "Many where joins".
     */
    public String parse(String clause) {
        clause = parseRootAlias(clause);
        return parseAliasMap(clause, aliasMap);
    }

    /**
     * Parse the clause replacing the table alias place holders.
     */
    private String parseRootAlias(String clause) {
    
        if (rootTableAlias == null){
            return clause.replace("${}", "");
        } else {
            return clause.replace("${}", rootTableAlias+".");         
        }
    }
    
	/**
	 * Parse the clause replacing the table alias place holders.
	 */
	private String parseAliasMap(String clause, HashMap<String,String> parseAliasMap) {

		Iterator<Entry<String, String>> i = parseAliasMap.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String,String> e = i.next();
			String k = "${"+e.getKey()+"}";
			clause  = clause.replace(k, e.getValue()+".");
		}
		
		return clause;
	}
	

	/**
	 * Return the next valid table alias given the preferred table alias.
	 */
	private String nextAlias(String prefAlias){
		
		if (validAlias(prefAlias)){
			return prefAlias;
		}
		String prefix;
		if (prefAlias.length()>1){
			prefix = prefAlias.substring(0,prefAlias.length()-1);
		} else{
			prefix = "";
		}
		
		for (int i = 0; i < 26; i++) {
			String test = prefix+alphabet.charAt(i);
			if (validAlias(test)){
				return test;
			}
		}
		
		return nextAlias(prefAlias+"z");
	}
	
	private boolean validAlias(String alias) {
	    boolean v = !SqlReservedWords.isKeyword(alias) 
			&& !aliasMap.containsValue(alias)
			&& !manyWhereAliasMap.containsValue(alias);
	    
	    if (v && manyWhereAliasMap.containsValue(alias)){
	        System.out.println("BUG");
	    }
	    return v;
	}
}

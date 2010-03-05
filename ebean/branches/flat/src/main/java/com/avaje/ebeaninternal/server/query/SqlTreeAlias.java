package com.avaje.ebeaninternal.server.query;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.avaje.ebeaninternal.server.deploy.parse.SqlReservedWords;

public class SqlTreeAlias {

	private static final String alphabet = "abcdefghijklmnopqrstuvwxy";
	
	private TreeSet<String> includes = new TreeSet<String>();
	
	private HashMap<String,String> aliasMap = new HashMap<String,String>();
	
	private final String rootTableAlias;
	
	public SqlTreeAlias(String rootTableAlias) {
		this.rootTableAlias = rootTableAlias;
	}

	/**
	 * Add fetch join includes.
	 */
	public void add(Set<String> includes) {
		if (includes != null){
			for (String include : includes) {
				addInclude(include);
			}
		}
	}
	
	private void addInclude(String include){
		//include = include.toLowerCase();
		includes.add(include);
		String[] split = SplitName.split(include);
		if (split[0] != null){
			addInclude(split[0]);
		}
	}
	
	/**
	 * Build a set of table alias for the given bean and fetch
	 * joined properties.
	 */
	public void buildAlias() {
				
		Iterator<String> i = includes.iterator();
		while (i.hasNext()) {
			calcAlias(i.next());
		}
	}
	
	private String calcAlias(String prefix) {
		
		String[] split = SplitName.split(prefix);
		String attempt = parentAlias(split[0])+split[1].charAt(0);
		String alias = nextAlias(attempt);
		aliasMap.put(prefix, alias);
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
			//prefix = prefix.toLowerCase();
			String s = aliasMap.get(prefix);
			if (s == null){
				return calcAlias(prefix);
				//throw new RuntimeException("No alias for "+prefix);
			}
			return s;
		}
	}
	
	/**
	 * Parse the clause replacing the table alias place holders.
	 */
	public String parse(String clause) {
	
		if (rootTableAlias == null){
			clause = clause.replace("${}", "");
		} else {
			clause = clause.replace("${}", rootTableAlias+".");			
		}

		Iterator<Entry<String, String>> i = aliasMap.entrySet().iterator();
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
		return !SqlReservedWords.isKeyword(alias) 
			&& !aliasMap.containsValue(alias);
	}
}

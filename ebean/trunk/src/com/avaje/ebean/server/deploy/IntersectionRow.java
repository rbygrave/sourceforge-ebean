package com.avaje.ebean.server.deploy;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlUpdate;

public class IntersectionRow {

	String tableName;
	
	Map<String,Object> values = new LinkedHashMap<String,Object>();
	
	public IntersectionRow(String tableName){
		this.tableName = tableName;
	}
	
	public void put(String key, Object value){
		values.put(key, value);
	}
	
	public SqlUpdate createInsert(EbeanServer server){


		SqlUpdate insert = server.createSqlUpdate();
		

		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tableName).append(" (");
		
		int count = 0;
		Iterator<Entry<String, Object>> it = values.entrySet().iterator();
		while (it.hasNext()) {
			if (count++ > 0){
				sb.append(", ");
			}

			Map.Entry<String, Object> entry = it.next();
			sb.append(entry.getKey());
			
			insert.setParameter(count, entry.getValue());
		}
		
		sb.append(") values (");
		for (int i = 0; i < count; i++) {
			if (i > 0){
				sb.append(", ");
			}
			sb.append("?");
		}
		sb.append(")");
		
		insert.setSql(sb.toString());
		

		return insert;
	}

	public SqlUpdate createDelete(EbeanServer server){


		SqlUpdate delete = server.createSqlUpdate();
		

		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(tableName).append(" where ");
		
		int count = 0;
		Iterator<Entry<String, Object>> it = values.entrySet().iterator();
		while (it.hasNext()) {
			if (count++ > 0){
				sb.append(", ");
			}
			Map.Entry<String, Object> entry = it.next();
			
			sb.append(entry.getKey());
			sb.append(" = ?");
			
			delete.setParameter(count, entry.getValue());
		}
		
		delete.setSql(sb.toString());
		

		return delete;
	}

}

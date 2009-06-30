package com.avaje.ebean.server.deploy;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.util.BindParams;

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


		BindParams bindParams = new BindParams();
		
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
			
			bindParams.setParameter(count, entry.getValue());
		}
		
		sb.append(") values (");
		for (int i = 0; i < count; i++) {
			if (i > 0){
				sb.append(", ");
			}
			sb.append("?");
		}
		sb.append(")");
		
		return new SqlUpdate(server, sb.toString(), bindParams);
	}

	public SqlUpdate createDelete(EbeanServer server){


		BindParams bindParams = new BindParams();
		

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
			
			bindParams.setParameter(count, entry.getValue());
		}
		
		return new SqlUpdate(server, sb.toString(), bindParams);		
	}

}

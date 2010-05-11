package com.avaje.ebeaninternal.server.deploy;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebeaninternal.api.BindParams;
import com.avaje.ebeaninternal.server.core.DefaultSqlUpdate;

public class IntersectionRow {

	private final String tableName;

	private final LinkedHashMap<String,Object> values = new LinkedHashMap<String,Object>();

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

		return new DefaultSqlUpdate(server, sb.toString(), bindParams);
	}

	public SqlUpdate createDelete(EbeanServer server){


		BindParams bindParams = new BindParams();


		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(tableName).append(" where ");

		int count = 0;
		Iterator<Entry<String, Object>> it = values.entrySet().iterator();
		while (it.hasNext()) {
			if (count++ > 0){
				sb.append(" and ");
			}

			Map.Entry<String, Object> entry = it.next();

			sb.append(entry.getKey());
			sb.append(" = ?");

			bindParams.setParameter(count, entry.getValue());
		}

		return new DefaultSqlUpdate(server, sb.toString(), bindParams);
	}

    public SqlUpdate createDeleteChildren(EbeanServer server) {

        BindParams bindParams = new BindParams();

        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(tableName).append(" where ");

        int count = 0;
        Iterator<Entry<String, Object>> it = values.entrySet().iterator();
        while (it.hasNext()) {
            if (count++ > 0) {
                sb.append(" and ");
            }

            Map.Entry<String, Object> entry = it.next();

            sb.append(entry.getKey());
            sb.append(" = ?");

            bindParams.setParameter(count, entry.getValue());
        }

        return new DefaultSqlUpdate(server, sb.toString(), bindParams);
    }
}

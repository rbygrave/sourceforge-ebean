package com.avaje.ebean.server.ddl;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class DbTypeMap {

	Map<Integer, DbType> typeMap = new HashMap<Integer, DbType>();

	public DbTypeMap() {
		loadDefaults();
	}
	
	private void loadDefaults() {

		put(Types.BOOLEAN, new DbType("boolean"));

		put(Types.INTEGER, new DbType("integer"));
		put(Types.BIGINT, new DbType("bigint"));
		put(Types.REAL, new DbType("float"));
		put(Types.DOUBLE, new DbType("double"));
		put(Types.SMALLINT, new DbType("smallint"));
		put(Types.TINYINT, new DbType("tinyint"));
		put(Types.DECIMAL, new DbType("decimal",38));
		
		put(Types.VARCHAR, new DbType("varchar", 255));
		put(Types.CHAR, new DbType("char", 1));

		put(Types.BLOB, new DbType("blob"));
		put(Types.CLOB, new DbType("clob"));
		put(Types.LONGVARBINARY, new DbType("longvarbinary"));
		put(Types.LONGVARCHAR, new DbType("lonvarcahr"));
		put(Types.VARBINARY, new DbType("varbinary"));

		put(Types.DATE, new DbType("date"));
		put(Types.TIME, new DbType("time"));
		put(Types.TIMESTAMP, new DbType("timestamp"));

	}

	public void put(int jdbcType, DbType dbType) {
		typeMap.put(Integer.valueOf(jdbcType), dbType);
	}


	public DbType get(int jdbcType) {

		DbType dbType = typeMap.get(Integer.valueOf(jdbcType));
		if (dbType == null) {
			String m = "No DB type for JDBC type " + jdbcType;
			throw new RuntimeException(m);
		}

		return dbType;
	}
}

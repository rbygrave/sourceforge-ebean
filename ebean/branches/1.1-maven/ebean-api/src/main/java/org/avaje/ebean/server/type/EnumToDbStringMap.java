package org.avaje.ebean.server.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Used to map Enum values to database string/varchar values.
 */
public class EnumToDbStringMap extends EnumToDbValueMap<String> {

	
	
	@Override
	public int getDbType() {
		return Types.VARCHAR;
	}

	@Override
	public EnumToDbStringMap add(Object beanValue, String dbValue) {
		addInternal(beanValue, dbValue);
		return this;
	}

	@Override
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null){
			pstmt.setNull(index, Types.VARCHAR);
		} else {
			String s = getDbValue(value);
			pstmt.setString(index, s);
		}
		
	}

	@Override
	public Object read(ResultSet rset, int index) throws SQLException {
		String s = rset.getString(index);
		if (s == null){
			return null;
		} else {
			return getBeanValue(s);
		}
	}
	
}

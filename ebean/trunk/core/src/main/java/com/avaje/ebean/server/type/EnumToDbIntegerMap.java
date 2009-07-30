package com.avaje.ebean.server.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.persistence.PersistenceException;

/**
 * Used to map enum values to database integer values.
 */
public class EnumToDbIntegerMap extends EnumToDbValueMap<Integer> {

	@Override
	public int getDbType() {
		return Types.INTEGER;
	}

	@Override
	public EnumToDbIntegerMap add(Object beanValue, String stringDbValue) {

		try {
			Integer value = Integer.valueOf(stringDbValue);
			addInternal(beanValue, value);
			
			return this;

		} catch (Exception e) {
			String msg = "Error converted enum type[" + beanValue.getClass().getName();
			msg += "] enum value[" + beanValue + "] string value [" + stringDbValue + "]";
			msg += " to an Integer.";
			throw new PersistenceException(msg, e);
		}
	}

	@Override
	public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
		if (value == null) {
			pstmt.setNull(index, Types.INTEGER);
		} else {
			Integer s = getDbValue(value);
			pstmt.setInt(index, s);
		}

	}

	@Override
	public Object read(ResultSet rset, int index) throws SQLException {
		int i = rset.getInt(index);
		if (rset.wasNull()) {
			return null;
		} else {
			return getBeanValue(Integer.valueOf(i));
		}
	}

}

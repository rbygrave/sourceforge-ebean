package com.avaje.ebean.config.dbplatform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

public class DbSequenceIdGenerator implements IdGenerator {

	private static final Logger logger = Logger.getLogger(DbSequenceIdGenerator.class.getName());
	
	final String sql;
	
	final DataSource dataSource;
	
	public DbSequenceIdGenerator(DataSource dataSource, String sql) {
		this.dataSource = dataSource;
		this.sql = sql;
	}
	
	public Object nextId() {
		
		Connection c = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		try {
			c = dataSource.getConnection();
			pstmt = c.prepareStatement(sql);
			rset = pstmt.executeQuery();
			if (rset.next()){
				int val = rset.getInt(1);
				return Integer.valueOf(val);
			} else {
				String m = "Always expecting 1 row from "+sql;
				throw new PersistenceException(m);
			}
		} catch (SQLException e){
			throw new PersistenceException("Error getting sequence nextval", e);
			
		} finally {
			try {
				if (rset != null){
					rset.close();
				}
			} catch (SQLException e){
				logger.error("Error closing ResultSet", e);
			}
			try {
				if (pstmt != null){
					pstmt.close();
				}
			} catch (SQLException e){
				logger.error("Error closing PreparedStatement", e);
			}
			try {
				if (c != null){
					c.close();
				}
			} catch (SQLException e){
				logger.error("Error closing Connection", e);
			}
		}

	}

	
}

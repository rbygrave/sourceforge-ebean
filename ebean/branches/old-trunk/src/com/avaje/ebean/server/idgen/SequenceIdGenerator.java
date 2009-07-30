/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean.server.idgen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.config.dbplatform.IdGenerator;

/**
 * Uses Database sequences to generate unique ids.
 */
public class SequenceIdGenerator implements IdGenerator {

	private static final Logger logger = Logger.getLogger(SequenceIdGenerator.class.getName());
	
	/**
	 * The dataSource that has the db sequences.
	 */
	final DataSource dataSource;

	final String sql;

	/**
	 * Create the DbSequence with a given DataSource.
	 */
	public SequenceIdGenerator(String sql, DataSource dataSource) {
		this.dataSource = dataSource;
		this.sql = sql;
	}



	/**
	 * Returns the next sequence value deriving the sequence name from the
	 * BeanDescriptor deployment information.
	 */
	public Object nextId() {

		Connection connection = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		try {
			// get the default datasource
			connection = dataSource.getConnection();
			pstmt = connection.prepareStatement(sql);
			rset = pstmt.executeQuery();
			if (rset.next()) {
				int nextValue = rset.getInt(1);
				return Integer.valueOf(nextValue);
			} else {
				throw new PersistenceException("[" + sql + "] returned no rows?");
			}

		} catch (SQLException e) {
			throw new PersistenceException(e);

		} finally {
			try {
				if (connection != null) {
					connection.commit();
				}
				if (rset != null) {
					rset.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {
	        	logger.log(Level.SEVERE, null, e);

			} finally {
				try {
					if (connection != null) {
						connection.close();
					}
				} catch (SQLException ex) {
		        	logger.log(Level.SEVERE, null, ex);
				}
			}

		}
	}
}

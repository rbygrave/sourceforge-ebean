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

import com.avaje.ebean.server.core.IdGenerator;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Uses Database sequences to generate unique ids.
 * <p>
 * Also refer to SequenceNaming under ebean server naming.
 * </p>
 * Example configuration in System properties.
 * <pre><code>
 * 
 *     ## McKoi Sequences
 *     ebean.namingconvention.sequence.select=SELECT
 *     ebean.namingconvention.sequence.from=
 *     ebean.namingconvention.sequence.nextvalprefix=NEXTVAL('
 *     ebean.namingconvention.sequence.nextvalsuffix=')
 *      
 *     ## Oracle Sequences
 *     ebean.namingconvention.sequence.select=SELECT
 *     ebean.namingconvention.sequence.from=FROM DUAL
 *     ebean.namingconvention.sequence.nextvalprefix=
 *     ebean.namingconvention.sequence.nextvalsuffix=.NEXTVAL
 *     
 *     
 * </code></pre>
 */
public class DbSequence implements IdGenerator {

	private static final Logger logger = Logger.getLogger(DbSequence.class.getName());
	
	/**
	 * The dataSource that has the db sequences.
	 */
	DataSource dataSource;

	/**
	 * The prefix to prepend to derive the db sequence name.
	 */
	String selectClause;

	/**
	 * The suffix to append to derive the db sequence name.
	 */
	String fromClause;

	/**
	 * Create the DbSequence with a given DataSource.
	 */
	public DbSequence() {
	}

	public void configure(String name, InternalEbeanServer server) {
		
		PluginProperties props = server.getPlugin().getProperties();
		
		this.dataSource = server.getPlugin().getDbConfig().getDataSource();

		selectClause = props.getProperty("namingconvention.sequence.select", "SELECT");
		selectClause += " ";

		fromClause = props.getProperty("namingconvention.sequence.from", "");
		fromClause = " " + fromClause;

	}

	/**
	 * Returns the next sequence value deriving the sequence name from the
	 * BeanDescriptor deployment information.
	 */
	public Object nextId(BeanDescriptor desc) {
		String sql = getNextValSql(desc);
		return getResult(sql);
	}
	
	/**
	 * Derive the db sequence name by adding a prefix and suffix to the base
	 * table name.
	 */
	protected String getNextValSql(BeanDescriptor desc) {

		return selectClause + desc.getSequenceNextVal() + fromClause;
	}

	/**
	 * Returns the next sequence value using the sequence name.
	 */
	protected Object getResult(String sql) {

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

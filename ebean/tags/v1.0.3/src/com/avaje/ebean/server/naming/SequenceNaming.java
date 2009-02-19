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
package com.avaje.ebean.server.naming;

import java.util.List;

import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Defines prefix and suffix etc to determine a sequence name from a table and
 * potentially primary key. In addition has options for determining the
 * 'nextval' wrapping around the sequence name.
 * <p>
 * By default this adds '_SEQ' as a suffix to the table name. Oracle9Plugin etc
 * will specify the defaults for nextval wrapping appropriately but you can
 * override them in the properties file.
 * <p>
 * Note that this determines the name of the sequence and is intended to exclude
 * the nextval and currentval type wrapping of the sequence name. The nextval
 * and currentval type wrapping
 * </p>
 * 
 * <pre><code>
 *   ## McKoi Sequences
 *   ebean.namingconvention.sequence.nextvalprefix=NEXTVAL('
 *   ebean.namingconvention.sequence.nextvalsuffix=')
 *   
 *   ## Oracle Sequences
 *   ebean.namingconvention.sequence.nextvalprefix=
 *   ebean.namingconvention.sequence.nextvalsuffix=.NEXTVAL
 *   
 *   ## Add _SEQ to the table name to get the sequence name
 *   ## and do not include the primary key column 
 *   namingconvention.sequence.prefix=
 *   namingconvention.sequence.middle=
 *   namingconvention.sequence.suffix=_SEQ
 *   namingconvention.sequence.includecolumn=false
 * </code></pre>
 */
public class SequenceNaming {


	final PluginProperties properties;
	
	/**
	 * Create loading the prefix, suffix etc for db sequence name generation.
	 */
	public SequenceNaming(PluginProperties properties) {
		this.properties = properties;
	}

	/**
	 * Return the sequence name plus wrapping to get the next value.
	 */
	public String getNextVal(DeployBeanDescriptor desc) {
		String seqName = getName(desc);
		
		String nextvalPrefix = properties.getProperty("namingconvention.sequence.nextvalprefix", "");
		String nextvalSuffix = properties.getProperty("namingconvention.sequence.nextvalsuffix", "");

		return nextvalPrefix + seqName + nextvalSuffix;
	}

	/**
	 * Generate the database sequence name. Use the table name and potentially
	 * the column name to derive the sequence name. You can add prefix, suffix
	 * and a string to go between the table and column names.
	 */
	public String getName(DeployBeanDescriptor desc) {

		if (desc.getIdGeneratorName() != null) {
			// user provided generator name is correct for the db already
			// no need to do further processing
			return desc.getIdGeneratorName();
		}
		 
		String baseTable = desc.getBaseTable();
		String uidColumn = null;

		boolean includeColumn = properties.getPropertyBoolean("namingconvention.sequence.includecolumn", false);

		if (includeColumn) {
			List<DeployBeanProperty> uids = desc.propertiesId();
			if (uids.size() == 1) {
				if (!uids.get(0).isEmbedded()) {
					uidColumn = uids.get(0).getDbColumn();
				}
			}
		}

		return getName(baseTable, uidColumn);
	}

	public String getName(String table, String uidColumn) {


		String prefix = properties.getProperty("namingconvention.sequence.prefix", "");
		String suffix = properties.getProperty("namingconvention.sequence.suffix", "_SEQ");
		String middle = properties.getProperty("namingconvention.sequence.middle", "");

		boolean includeColumn = properties.getPropertyBoolean("namingconvention.sequence.includecolumn", false);
		
		if (includeColumn) {
			return prefix + table + middle + uidColumn + suffix;
		} else {
			return prefix + table + middle + suffix;
		}
	}

}

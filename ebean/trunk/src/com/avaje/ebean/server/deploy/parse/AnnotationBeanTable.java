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
package com.avaje.ebean.server.deploy.parse;

import com.avaje.ebean.config.naming.TableName;
import com.avaje.ebean.server.deploy.meta.DeployBeanTable;

/**
 * Read the annotations for BeanTable.
 * <p>
 * Refer to BeanTable but basically determining base table, table alias
 * and the unique id properties.
 * </p>
 */
public class AnnotationBeanTable extends AnnotationBase {

	final DeployBeanTable beanTable;

    public AnnotationBeanTable(DeployUtil util, DeployBeanTable beanTable){
    	super(util);
        this.beanTable = beanTable;
    }

    /**
     * Parse the annotations.
     */
    public void parse() {
    	
		TableName tableName = getTableName(beanTable.getBeanType());

		beanTable.setBaseTable(tableName.getQualifiedName());
    }
}

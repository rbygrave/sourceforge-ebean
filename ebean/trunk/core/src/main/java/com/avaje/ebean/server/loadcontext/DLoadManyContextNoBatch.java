/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebean.server.loadcontext;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.querydefn.OrmQueryProperties;

public class DLoadManyContextNoBatch extends DLoadManyContext {
	
	public DLoadManyContextNoBatch(DLoadContext parent, 
			BeanPropertyAssocMany<?> p, String path, OrmQueryProperties queryProps) {
		
		super(parent, p, path, 1, queryProps);
	}
	
	public void register(BeanCollection<?> bc){
		
		bc.setLoader(0, this);
	}

	public void loadMany(BeanCollection<?> bc) {
				
		parent.getEbeanServer().loadMany(bc, this);
	}
}

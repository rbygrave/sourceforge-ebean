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
package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.TxScope;
import com.avaje.ebean.TxType;
import com.avaje.ebean.annotation.EmbeddedColumns;
import com.avaje.ebean.annotation.Transactional;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.common.EnhancedTransactional;
import com.avaje.ebean.enhance.asm.Type;

/**
 * Constant values used in byte code generation.
 */
public interface EnhanceConstants {

	public static final String AVAJE_TRANSACTIONAL_ANNOTATION = "L"+Type.getInternalName(Transactional.class)+";";
	
	public static final String ENTITY_ANNOTATION = "Ljavax/persistence/Entity;";

	public static final String EMBEDDABLE_ANNOTATION = "Ljavax/persistence/Embeddable;";
	
	public static final String MAPPEDSUPERCLASS_ANNOTATION = "Ljavax/persistence/MappedSuperclass;";
	
	public static final String IDENTITY_FIELD = "_ebean_identity";

    public static final String INTERCEPT_FIELD = "_ebean_intercept";
    
    public static final String C_ENHANCEDTRANSACTIONAL = Type.getInternalName(EnhancedTransactional.class);
    
    public static final String C_ENTITYBEAN = Type.getInternalName(EntityBean.class);
    
    public static final String C_SCALAOBJECT = "scala/ScalaObject";
    
    public static final String C_GROOVYOBJECT = "groovy/lang/GroovyObject";
    
    public static final String C_INTERCEPT = Type.getInternalName(EntityBeanIntercept.class);

    public static final String L_INTERCEPT = "L"+Type.getInternalName(EntityBeanIntercept.class)+";";

    public static final String L_EmbeddedColumns = "L"+Type.getInternalName(EmbeddedColumns.class)+";";

    public static final String C_TXTYPE = Type.getInternalName(TxType.class);
    public static final String C_TXSCOPE = Type.getInternalName(TxScope.class);
    public static final String C_TXISOLATION = Type.getInternalName(TxIsolation.class);
    
    public static final String EBEAN_META_PREFIX = "com/avaje/ebean/meta/";

    public static final String EBEAN_PREFIX = "com/avaje/ebean/";

    /**
     * The suffix added to the super class name.
     */
    public static final String SUFFIX = "$$EntityBean";
}

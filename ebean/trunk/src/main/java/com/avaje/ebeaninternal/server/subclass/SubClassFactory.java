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
package com.avaje.ebeaninternal.server.subclass;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.enhance.agent.ClassPathClassBytesReader;
import com.avaje.ebean.enhance.agent.EnhanceConstants;
import com.avaje.ebean.enhance.agent.EnhanceContext;
import com.avaje.ebean.enhance.agent.LoaderAwareClassWriter;
import com.avaje.ebean.enhance.asm.ClassReader;
import com.avaje.ebean.enhance.asm.ClassWriter;

/**
 * Creates Classes that implement EntityBean for a given normal bean Class.
 * <p>
 * This dynamically creates a subclass of a normal bean class. The subclass has
 * method interception to handle the lazy loading of references and old values 
 * creation. 
 * </p>
 */
public class SubClassFactory extends ClassLoader implements EnhanceConstants, GenSuffix {
   
	private static final Logger logger = Logger.getLogger(SubClassFactory.class.getName());
	
	private static final int CLASS_WRITER_FLAGS = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;

	private final EnhanceContext enhanceContext;
	
	private final ClassLoader parentClassLoader;
	

    /**
     * Create with a given ClassLoader.
     */
    public SubClassFactory(ClassLoader parent, int logLevel) {
        super(parent);
        parentClassLoader = parent;
        
        ClassPathClassBytesReader reader = new ClassPathClassBytesReader(null);
        enhanceContext = new EnhanceContext(reader, true, "debug="+logLevel);
    }

    /**
     * Create a subclass for the given bean class that implements EntityBean interface.
     * <p>
     * The transientGetters is a list of getter methods that are considered
     * no persistent.  That is, when they are called the bean should NOT 
     * trigger creation of an 'old values' copy of the beans values.
     * </p>
     */
    public Class<?> create(Class<?> normalClass, String serverName) throws IOException {
                
        String subClassSuffix = EnhanceConstants.SUFFIX;
        if (serverName != null){
        	subClassSuffix += "$"+serverName;
        }
        
        // Note: these have periods rather than slashes
        String clsName = normalClass.getName();
        String subClsName = clsName+subClassSuffix;
        
        try {
            byte[] newClsBytes = subclassBytes(clsName, subClassSuffix);
              
            Class<?> newCls = defineClass(subClsName, newClsBytes, 0, newClsBytes.length);
            return newCls;
            
        } catch (IOException ex){
        	String m = "Error creating subclass for ["+clsName+"]";
        	logger.log(Level.SEVERE, m, ex);
            throw ex;
            
        } catch (Throwable ex){
        	String m = "Error creating subclass for ["+clsName+"]";
        	logger.log(Level.SEVERE, m, ex);
        	throw new RuntimeException(ex);
        }
    }
    
    
    /**
     * Return byte code for the subclass.
     * <p>
     * Note that if transientInfo is null, then no interception of getters or setters
     * takes place.
     * </p>
     */
    private byte[] subclassBytes(String className, String subClassSuffix)
        throws IOException {
    	
    	String resName = className.replace('.', '/')+".class";
    	
    	InputStream is  = getResourceAsStream(resName);
        
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new LoaderAwareClassWriter(CLASS_WRITER_FLAGS, getParent(), null);
		
		SubClassClassAdpater ca = new SubClassClassAdpater(subClassSuffix, cw, parentClassLoader, enhanceContext);
		if (ca.isLog(1)) {
			ca.log(" enhancing " + className+subClassSuffix);
		}

		cr.accept(ca, 0);
		
		return cw.toByteArray();
    }
}

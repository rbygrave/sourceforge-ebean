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
package org.avaje.ebean.server.deploy;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the type information for a Set/List or Map.
 */
public final class ManyType implements Serializable {

    static final long serialVersionUID = -817917715486897189L;
    
    public static final char MAP_CODE = 'm';
    
    public static final char LIST_CODE = 'l';
    
    public static final char SET_CODE = 's';
    
    /**
     * A generic Map type with no specific type.
     */
    public static final ManyType MAP = new ManyType(MAP_CODE,"Map");
    
    /**
     * A generic List type with no specific type.
     */
    public static final ManyType LIST = new ManyType(LIST_CODE,"List");
    
    /**
     * A generic Set type with no specific type.
     */
    public static final ManyType SET = new ManyType(SET_CODE,"Set");
    
    /**
     * Return a ManyType for the given class. Determines the generic type
     * of List Map or Set and whether it has a specific implementation type or not.
     */
    public static ManyType getManyType(Class<?> type) {
        ManyType manyType = getManyType(type, null);
        if (manyType != null) {
            return manyType;
        }
        
        Class<?>[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            manyType = getManyType(interfaces[i], type);
            if (manyType != null){
                return manyType;
            }
        }
        return null;
    }
    
    private static ManyType getManyType(Class<?> type, Class<?> specific) {
        if (type.equals(List.class)){
        	return LIST;
            //return new ManyType(LIST_CODE);//, specific);
        }
        if (type.equals(Set.class)){
        	return SET;
            //return new ManyType(SET_CODE);//, specific);
        } 
        if (type.equals(Map.class)){
        	return MAP;
            //return new ManyType(MAP_CODE);//, specific);
        }
        return null;
    }

    
    final char typeCode;
    
    final String typeName;

    private ManyType(char typeCode, String typeName) {
        this.typeCode = typeCode;
        this.typeName = typeName;
    }
    
    public int hashCode() {
    	return typeCode;
    }
    
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o instanceof ManyType) {
			return hashCode() == o.hashCode();
		}
		return false;
	}
	
    public char getCode() {
        return typeCode;
    }
    
    public String toString() {
    	return typeName;
    }
    
    public Class<?> getGenericType() {
        switch (typeCode) {
        case MAP_CODE: return Map.class;
        case LIST_CODE: return List.class;
        case SET_CODE: return Set.class;            
        default:
            throw new RuntimeException("Invalid code "+typeCode);
        }
    }
    
    public boolean isMap() {
        return typeCode == MAP_CODE;
    }
    
    public boolean isList() {
        return typeCode == LIST_CODE;
    }
    
    public boolean isSet() {
        return typeCode == SET_CODE;
    }
}

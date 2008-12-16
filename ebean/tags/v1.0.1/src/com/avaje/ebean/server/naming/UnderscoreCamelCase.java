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


/**
 * Converts from database column names with underscores.
 */
public class UnderscoreCamelCase implements PropertyNamingConvention {

    /**
     * Force toUnderscore to return in upper case.
     */
    boolean forceUpperCase;
    
    boolean digitsCompressed = true;
    
    /**
     * Create the UnderscoreNameConverter.
     */
    public UnderscoreCamelCase(){
    }
    
    /**
     * Force the db column name to be upper case.
     */
    public void setForceUpperCase(boolean forceUpperCase) {
        this.forceUpperCase = forceUpperCase;
    }
    
    public String toColumn(String camelCase){

        int lastUpper = -1;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isDigit(c)) {
                if (i > lastUpper+1 && !digitsCompressed){
                    sb.append("_");
                }
                sb.append(c);
                lastUpper = i;
                
            } else if (Character.isUpperCase(c)) {
                if (i > lastUpper+1){
                    sb.append("_");
                }
                sb.append(Character.toLowerCase(c));
                lastUpper = i;
            
            } else {
                sb.append(c);
            }
        }
        String ret = sb.toString();
        if (forceUpperCase){
            ret = ret.toUpperCase();
        } 
        return ret;
    }
    
    public String toPropertyName(String underscore){
        
        StringBuffer result = new StringBuffer();
        String[] vals = underscore.split("_");
        
        for (int i = 0; i < vals.length; i++) {
            String lower = vals[i].toLowerCase();
            if (i > 0){   
                char c = Character.toUpperCase(lower.charAt(0));
                result.append(c);
                result.append(lower.substring(1));
            } else {
                result.append(lower);
            }
        }
        
        return result.toString();
    }
}

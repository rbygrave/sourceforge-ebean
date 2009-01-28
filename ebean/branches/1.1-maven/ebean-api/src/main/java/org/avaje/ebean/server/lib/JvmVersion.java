/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.avaje.ebean.server.lib;


/**
 * Helper used to determine if JDK 1.5 features are available.
 */
public class JvmVersion {

    static boolean jdk15 = false;
    
    static double jdkVersion = 0;
    
    static {
        String specVersion = System.getProperty("java.specification.version");
        Double db = Double.valueOf(specVersion);
        jdkVersion = db.doubleValue();
        jdk15 = jdkVersion > 1.4d;
    }
   
    /**
     * Return true if the Jdk is 1.5 or greater.
     */
    public static boolean isVer15() {
        return jdk15;
    }
    
    /**
     * Return the "java.specification.version" from System Properties.
     */
    public double getJdkVersion() {
        return jdkVersion;
    }
}

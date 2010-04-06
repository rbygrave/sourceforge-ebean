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
package com.avaje.ebeaninternal.server.net;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A map of header name value pairs.
 * <p>
 * Used to define the headers for a request and a response from the server.
 * </p>
 */
public class Headers implements Serializable {

    static final long serialVersionUID = 2993350408394934473L;
    
    HashMap<String,String> map = new HashMap<String, String>();
    
    /**
     * A session object can be associated with the headers.
     * Typically set by a servlet after it has read the headers
     * from the inputstream.
     */
    transient Object session;
    
    /**
     * Used to construct a Child AttributeMap.
     */
    public Headers() {
    }

    /**
     * Return the key of object that should process this message. 
     */
    public String getProcessorId() {
        return get("PROCESSOR.ID");
    }
    
    /**
     * Set the key of the object that should process this message.
     */
    public void setProcesorId(String processorId){
        set("PROCESSOR.ID",processorId);
    }

    /**
     * Set the success status on this message.
     */
    public void setSuccess() {
        set("STATUS","SUCCESS");
    }

    /**
     * Return true if the success status was set.
     */
    public boolean isSuccess() {
        String s = get("STATUS");
        if (s != null){
            return s.endsWith("SUCCESS");
        }
        return false;
    }
    
    /**
     * Set the status to error and the source error.
     */
    public void setThrowable(Throwable ex){
        set("STATUS","ERROR");
        set("THROWABLE.CLASS", ex.getClass().getName());
        set("THROWABLE.MSG", ex.getMessage());
    }

    /**
     * Return the class of the error.
     */
    public String getThrowableClass() {
        return get("THROWABLE.CLASS");
    }

    /**
     * Return the message text of the error.
     */
    public String getThrowableMessage() {
        return get("THROWABLE.MSG");
    }
    
    /**
     * Set a header value.
     * @param key the name of the parameter
     * @param value the value of the parameter.
     */
    public void set(String key, String value){
        map.put(key.toUpperCase(), value);
    }
    
    /**
     * Set a header value with an int value.
     */
    public void set(String key, int value){
        map.put(key.toUpperCase(), ""+value);
    }
    
    /**
     * Return the value of a specific header.
     */
    public String get(String key){
        return (String)map.get(key.toUpperCase());
    }
    
    /**
     * Return an int value header.
     * @param key the name of the header
     * @param nullValue return this value if the header is null
     */
    public int getInt(String key, int nullValue){
        String val = (String)map.get(key.toUpperCase());
        if (val != null){
            return Integer.parseInt(val);
        } else {
            return nullValue;
        }
    }
    
    /**
     * Return an associated session object.
     */
    public Object getSession() {
        return session;
    }

    /**
     * Set an associated Session object. This is typically a HttpSession
     * and set by a Servlet after it gets this header instance from the
     * servlet inputstream.
     */
    public void setSession(Object session) {
        this.session = session;
    }

    public String toString() {
        return map.toString();
    }
   
}

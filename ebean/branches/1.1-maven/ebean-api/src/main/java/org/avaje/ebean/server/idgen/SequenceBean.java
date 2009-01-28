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
package org.avaje.ebean.server.idgen;

import org.avaje.ebean.MapBean;

/**
 * Mapped to ebean_sequence table.
 * Used for managing the 
 */
public class SequenceBean extends MapBean {

    private static final long serialVersionUID = -7529857843840562782L;
    
    public void setName(String name){
        set("name", name);
    }
    public String getName() {
        return getString("name");
    }
    
    public Integer getNextId() {
        return getInteger("nextId");
    }
    
    public void setNextId(Integer id){
        set("nextId", id);
    }
    
    public void setStep(Integer step){
        set("step", step);
    }
    
    public Integer getStep(){
        return getInteger("step");
    }
}

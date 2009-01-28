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
package com.avaje.ebean.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of commands.
 * <p>
 * It is likely to be more efficient to put commands into a list and
 * have them processed that way. Reducing the number of network requests.
 * </p>
 */
public class CommandList implements Serializable {

    static final long serialVersionUID = -6056029569865789117L;
    
    ArrayList<Command> list = new ArrayList<Command>();
    
    public CommandList() {
        
    }
    public String toString() {
        return "CommandList "+list;
    }
    /**
     * Return the size of the list.
     */
    public int size() {
        return list.size();
    }
    /**
     * Add a command to the list.
     */
    public int add(Command command){
        int p = list.size();
        list.add(command);
        return p;
    }
    
    /**
     * Get the command from the index position.
     */
    public Command getCommand(int i){
        return (Command)list.get(i);
    }
    
    /**
     * Return the underlying list.
     */
    public List<Command> list() {
        return list;
    }
}

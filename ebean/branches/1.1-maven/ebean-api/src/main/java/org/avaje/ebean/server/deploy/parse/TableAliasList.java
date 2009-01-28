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
package org.avaje.ebean.server.deploy.parse;

import java.util.ArrayList;

/**
 * Used to in SQL generation to determine unique sql table alias.   
 */
public class TableAliasList {

    /**
     * Used to load 26 alias into the list.
     */
    String alphabet = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Used to support more than 26 table joins. Unlimited.
     */
    String prefix = "";

    /**
     * The list of available alias's.
     */
    ArrayList<String> list = new ArrayList<String>();

    /**
     * Create the alias list.
     */
    public TableAliasList() {
        loadAlphabet();
    }
    
    private void loadAlphabet() {
        for (int i = 0; i < alphabet.length(); i++) {
            list.add("" + alphabet.charAt(i));
        }
    }

    /**
     * Remove the alias from the list. Returns true if this succeeded. Returns
     * false if the alias has already been used and you should try to get
     * another one.
     */
    public boolean remove(String alias) {
        return list.remove(alias);
    }
    
    /**
     * Take the next available alias from the list.
     */
    public String removeNext() {
        if (list.size() == 0) {
            // joined 26 tables, prepending x and loading the alphabet again
            prefix += "x";
            loadAlphabet();
        }
        return prefix+(String) list.remove(0);
    }
}

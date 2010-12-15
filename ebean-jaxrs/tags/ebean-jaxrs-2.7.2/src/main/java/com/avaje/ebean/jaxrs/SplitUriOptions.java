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
package com.avaje.ebean.jaxrs;

import java.util.ArrayList;

/**
 * Helper to split UriOptions into segments for parsing.
 * 
 * @author rbygrave
 */
final class SplitUriOptions {

    private final ArrayList<String> list;

    private final String source;
    private final int sourceLength;
    private final char[] chars;
    
    private int pos;
    private int startPos;
    
    public static String[] split(String source){
        return new SplitUriOptions(source).split();
    }
    
    private SplitUriOptions(String source) {
        this.list = new ArrayList<String>(5);
        this.source = source;
        this.sourceLength = source.length();
        this.chars = source.toCharArray();
    }
    
    private String[] split() {

        if (chars[0] == ':'){
            // ignore leading ':'
            ++pos;
        }
        String section;
        while((section = nextSection()) != null) {
            list.add(section);
        }

        return list.toArray(new String[list.size()]);
    }
    
    private String nextSection() {
        
        if (pos >= sourceLength) {
            return null;
        }
        startPos = pos;
        
        // ignore first char as it could be a colon too
        // for the id list section
        if (++pos < sourceLength) {
            // loop until colon or eof
            char c = chars[pos++];
            while (pos < sourceLength) {
                c = chars[pos++];
                if (c == ':'){
                    // split on colon
                    return source.substring(startPos, pos-1);
                }
            }
        }
        return source.substring(startPos, sourceLength);
    }
}

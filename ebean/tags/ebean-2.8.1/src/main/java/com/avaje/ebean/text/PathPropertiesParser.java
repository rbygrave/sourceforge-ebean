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
package com.avaje.ebean.text;

/**
 * Parses Uri segments like :(id,name,shippingAddress(*),contacts(*)) so that
 * the response can be customised for performance.
 * 
 * @author rbygrave
 */
class PathPropertiesParser {

    // :(a,b,c(d,e,f))

    private final PathProperties pathProps;
    
    private final String source;

    private final char[] chars;

    private final int eof;

    private int pos;
    private int startPos;
    
    private PathProperties.Props currentPathProps;

    /**
     * Use {@link PathProperties#parse(String)}.
     */
    static PathProperties parse(String source) {
        return new PathPropertiesParser(source).pathProps;
    }

    private PathPropertiesParser(String src) {

        if (src.startsWith(":")) {
            src = src.substring(1);
        }
        this.pathProps = new PathProperties();
        this.source = src;
        this.chars = src.toCharArray();
        this.eof = chars.length;

        if (eof > 0){
            currentPathProps = pathProps.getRootProperties();
            parse();
        }
    }

    private String getPath() {
        do {
            char c1 = chars[pos++];
            switch (c1) {
            case '(':
                return currentWord();
            default:
            }
        } while (pos < eof);
        throw new RuntimeException("Hit EOF while reading sectionTitle from " + startPos);
    }

    private void parse() {

        do {
            String path = getPath();
            pushPath(path);
            parseSection();

        } while (pos < eof);
    }

    private void parseSection() {
        do {
            char c1 = chars[pos++];
            switch (c1) {
            case '(':
                addSubpath();
                break;
            case ',':
                addCurrentProperty();
                break;
            case ':':
                // start new section
                startPos = pos;
                return;
            case ')':
                // end of section
                addCurrentProperty();
                popSubpath();
                break;
            default:
            }

        } while (pos < eof);
    }

    private void addSubpath() {
        pushPath(currentWord());
    }

    private void addCurrentProperty() {
        String w = currentWord();
        if (w.length() > 0) {
            currentPathProps.addProperty(w);
        }
    }

    private String currentWord() {
        if (startPos == pos) {
            return "";
        }
        String currentWord = source.substring(startPos, pos - 1);
        startPos = pos;
        return currentWord;
    }

    private void pushPath(String title) {

        if (!"".equals(title)) {
            currentPathProps = currentPathProps.addChild(title);            
        }
    }

    private void popSubpath() {
        
        currentPathProps = currentPathProps.getParent();
    }

}

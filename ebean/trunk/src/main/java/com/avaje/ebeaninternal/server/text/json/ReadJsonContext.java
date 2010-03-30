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
package com.avaje.ebeaninternal.server.text.json;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.text.TextException;
import com.avaje.ebean.text.json.JsonElement;
import com.avaje.ebean.text.json.JsonReadBeanVisitor;
import com.avaje.ebean.text.json.JsonReadOptions;
import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebeaninternal.server.util.ArrayStack;

public class ReadJsonContext {
    
    private final ReadJsonSource src;
    
    private final Map<String, JsonReadBeanVisitor<?>> visitorMap;

    private final JsonValueAdapter valueAdapter;
    
    private final ArrayStack<ReadBeanState> beanState;
    private ReadBeanState currentState;

    private char prevChar;
    private char tokenStart;
    private String tokenKey;
    
    public ReadJsonContext(ReadJsonSource src, JsonValueAdapter dfltValueAdapter, JsonReadOptions options) {

        this.src = src;
        this.beanState = new ArrayStack<ReadBeanState>();
        if (options == null){
            this.valueAdapter = dfltValueAdapter;
            this.visitorMap = null;
        } else {
            this.valueAdapter = getValueAdapter(dfltValueAdapter, options.getValueAdapter());
            this.visitorMap = options.getVisitorMap();
        }
    }
    
    private JsonValueAdapter getValueAdapter(JsonValueAdapter dfltValueAdapter, JsonValueAdapter valueAdapter) {
        return valueAdapter == null ? dfltValueAdapter : valueAdapter;
    }

    public JsonValueAdapter getValueAdapter() {
        return valueAdapter;
    }

    public char getToken() {
        return tokenStart;
    }

    public String getTokenKey() {
        return tokenKey;
    }
    
    public boolean isTokenKey() {
        return '\"' == tokenStart;
    }

    public boolean isTokenObjectEnd() {
        return '}' == tokenStart;
    }
        
    public boolean readObjectBegin() {
        readNextToken();
        if ('{' == tokenStart){
            return true;
        } else if ('n' == tokenStart) {
            return false;
        } else if (']' == tokenStart) {
            // an empty array
            return false;
        }
        throw new RuntimeException("Expected object begin at "+src.getErrorHelp());
    }
 
    public boolean readKeyNext() {
        readNextToken();
        if ('\"' == tokenStart){
            return true;
        } else if ('}' == tokenStart) {
            return false;
        }
        throw new RuntimeException("Expected '\"' or '}' at "+src.getErrorHelp());        
    }
    
    public boolean readValueNext() {
        readNextToken();
        if (',' == tokenStart){
            return true;
        } else if ('}' == tokenStart) {
            return false;
        }
        throw new RuntimeException("Expected ',' or '}' at "+src.getErrorHelp()+" but got "+tokenStart);        
    }
    
    public boolean readArrayBegin() {
        readNextToken();
        if ('[' == tokenStart){
            return true;
        } else if ('n' == tokenStart) {
            return false;
        }
        throw new RuntimeException("Expected array begin at "+src.getErrorHelp());
    }
    
    public boolean readArrayNext() {
        readNextToken();
        if (',' == tokenStart){
            return true;
        }
        if (']' == tokenStart){
            return false;
        }
        throw new RuntimeException("Expected ',' or ']' at "+src.getErrorHelp());
    }
    
    public String readScalarValue() {
        
        ignoreWhiteSpace();
        
        prevChar = src.nextChar("EOF reading scalarValue?");
        if ('"' == prevChar){
            return readQuotedValue();
        } else {
            return readUnquotedValue();
        }
    }
   
    
    
    public void readNextToken() {
        
        ignoreWhiteSpace();
        
        tokenStart = src.nextChar("EOF finding next token");
        switch (tokenStart) {
        case '"': 
            internalReadKey();
            break;
        case '{': break;
        case '}': break;
        case '[': break; // not expected
        case ']': break; // not expected
        case ',': break; // not expected
        case ':': break; // not expected
        case 'n': 
            internalReadNull();
            break; // not expected

        default:
            throw new RuntimeException("Unexpected tokenStart["+tokenStart+"] "+src.getErrorHelp());
        }
        
    }
    
    
    protected String readQuotedValue() {
        
        boolean escape = false;
        StringBuilder sb = new StringBuilder();

        do {
            char ch = src.nextChar("EOF reading quoted value");
            if (escape) {
                // in escape mode so just append the character
                sb.append(ch);
                
            } else {
                switch (ch) {
                case '\\':
                    // put into 'escape' mode for next character
                    escape = true;
                    break;
                case '"':
                    return sb.toString();

                default:
                    sb.append(ch);
                }
            }
        } while (true);
    }

    protected String readUnquotedValue() {
        String v = readUnquotedValueRaw();
        if ("null".equals(v)){
            return null;
        } else {
            return v;
        }
    }
    
    private String readUnquotedValueRaw() {

        StringBuilder sb = new StringBuilder();
        sb.append(prevChar);
        
        do {
            tokenStart = src.nextChar("EOF reading unquoted value");
            switch (tokenStart) {
            case ',':
                src.back();
                return sb.toString();
                
            case '}':
                src.back();
                return sb.toString();
                
            case ' ':
                return sb.toString();

            case '\t':
                return sb.toString();

            case '\r':
                return sb.toString();
            
            case '\n':
                return sb.toString();

            default:
                sb.append(tokenStart);
            }
            
        } while (true);
        
    }

    private void internalReadNull() {
        
        StringBuilder sb = new StringBuilder(4);
        sb.append(tokenStart);
        for (int i = 0; i < 3; i++) {
            char c = src.nextChar("EOF reading null ");
            sb.append(c);
        }
        if (!"null".equals(sb.toString())){
            throw new TextException("Expected 'null' but got "+sb.toString()+" "+src.getErrorHelp());
        }
    }
    
    private void internalReadKey() {
        StringBuilder sb = new StringBuilder();
        do {
            char c = src.nextChar("EOF reading key");
            if ('\"' == c){
                tokenKey = sb.toString();
                break;
            } else {
                sb.append(c);
            }
        } while (true);
        
        ignoreWhiteSpace();
        
        char c = src.nextChar("EOF reading ':'");
        if (':' != c){
            throw new TextException("Expected to find colon after key at "+(src.pos()-1)+" but found ["+c+"]"+src.getErrorHelp());
        }
    }
    
    protected void ignoreWhiteSpace() {
        src.ignoreWhiteSpace();
    }
        
    public void pushBean(Object bean){
        currentState = new ReadBeanState(bean);
        beanState.push(currentState);
    }
    
    public void popBean() {
        
        currentState.setLoadedState();
        beanState.pop();
        currentState = beanState.peekWithNull();
    }
    
    public void setProperty(String propertyName){
        currentState.setLoaded(propertyName);
    }
        
    /**
     * Got a key that doesn't map to a known property so read the json value
     * which could be json primitive, object or array.
     * <p>
     * Provide these values to a JsonReadBeanVisitor if registered.
     * </p>
     */
    public void readUnmappedJson(String key) {
        
        ReadJsonRawReader rawReader = new ReadJsonRawReader(this);
        JsonElement rawJsonValue = rawReader.readUnknownValue();
        if (visitorMap != null){
            currentState.addUnmappedJson(key, rawJsonValue);
        }
    }

    protected char nextChar() {
        tokenStart = src.nextChar("EOF getting nextChar for raw json");
        return tokenStart;
    }

    private static class ReadBeanState {
        
        private final Object bean;
        private final EntityBeanIntercept ebi;
        private final Set<String> loadedProps;
        private Map<String,JsonElement> unmapped;
        
        private ReadBeanState(Object bean) {
            this.bean = bean;
            if (bean instanceof EntityBean){
                this.ebi = ((EntityBean)bean)._ebean_getIntercept();
                this.loadedProps = new HashSet<String>();
            } else {
                this.ebi = null;
                this.loadedProps = null;
            }
        }
        public String toString(){
            return bean.getClass().getSimpleName()+" loaded:"+loadedProps;
        }
        
        private void setLoaded(String propertyName){
            if (ebi != null){
                loadedProps.add(propertyName);
            }
        }
        
        private void addUnmappedJson(String key, JsonElement value){
            if (unmapped == null){
                unmapped = new LinkedHashMap<String, JsonElement>();
            }
            unmapped.put(key, value);
        }
        
        private void setLoadedState(){
            if (ebi != null){
                ebi.setLoadedProps(loadedProps);
                //ebi.setLoaded();
            }
        }
    }

}

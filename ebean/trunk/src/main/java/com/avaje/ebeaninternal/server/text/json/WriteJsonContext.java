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

import java.util.Map;
import java.util.Set;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebean.text.json.JsonWriteBeanVisitor;
import com.avaje.ebean.text.json.JsonWriteOptions;
import com.avaje.ebean.text.json.JsonWriter;
import com.avaje.ebeaninternal.server.util.ArrayStack;


public class WriteJsonContext implements JsonWriter {

    private final WriteJsonBuffer buffer;

    private final boolean pretty;

    private final JsonValueAdapter valueAdapter;
    
    private final ArrayStack<Object> parentBeans = new ArrayStack<Object>();
    
    private final Map<String, JsonWriteBeanVisitor<?>> visitorMap;

    private final PathStack pathStack;
    
    private WriteBeanState beanState;
    
    private int depthOffset;
    
    boolean assocOne;

    public WriteJsonContext(WriteJsonBuffer buffer, boolean pretty, JsonValueAdapter dfltValueAdapter, JsonWriteOptions options){
        this.buffer = buffer;
        if (options == null){
            this.pretty = pretty;
            this.valueAdapter = dfltValueAdapter;
            this.visitorMap = null;
            this.pathStack = null;
        } else {
            this.pretty = options.isPretty();
            this.valueAdapter = getValueAdapter(dfltValueAdapter, options.getValueAdapter());
            this.visitorMap = options.getVisitorMap();
            this.pathStack = (visitorMap == null || visitorMap.isEmpty()) ? null : new PathStack();
        }
    }

    private JsonValueAdapter getValueAdapter(JsonValueAdapter dfltValueAdapter, JsonValueAdapter valueAdapter) {
        return valueAdapter == null ? dfltValueAdapter : valueAdapter;
    }
    
    public JsonWriteBeanVisitor<?> getBeanVisitor() {
        if (pathStack != null){
            String path = pathStack.peekWithNull();
            return visitorMap.get(path);
        }
        return null;
    }
    
    public String getJson() {
        return buffer.toString();
    }    

    private void appendIndent(){
        
        buffer.append("\n");
        int depth = depthOffset + parentBeans.size();
        for (int i = 0; i < depth; i++) {
            buffer.append("    ");
        }    
    }
    
    public void appendObjectBegin(){
        if (pretty && !assocOne){
            appendIndent();
        }
        buffer.append("{");
    }
    public void appendObjectEnd(){
        buffer.append("}");
    }
    
    public void appendArrayBegin(){
        if (pretty){
            appendIndent();
        }
        buffer.append("[");
        depthOffset++;
    }
    
    public void appendArrayEnd(){
        depthOffset--;
        if (pretty){
            appendIndent();
        }
        buffer.append("]");
    }

    public void appendComma(){
        buffer.append(",");
    }
    
    public void addDepthOffset(int offset){
        depthOffset += offset;
    }

    public void beginAssocOneIsNull(String key) {
        depthOffset++;
        internalAppendKeyBegin(key);
        appendNull();
        depthOffset--;
    }
    
    public void beginAssocOne(String key) {
        if (pathStack != null){
            pathStack.pushPathKey(key);
        }
        internalAppendKeyBegin(key);
        assocOne = true;
    }
    
    public void endAssocOne() {
        if (pathStack != null){
            pathStack.pop();
        }
        assocOne = false;
    }
    
    public void beginAssocMany(String key) {
        if (pathStack != null){
            pathStack.pushPathKey(key);
        }
        depthOffset--;
        internalAppendKeyBegin(key);
        depthOffset++;
        buffer.append("[");
    }

    public void endAssocMany(){
        if (pathStack != null){
            pathStack.pop();
        }
        if (pretty){
            depthOffset--;
            appendIndent();
            depthOffset++;
        }
        buffer.append("]");
    }
    
    private void internalAppendKeyBegin(String key) {
        if (!beanState.isFirstKey()){
            buffer.append(",");
        }
        if (pretty){
            appendIndent();
        }
        appendKeyWithComma(key, false);
    }

    public void appendKey(String key) {
        appendKeyWithComma(key, true);
    }
    
    private void appendKeyWithComma(String key, boolean withComma) {
        if (withComma){
            if (!beanState.isFirstKey()){
                buffer.append(",");
            }
        }
        buffer.append("\"");
        if(key == null) {
            buffer.append("null");
        } else {
            buffer.append(key);
        }
        buffer.append("\":");
    }

    public void appendKeyValue(String key, String escapedValue) {
        appendKey(key);
        buffer.append(escapedValue);
    }

    public void appendNull(String key) {
        appendKey(key);
        buffer.append("null");
    }

    public void appendNull() {
        buffer.append("null");
    }

    public JsonValueAdapter getValueAdapter() {
        return valueAdapter;
    }

    public String toString() {
        return buffer.toString();
    }
        
    public void popParentBean(){
        parentBeans.pop();
    }

    public void pushParentBean(Object parentBean){
        parentBeans.push(parentBean);
    }

    public void popParentBeanMany(){
        parentBeans.pop();
        depthOffset--;
    }
    
    public void pushParentBeanMany(Object parentBean){
        parentBeans.push(parentBean);
        depthOffset++;
    }

    public boolean isParentBean(Object bean){
        if (parentBeans.isEmpty()){
            return false;
        } else {
            return bean == parentBeans.peek();
        }
    }
    
    public WriteBeanState pushBeanState(Object bean) {
        WriteBeanState newState = new WriteBeanState(bean);
        WriteBeanState prevState = beanState;
        beanState = newState;
        return prevState;
    }
    
    public void pushPreviousState(WriteBeanState previousState) {
        this.beanState = previousState;
    }
    
    public boolean isLoadedBean() {
        return beanState.isLoadedBean();
    }
    
    public boolean includedProp(String name) {
        return beanState.includedProp(name);
    }
    
    public Set<String> getLoadedProps() {
        return beanState.getLoadedProps();
    }
    
    
    public static class WriteBeanState {
        
        private final EntityBeanIntercept ebi;
        private final Set<String> loadedProps;
        private final boolean loadedBean;
        private boolean firstKeyOut;
        
        public WriteBeanState(Object bean) {
            if (bean instanceof EntityBean){
                this.ebi = ((EntityBean)bean)._ebean_getIntercept();
                this.loadedProps = ebi.getLoadedProps();
                this.loadedBean = ebi.isLoaded();
            } else {
                this.ebi = null;
                this.loadedProps = null;
                this.loadedBean = true;
            }
        }
        
        public Set<String> getLoadedProps() {
            return loadedProps;
        }
        
        public boolean includedProp(String name) {
            if (loadedProps == null || loadedProps.contains(name)){
                return true;
            } else {
                return false;
            }
        }
        public boolean isLoadedBean() {
            return loadedBean;
        }
        
        public boolean isFirstKey() {
            if (!firstKeyOut){
                firstKeyOut = true;
                return true;
            } else {
                return false;
            }
        }
        
    }
}

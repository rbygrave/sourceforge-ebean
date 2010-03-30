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

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.avaje.ebean.text.TextException;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonReadOptions;
import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebean.text.json.JsonWriteOptions;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

/**
 * Default implementation of JsonContext.
 * 
 * @author rbygrave
 */
public class DJsonContext implements JsonContext {

    private final SpiEbeanServer server;
    
    private final JsonValueAdapter dfltValueAdapter;
        
    private final boolean dfltPretty;
    
    public DJsonContext(SpiEbeanServer server, JsonValueAdapter dfltValueAdapter, boolean dfltPretty){
        this.server = server;
        this.dfltValueAdapter = dfltValueAdapter;
        this.dfltPretty = dfltPretty;
    }


    private ReadJsonSource createReader(Reader jsonReader) {
        return new ReadJsonSourceReader(jsonReader, 256, 512);
    }
    
    public <T> T toBean(Class<T> cls, String json){
        return toBean(cls, new ReadJsonSourceString(json), null);
    }
    
    public <T> T toBean(Class<T> cls, Reader jsonReader) {
        return toBean(cls, createReader(jsonReader), null);
    }
    
    public <T> T toBean(Class<T> cls, String json, JsonReadOptions options){
        return toBean(cls, new ReadJsonSourceString(json), options);
    }    

    public <T> T toBean(Class<T> cls, Reader jsonReader, JsonReadOptions options) {
        return toBean(cls, createReader(jsonReader), options);
    }

    private <T> T toBean(Class<T> cls, ReadJsonSource src, JsonReadOptions options){

        BeanDescriptor<T> d = getDecriptor(cls);
        ReadJsonContext ctx = new ReadJsonContext(src, dfltValueAdapter, options);
        return d.jsonRead(ctx);
    }

    public <T> List<T> toList(Class<T> cls, String json){
        return toList(cls, new ReadJsonSourceString(json), null);        
    }

    public <T> List<T> toList(Class<T> cls, String json, JsonReadOptions options){
        return toList(cls, new ReadJsonSourceString(json), options);        
    }
    
    public <T> List<T> toList(Class<T> cls, Reader jsonReader){
        return toList(cls, createReader(jsonReader), null);        
    }

    public <T> List<T> toList(Class<T> cls, Reader jsonReader, JsonReadOptions options){
        return toList(cls, createReader(jsonReader), options);        
    }
    
    private <T> List<T> toList(Class<T> cls, ReadJsonSource src, JsonReadOptions options){

        try {
            BeanDescriptor<T> d = getDecriptor(cls);
    
            List<T> list = new ArrayList<T>();
            
            ReadJsonContext ctx = new ReadJsonContext(src, dfltValueAdapter, options);
            ctx.readArrayBegin();
            do {
                T bean = d.jsonRead(ctx);
                if (bean != null){
                    list.add(bean);
                }
                if (!ctx.readArrayNext()){
                    break;
                }
            } while(true);
            
            return list;
        } catch (RuntimeException e){
            throw new TextException("Error parsing "+src, e);
        }
    }
    
    
    
    public void toJsonWriter(Object o, Writer writer) {
        toJsonInternal(o, new WriteJsonBufferWriter(writer), dfltPretty, null);
    }

    public void toJsonWriter(Object o, Writer writer, JsonWriteOptions options) {
        toJsonInternal(o, new WriteJsonBufferWriter(writer), dfltPretty, options);
    }    

    public void toJsonWriterPretty(Object o, Writer writer, boolean pretty){
        toJsonInternal(o, new WriteJsonBufferWriter(writer), pretty, null);
    }
    
    public String toJsonString(Object o){
        WriteJsonBufferString b = new WriteJsonBufferString();
        toJsonInternal(o, b, dfltPretty, null);
        return b.getBufferOutput();
    }

    public String toJsonString(Object o, JsonWriteOptions options){
        WriteJsonBufferString b = new WriteJsonBufferString();
        toJsonInternal(o, b, dfltPretty, options);
        return b.getBufferOutput();
    }

    public String toJsonStringPretty(Object bean, boolean pretty){
        WriteJsonBufferString b = new WriteJsonBufferString();
        toJsonInternal(bean, b, pretty, null);
        return b.getBufferOutput();
    }

    private void toJsonInternal(Object o, WriteJsonBuffer buffer, boolean pretty, JsonWriteOptions options){

        if (o instanceof Collection<?>){
            toJsonFromCollection((Collection<?>)o, buffer, pretty, options);
            
        } else {
            BeanDescriptor<?> d = getDecriptor(o.getClass());
            WriteJsonContext ctx = new WriteJsonContext(buffer, pretty, dfltValueAdapter, options);
            d.jsonWrite(ctx, o);
        }
    }
   

    private <T> String toJsonFromCollection(Collection<T> c, WriteJsonBuffer buffer, boolean pretty, JsonWriteOptions options){
        
        Iterator<T> it = c.iterator();
        if (!it.hasNext()){
            return null;
        }
        
        WriteJsonContext ctx = new WriteJsonContext(buffer, pretty, dfltValueAdapter, options);

        Object o = it.next();
        BeanDescriptor<?> d = getDecriptor(o.getClass());

        ctx.appendArrayBegin();
        d.jsonWrite(ctx, o);
        while (it.hasNext()) {
            ctx.appendComma();
            T t = it.next();        
            d.jsonWrite(ctx, t);
        }
        ctx.appendArrayEnd();       
        return ctx.getJson();
    }
    
    private <T> BeanDescriptor<T> getDecriptor(Class<T> cls) {
        BeanDescriptor<T> d = server.getBeanDescriptor(cls);
        if (d == null){
            String msg = "No BeanDescriptor found for "+cls;
            throw new RuntimeException(msg);
        }
        return d;
    }
}

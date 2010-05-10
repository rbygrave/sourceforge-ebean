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
import java.lang.reflect.Type;
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
import com.avaje.ebeaninternal.util.ParamTypeHelper;
import com.avaje.ebeaninternal.util.ParamTypeHelper.ManyType;
import com.avaje.ebeaninternal.util.ParamTypeHelper.TypeInfo;

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

    public boolean isSupportedType(Type genericType) {
        return server.isSupportedType(genericType);
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
        return d.jsonRead(ctx, null);
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
                T bean = d.jsonRead(ctx, null);
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
    
    
    public Object toObject(Type genericType, String json, JsonReadOptions options) {
        
        TypeInfo info = ParamTypeHelper.getTypeInfo(genericType);
        ManyType manyType = info.getManyType();
        switch (manyType) {
        case NONE:
            return toBean(info.getBeanType(), json, options);            

        case LIST:
            return toList(info.getBeanType(), json, options);
            
        default:
            String msg = "ManyType "+manyType+" not supported yet";
            throw new TextException(msg);
        }
    }
    
    public Object toObject(Type genericType, Reader json, JsonReadOptions options) {
        
        TypeInfo info = ParamTypeHelper.getTypeInfo(genericType);
        ManyType manyType = info.getManyType();
        switch (manyType) {
        case NONE:
            return toBean(info.getBeanType(), json, options);            

        case LIST:
            return toList(info.getBeanType(), json, options);
            
        default:
            String msg = "ManyType "+manyType+" not supported yet";
            throw new TextException(msg);
        }
    }


    public void toJsonWriter(Object o, Writer writer) {
        toJsonWriter(o, writer, dfltPretty, null, null);
    }

    public void toJsonWriter(Object o, Writer writer, boolean pretty) {
        toJsonWriter(o, writer, pretty, null, null);
    }    

    public void toJsonWriter(Object o, Writer writer, boolean pretty, JsonWriteOptions options){
        toJsonWriter(o, writer, pretty, null, null);
    }
    
    public void toJsonWriter(Object o, Writer writer, boolean pretty, JsonWriteOptions options, String callback) {
        toJsonInternal(o, new WriteJsonBufferWriter(writer), pretty, options, callback);
    }

    public String toJsonString(Object o){
        return toJsonString(o, dfltPretty, null);
    }

    public String toJsonString(Object o, boolean pretty){
        return toJsonString(o, pretty, null);
    }

    public String toJsonString(Object o, boolean pretty, JsonWriteOptions options){
        return toJsonString(o, pretty, options, null);
    }
    
    public String toJsonString(Object o, boolean pretty, JsonWriteOptions options, String callback){
        WriteJsonBufferString b = new WriteJsonBufferString();
        toJsonInternal(o, b, pretty, options, callback);
        return b.getBufferOutput();
    }

    private void toJsonInternal(Object o, WriteJsonBuffer buffer, boolean pretty, JsonWriteOptions options, String requestCallback){

        if (o instanceof Collection<?>){
            toJsonFromCollection((Collection<?>)o, buffer, pretty, options, requestCallback);
            
        } else {
            BeanDescriptor<?> d = getDecriptor(o.getClass());
            WriteJsonContext ctx = new WriteJsonContext(buffer, pretty, dfltValueAdapter, options, requestCallback);
            d.jsonWrite(ctx, o);
            ctx.end();
        }
    }
   

    private <T> String toJsonFromCollection(Collection<T> c, WriteJsonBuffer buffer, boolean pretty, 
            JsonWriteOptions options, String requestCallback){
        
        Iterator<T> it = c.iterator();
        if (!it.hasNext()){
            return null;
        }
        
        WriteJsonContext ctx = new WriteJsonContext(buffer, pretty, dfltValueAdapter, options, requestCallback);

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
        ctx.end();
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

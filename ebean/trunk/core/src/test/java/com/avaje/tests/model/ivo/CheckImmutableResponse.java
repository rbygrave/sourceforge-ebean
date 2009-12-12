package com.avaje.tests.model.ivo;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CheckImmutableResponse {

    Constructor<?> constructor;
    
    Method[] readers;
    
//    List<Field> fields = new ArrayList<Field>();
    
    boolean immutable = true;
    
    String error;

    public String toString(){
        if(immutable){
            return "immutable \n   constructor:"+constructor+" \n   readers:"+Arrays.toString(readers);
        } else {
            return "not immutable due to:"+error;
        }
    }
    
    
    
    public Constructor<?> getConstructor() {
        return constructor;
    }



    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    


    public Method[] getReaders() {
        return readers;
    }



    public void setReaders(Method[] readers) {
        this.readers = readers;
    }



    public String getError() {
        return error;
    }



    public void setError(String error) {
        System.err.println("ERROR: "+error);
        this.immutable = false;
        this.error = error;
    }



    public boolean isImmutable() {
        return immutable;
    }



    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }


//
//    public void add(Field field){
//        fields.add(field);
//    }
}

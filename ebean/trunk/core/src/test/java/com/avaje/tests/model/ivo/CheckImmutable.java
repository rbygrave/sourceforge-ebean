package com.avaje.tests.model.ivo;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class CheckImmutable {

    public static CheckImmutableResponse isImmutable(Class<?> cls) {

        CheckImmutableResponse res = new CheckImmutableResponse();
        isImmutable(cls, res);
        
        if (res.isImmutable()){
            RuntimeException error = null;
            
            List<Constructor<?>> conOptions = findConstructors(cls, res);
            for (int i = 0; i < conOptions.size(); i++) {
                try {
                    Method[] getters = findGetters(cls, conOptions.get(i), res);
                    res.setReaders(getters);
                    res.setConstructor(conOptions.get(i));
                    
                } catch (RuntimeException e){
                    error = e;
                }
            }
            if (res.getConstructor() == null) {
                throw new RuntimeException("No Constructor found", error);
            }
            
        }
        
        return res;
    }

    private static Method[] findGetters(Class<?> cls, Constructor<?> con, CheckImmutableResponse res) {

        Method[] methods = cls.getMethods();

        Class<?>[] paramTypes = con.getParameterTypes();
        
        Method[] readers = new Method[paramTypes.length];
        
        for (int i = 0; i < paramTypes.length; i++) {
            Method getMethod = findGetter(paramTypes[i], methods);
            if (getMethod == null) {
                throw new RuntimeException("Get Method not found for "+paramTypes[i]+" in "+cls);
            }
            readers[i] = getMethod;
        }
        
        return readers;
    }
    
    private static Method findGetter(Class<?> paramType, Method[] methods) {
        for (int i = 0; i < methods.length; i++) {
            if (Modifier.isStatic(methods[i].getModifiers())) {
                
            } else {
                if (methods[i].getParameterTypes().length == 0) {
                    // could be a getter
                    String methName = methods[i].getName();
                    if (methName.equals("hashCode")){
                        
                    } else {
                        Class<?> returnType = methods[i].getReturnType();
                        if (paramType.equals(returnType)){
                            //if (methName.startsWith("get") || methName.startsWith("is") || methName.startsWith("has")){
                                return methods[i];
                            //}
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static List<Constructor<?>> findConstructors(Class<?> cls, CheckImmutableResponse res) {
        
        int maxLength = 0;
        List<Constructor<?>> chosen = new ArrayList<Constructor<?>>();
        
        // find the constructor with the most number of parameters
        Constructor<?>[] constructors = cls.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Class<?>[] parameterTypes = constructors[i].getParameterTypes();
            if (parameterTypes.length > maxLength){
                maxLength = parameterTypes.length;
            }
        }
        
        for (int i = 0; i < constructors.length; i++) {
            Class<?>[] parameterTypes = constructors[i].getParameterTypes();
            if (parameterTypes.length == maxLength){
                chosen.add(constructors[i]);
            }
        }
        
        return chosen;
    }
    
    public static boolean isImmutable(Class<?> cls, CheckImmutableResponse res) {

        
        if (isKnownImmutable(cls)) {
            return true;
        }
        Class<?>[] noParams = new Class<?>[0];
        try {
            cls.getDeclaredConstructor(noParams);
            res.setError("Not allowed a default constructor");
            return false;
        } catch (SecurityException e) {
            // this is ok
        } catch (NoSuchMethodException e) {
            // this is expected for our IVO's
        }

        // check super class
        Class<?> superClass = cls.getSuperclass();

        if (!isImmutable(superClass, res)) {
            res.setError("Super not Immutable " + superClass);
            return false;
        }

        // Check all fields defined in the class for type and if they are final
        Field[] objFields = cls.getDeclaredFields();
        for (int i = 0; i < objFields.length; i++) {
            if (Modifier.isStatic(objFields[i].getModifiers())) {
                // ignore static fields
            } else {
                //System.out.println("------------- field " + cls + "." + objFields[i].getName());
                if (!Modifier.isFinal(objFields[i].getModifiers())) {
                    res.setError("Non final field " + cls + "." + objFields[i].getName());
                    return false;
                }
                if (!isImmutable(objFields[i].getType(), res)) {
                    res.setError("Non Immutable field type " + objFields[i].getType());
                    return false;
                }
                //res.add(objFields[i]);
            }
        }

        // Lets hope we didn't forget something
        return true;
    }

    private static boolean isKnownImmutable(Class<?> cls) {
        // Check for all allowed property types...
        if (cls.isPrimitive() || String.class.equals(cls) || Object.class.equals(cls)) {
            return true;
        }
        if (java.util.Date.class.equals(cls) || java.sql.Date.class.equals(cls) || java.sql.Timestamp.class.equals(cls)) {
            // treat as immutable even through they are not strictly so
            return true;
        }
        if (java.math.BigDecimal.class.equals(cls) || java.math.BigInteger.class.equals(cls)) {
            // treat as immutable (contain non-final fields)
            return true;
        }

        if (Integer.class.equals(cls) || Long.class.equals(cls) || Double.class.equals(cls) || Float.class.equals(cls)
                || Short.class.equals(cls) || Byte.class.equals(cls) || Character.class.equals(cls)
                || Boolean.class.equals(cls)) {
            return true;
        }

        return false;
    }

}

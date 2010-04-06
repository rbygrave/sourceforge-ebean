package com.avaje.ebeaninternal.server.deploy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.Query;

public class DetermineQueryType {

	public static Query.Type getQueryType(Class<?> type) {
        if (type.equals(List.class)){
        	return Query.Type.LIST;
        }
        if (type.equals(Set.class)){
        	return Query.Type.SET;
        } 
        if (type.equals(Map.class)){
        	return Query.Type.MAP;
        }
        return null;
    }
}

package com.avaje.ebeaninternal.server.deploy;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Determine the Many Type for a property.
 * <p>
 * Scala types require CollectionTypeConverter's to wrap and unwrap from the
 * underlying java types used.
 * </p>
 */
public class DetermineManyType {

    private static final CollectionTypeConverter BUF_CONVERTER = new ScalaBufferConverter();
    private static final CollectionTypeConverter SET_CONVERTER = new ScalaSetConverter();
    private static final CollectionTypeConverter MAP_CONVERTER = new ScalaMapConverter();
    
    private static final ManyType SCALA_BUF_MANY = new ManyType(ManyType.Underlying.LIST, BUF_CONVERTER);
    private static final ManyType SCALA_SET_MANY = new ManyType(ManyType.Underlying.SET, SET_CONVERTER);
    private static final ManyType SCALA_MAP_MANY = new ManyType(ManyType.Underlying.MAP, MAP_CONVERTER);
    
	public static ManyType getManyType(Class<?> type) {
        if (type.equals(List.class)){
        	return ManyType.JAVA_LIST;
        }
        if (type.equals(Set.class)){
        	return ManyType.JAVA_SET;
        } 
        if (type.equals(Map.class)){
        	return ManyType.JAVA_MAP;
        }
        if (type.equals(scala.collection.mutable.Buffer.class)){
            return SCALA_BUF_MANY;
        }
        if (type.equals(scala.collection.mutable.Set.class)){
            return SCALA_SET_MANY;
        }
        if (type.equals(scala.collection.mutable.Map.class)){
            return SCALA_MAP_MANY;
        }
        return null;
    }
}

package com.avaje.ebean.server.deploy;

import com.avaje.ebean.Query;
import com.avaje.ebean.server.core.OrmQueryRequest;


/**
 * Creates Helpers specific to the type of the property (List Set or Map).
 */
public class BeanCollectionHelpFactory {

	/**
	 * Create the helper based on the many property.
	 */
	public static <T> BeanCollectionHelp<T> create(BeanPropertyAssocMany<T> manyProperty) {

		Query.Type manyType = manyProperty.getManyType();
		switch (manyType) {
		case LIST:
			return new BeanListHelp<T>(manyProperty);
		case SET:
			return new BeanSetHelp<T>(manyProperty);
		case MAP:
			return new BeanMapHelp<T>(manyProperty);
		default:
			throw new RuntimeException("Invalid type "+manyType);
		}
		
	}
		
	public static <T> BeanCollectionHelp<T> create(OrmQueryRequest<T> request) {

		Query.Type manyType = request.getQuery().getType();
		
		if (manyType.equals(Query.Type.LIST)){
			return new BeanListHelp<T>();
		
		} else if (manyType.equals(Query.Type.SET)) {
			return new BeanSetHelp<T>();
		
		} else {
			BeanDescriptor<T> target = request.getBeanDescriptor();
			String mapKey = request.getQuery().getMapKey();
			return new BeanMapHelp<T>(target, mapKey);
		}
	}

	
}

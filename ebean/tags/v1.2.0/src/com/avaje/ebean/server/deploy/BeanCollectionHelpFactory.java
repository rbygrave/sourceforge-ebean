package com.avaje.ebean.server.deploy;

import com.avaje.ebean.server.core.OrmQueryRequest;


/**
 * Creates Helpers specific to the type of the property (List Set or Map).
 */
public class BeanCollectionHelpFactory {

	/**
	 * Create the helper based on the many property.
	 */
	public static <T> BeanCollectionHelp<T> create(BeanPropertyAssocMany<T> manyProperty) {

		ManyType manyType = manyProperty.getManyType();
		
		if (manyType.equals(ManyType.LIST)){
			return new BeanListHelp<T>(manyProperty);
		
		} else if (manyType.equals(ManyType.SET)) {
			return new BeanSetHelp<T>(manyProperty);
		
		} else {
			return new BeanMapHelp<T>(manyProperty);
		}
	}
		
	public static <T> BeanCollectionHelp<T> create(OrmQueryRequest<T> request) {

		ManyType manyType = request.getManyType();
		
		if (manyType.equals(ManyType.LIST)){
			return new BeanListHelp<T>();
		
		} else if (manyType.equals(ManyType.SET)) {
			return new BeanSetHelp<T>();
		
		} else {
			BeanDescriptor<T> target = request.getBeanDescriptor();
			String mapKey = request.getQuery().getMapKey();
			return new BeanMapHelp<T>(target, mapKey);
		}
	}

	
}

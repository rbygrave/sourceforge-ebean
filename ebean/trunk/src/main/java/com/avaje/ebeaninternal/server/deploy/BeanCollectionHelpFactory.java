package com.avaje.ebeaninternal.server.deploy;

import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;


/**
 * Creates Helpers specific to the type of the property (List Set or Map).
 */
public class BeanCollectionHelpFactory {

	/**
	 * Create the helper based on the many property.
	 */
	public static <T> BeanCollectionHelp<T> create(BeanPropertyAssocMany<T> manyProperty) {

		ManyType manyType = manyProperty.getManyType();
		switch (manyType.getUnderlying()) {
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

		SpiQuery.Type manyType = request.getQuery().getType();
		
		if (manyType.equals(SpiQuery.Type.LIST)){
			return new BeanListHelp<T>();
		
		} else if (manyType.equals(SpiQuery.Type.SET)) {
			return new BeanSetHelp<T>();
		
		} else {
			BeanDescriptor<T> target = request.getBeanDescriptor();
			String mapKey = request.getQuery().getMapKey();
			return new BeanMapHelp<T>(target, mapKey);
		}
	}

	
}

package org.avaje.ebean.server.deploy;

/**
 * Creates Helpers specific to the type of the property (List Set or Map).
 */
public class BeanCollectionHelpFactory {

	private static final BeanCollectionHelp LIST = new BeanListHelp();

	private static final BeanCollectionHelp SET = new BeanSetHelp();

	/**
	 * Create the helper based on the many property.
	 */
	public static BeanCollectionHelp create(BeanPropertyAssocMany manyProperty) {
		return create(manyProperty.getManyType(), manyProperty.getTargetDescriptor());
	}
		
	/**
	 * Create the helper based on the many type.
	 */
	public static BeanCollectionHelp create(ManyType manyType, BeanDescriptor target) {

		if (manyType.equals(ManyType.LIST)){
			return LIST;
		
		} else if (manyType.equals(ManyType.SET)) {
			return SET;
		
		} else {
			return new BeanMapHelp(target);
		}
	}
}

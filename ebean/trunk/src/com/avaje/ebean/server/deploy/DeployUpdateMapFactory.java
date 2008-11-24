package com.avaje.ebean.server.deploy;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.deploy.id.ImportedId;

/**
 * Build a map of logical to physical names for use in Orm Updates.
 *
 */
public class DeployUpdateMapFactory {

	private static final Logger logger = Logger.getLogger(DeployUpdateMapFactory.class.getName());
	
	/**
	 * Build a map of logical to physical names for use in Orm Updates for a given descriptor.
	 * <p>
	 * This includes the dbWrite scalar properties and imported foreign key properties.
	 * </p>
	 */
	public static Map<String,String> build(BeanDescriptor descriptor) {
		
		Map<String,String> deployMap = new HashMap<String,String>();
		
		String shortName = descriptor.getName();
		if (shortName == null){
			System.out.println("asdasd");
		}
		String beanName = shortName.toLowerCase();
		deployMap.put(beanName, descriptor.getBaseTable());
		
		BeanProperty[] baseScalar = descriptor.propertiesBaseScalar();
		for (BeanProperty baseProp : baseScalar) {
			// excluding formula, secondary table properties
			if (baseProp.isDbWrite()){
				deployMap.put(baseProp.getName().toLowerCase(), baseProp.getDbColumn());
			}
		}
		
		BeanPropertyAssocOne[] oneImported = descriptor.propertiesOneImported();
		for (BeanPropertyAssocOne assocOne : oneImported) {
			
			ImportedId importedId = assocOne.getImportedId();
			if (importedId == null){
				String m = descriptor.getFullName()+" importedId is null for associated: "+assocOne.getFullBeanName();
				logger.log(Level.SEVERE, m);
				
			} else if (importedId.isScalar()){
				deployMap.put(importedId.getLogicalName(), importedId.getDbColumn());
			}
		}
		
		return deployMap;
	}		
	
	
}

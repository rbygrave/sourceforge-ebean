package com.avaje.ebean.server.deploy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.server.deploy.jointree.PropertyDeploy;

/**
 * Converts logical property names to database columns with table alias.
 * <p>
 * In doing so it builds an 'includes' set which becomes the joins required to
 * support the properties parsed.
 * </p>
 */
public final class DeployPropertyParser extends DeployParser {

	
	private final Map<String, PropertyDeploy> propMap;

	private final Set<String> includes = new HashSet<String>();


	public DeployPropertyParser(final Map<String, PropertyDeploy> propMap) {
		this.propMap = propMap;
	}

	public Set<String> getIncludes() {
		return includes;
	}

	public String convertWord() {

		PropertyDeploy propertyDeploy = propMap.get(word);

		if (propertyDeploy == null) {
			return word;

		}
		String include = propertyDeploy.getInclude();

		// add includes for this property
		addIncludes(include, include.length());

		return propertyDeploy.getDeploy();
	}

	/**
	 * Add 'includes'.
	 * <p>
	 * These are used to determine the 'joins' required to support the
	 * predicates and order by clauses.
	 * </p>
	 */
	private void addIncludes(String logicalWord, int fromIndex) {
		int lastPeriod = logicalWord.lastIndexOf('.', fromIndex);
		if (lastPeriod > -1) {
			String inc = logicalWord.substring(0, lastPeriod);
			// testing all lower case...
			// for the includes
			includes.add(inc.toLowerCase());
			addIncludes(logicalWord, lastPeriod - 1);
		}
	}

	

}

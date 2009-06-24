package com.avaje.ebean.server.deploy;

import java.util.HashSet;
import java.util.Set;

import com.avaje.ebean.el.ElPropertyDeploy;

/**
 * Converts logical property names to database columns with table alias.
 * <p>
 * In doing so it builds an 'includes' set which becomes the joins required to
 * support the properties parsed.
 * </p>
 */
public final class DeployPropertyParser extends DeployParser {

	
	private final BeanDescriptor<?> beanDescriptor;
	
	private final Set<String> includes = new HashSet<String>();

	public DeployPropertyParser(BeanDescriptor<?> beanDescriptor) {
		this.beanDescriptor = beanDescriptor;
	}

	public Set<String> getIncludes() {
		return includes;
	}

	public String convertWord() {

		
		ElPropertyDeploy elGetValue = beanDescriptor.getElPropertyDeploy(word);
		if (elGetValue == null){
			return word;
		} else {
			String prefix = elGetValue.getPrefix();
			if (prefix != null){
				addIncludes(prefix);
				return "${"+prefix+"}"+elGetValue.getDbColumn();
			} else {
				return "${}"+elGetValue.getDbColumn();
			}
		}
	}

	private void addIncludes(String prefix) {
		includes.add(prefix);
	}
}

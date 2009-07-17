package com.avaje.ebean.server.deploy;

import java.util.HashSet;
import java.util.Set;

import com.avaje.ebean.server.el.ElPropertyDeploy;

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
		
		ElPropertyDeploy elProp = beanDescriptor.getElPropertyDeploy(word);
		if (elProp == null){
			return word;
		} else {
			addIncludes(elProp.getElPrefix());
			return elProp.getElPlaceholder();
		}
	}

	private void addIncludes(String prefix) {
		if (prefix != null){
			includes.add(prefix);
		}
	}
}

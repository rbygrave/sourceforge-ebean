package com.avaje.ebean.el;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility object used to build a ElGetChain.
 * <p>
 * Builds a ElGetChain based on a chain of properties with dot separators.
 * </p>
 */
public class ElGetChainBuilder {

	final String expression;
	
	final List<ElGetValue> chain = new ArrayList<ElGetValue>();

	/**
	 * Create with the original expression.
	 */
	public ElGetChainBuilder(String expression) {
		this.expression = expression;
	}

	/**
	 * Add a ElGetValue element to the chain. 
	 */
	public ElGetChainBuilder add(ElGetValue element){
		chain.add(element);
		return this;
	}

	/**
	 * Build the immutable ElGetChain from the build information.
	 */
	public ElGetChain build() {
		return new ElGetChain(expression, chain.toArray(new ElGetValue[chain.size()]));
	}
	
}

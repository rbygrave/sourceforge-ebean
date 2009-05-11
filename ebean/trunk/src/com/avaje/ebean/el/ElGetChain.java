package com.avaje.ebean.el;

/**
 * A ElGetValue based on a chain of properties.
 * <p>
 * Used to get the value for an compound expression like customer.name or
 * customer.shippingAddress.city etc.
 * </p>
 * <p>
 * Note that if any element in the chain returns null, then null is returned and
 * no further processing of the chain occurs.
 * </p>
 */
public class ElGetChain implements ElGetValue {

	final String expression;

	final ElGetValue[] chain;

	public ElGetChain(String expression, ElGetValue[] chain) {
		this.expression = expression;
		this.chain = chain;
	}

	public Object elGetValue(Object bean) {

		for (int i = 0; i < chain.length; i++) {
			bean = chain[i].elGetValue(bean);
			if (bean == null) {
				return bean;
			}
		}

		return bean;
	}

}

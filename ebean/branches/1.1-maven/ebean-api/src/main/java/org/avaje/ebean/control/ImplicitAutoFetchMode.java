package org.avaje.ebean.control;

import org.avaje.ebean.Query;

/**
 * The mode for determining if autoFetch will be used for a given query when
 * {@link Query#setAutoFetch(boolean)} has not been explicitly set on a query.
 * <p>
 * The explicit control of {@link Query#setAutoFetch(boolean)} will always take
 * precedence. This mode is used when this has not been explicitly set on a
 * query.
 * </p>
 */
public enum ImplicitAutoFetchMode {

	/**
	 * Don't implicitly use autoFetch. Must explicitly turn it on.
	 */
	DEFAULT_OFF,

	/**
	 * Use autoFetch implicitly. Must explicitly turn it off.
	 */
	DEFAULT_ON,

	/**
	 * Implicitly use autoFetch if the query has not got either select() or
	 * join() defined.
	 */
	DEFAULT_ON_IF_EMPTY

}

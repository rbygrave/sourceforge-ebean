/**
 * JAX-RS Ebean integration module.
 * <p>
 * Generally you need to:
 * </p>
 * <ul>
 * <li>Register com.avaje.ebean.jaxrs with JAX-RS</li>
 * <li>Create JAX-RS Resources that extend AbstractEntityResource</li>
 * </ul>
 * <p>
 * The com.avaje.ebean.jaxrs package needs to be registered with
 * JAX-RS so that it finds Ebean's JaxrsJsonProvider. This provides
 * the JSON Marshalling and Unmarshalling of entity beans.
 * </p>
 * <p>
 * If you use custom Media types then you might need to subclass
 * the JaxrsJsonProvider and register it with those media types.
 * </p>
 */
package com.avaje.ebean.jaxrs;
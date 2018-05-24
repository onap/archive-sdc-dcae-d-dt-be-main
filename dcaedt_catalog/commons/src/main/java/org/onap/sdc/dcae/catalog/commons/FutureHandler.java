package org.onap.sdc.dcae.catalog.commons;

/**
 * Modeled after the vertx future
 */
@FunctionalInterface
public interface FutureHandler<T> {

	public void handle(Future<T> theResult);
	
}

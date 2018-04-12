package org.onap.sdc.dcae.catalog.commons;

import org.onap.sdc.dcae.catalog.commons.Future;

/**
 * Modeled after the vertx future
 */
@FunctionalInterface
public interface FutureHandler<T> {

	public void handle(Future<T> theResult);
	
}

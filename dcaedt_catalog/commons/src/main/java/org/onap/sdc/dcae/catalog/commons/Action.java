package org.onap.sdc.dcae.catalog.commons;

import org.onap.sdc.dcae.catalog.commons.Future;

/**
 */
public interface Action<T> {

	public Future<T> execute();

}

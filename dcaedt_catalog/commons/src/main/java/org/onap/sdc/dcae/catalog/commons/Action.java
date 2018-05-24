package org.onap.sdc.dcae.catalog.commons;

/**
 */
public interface Action<T> {

	public Future<T> execute();

}

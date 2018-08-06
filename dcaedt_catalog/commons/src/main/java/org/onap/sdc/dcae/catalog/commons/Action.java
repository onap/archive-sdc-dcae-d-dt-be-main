package org.onap.sdc.dcae.catalog.commons;

/**
 */
public interface Action<T> {

	Future<T> execute();
}

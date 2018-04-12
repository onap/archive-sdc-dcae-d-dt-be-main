package org.onap.sdc.dcae.catalog.engine;

import java.net.URI;

import org.onap.sdc.dcae.catalog.engine.CatalogMessage;

public class CatalogRequest extends CatalogMessage {

	private URI		 	catalog;
	private long		timeout = 0;

	public void setCatalog(URI theCatalogUri) {
		this.catalog = theCatalogUri;
	}

	public URI getCatalog() {
		return this.catalog;
	}

	public void setTimeout(long theTimeout) {
		this.timeout = theTimeout;
	}

	public long getTimeout() {
		return this.timeout;
	}
}

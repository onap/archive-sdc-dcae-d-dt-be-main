package org.onap.sdc.dcae.catalog.engine;

/**
 */
public class CatalogError extends CatalogResponse {

	public CatalogError(CatalogRequest theRequest, String theMessage) {
		super(theRequest);
		error().put("message", theMessage);
	}

	public CatalogError(CatalogRequest theRequest, String theMessage, Throwable theError) {
		super(theRequest);
		error().put("message", theMessage)
					 .put("exception", theError.toString());
	}
}

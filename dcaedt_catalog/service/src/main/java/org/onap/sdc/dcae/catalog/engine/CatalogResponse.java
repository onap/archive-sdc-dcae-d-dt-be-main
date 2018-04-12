package org.onap.sdc.dcae.catalog.engine;


import com.fasterxml.jackson.annotation.JsonRawValue;

import org.json.JSONObject;
import org.onap.sdc.dcae.catalog.engine.CatalogMessage;
import org.onap.sdc.dcae.catalog.engine.CatalogRequest;

/**
 */
public class CatalogResponse extends CatalogMessage {

	private JSONObject data = new JSONObject(),
										 error = new JSONObject();

	public CatalogResponse(CatalogRequest theRequest) {
		setId(theRequest.getId());
		setTimestamp(theRequest.getTimestamp());
	}

	public JSONObject data() {
		return this.data;
	}

	@JsonRawValue
	public String getData() {
		return this.data.toString();
	}
	
	public JSONObject error() {
		return this.error;
	}

	@JsonRawValue
	public String getError() {
		return this.error.toString();
	}
}

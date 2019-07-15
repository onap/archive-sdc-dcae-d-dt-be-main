/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

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

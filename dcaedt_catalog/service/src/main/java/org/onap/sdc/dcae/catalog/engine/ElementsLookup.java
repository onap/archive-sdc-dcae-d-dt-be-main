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

import java.util.Map;
import java.util.Collections;

import org.json.JSONObject;
import org.onap.sdc.dcae.catalog.engine.CatalogRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** 
 */
public class ElementsLookup extends CatalogRequest {

	private String 						annotation;
	private Map<String,Object> selector;

	public void setAnnotation(String theAnnon) {
		this.annotation = theAnnon;
	}

	public String getAnnotation() {
		return this.annotation;
	}

	public Map<String,Object> getSelector() {
		return this.selector == null ? Collections.EMPTY_MAP : this.selector;
	}

	public void setSelector(Map<String,Object> theSelector) {
		this.selector = theSelector;
	}

	public Object getSelectorEntry(String theName) {
		return getSelector().get(theName);
	}

	/**
   * Because the JSONObject(Map) constructor would not copy entries wth null values.
   */
	@JsonIgnore
	public JSONObject getJSONSelector() {
		JSONObject jsonSelector = new JSONObject();
		for (Map.Entry<String, Object> entry: this.selector.entrySet()) {
			jsonSelector.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : JSONObject.NULL);
		}
		return jsonSelector;
	}
}

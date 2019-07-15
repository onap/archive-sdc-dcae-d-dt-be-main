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

package org.onap.sdc.dcae.catalog.commons;

import java.util.Map;
import java.lang.reflect.ParameterizedType;

import org.json.JSONObject;

import org.onap.sdc.dcae.catalog.commons.ProxyBuilder;


public class Proxies {

	private Proxies() {
	}


	private static ProxyBuilder builder = new ProxyBuilder();

	public static <T> T build(Map theData, Class<T> theType) {
		return builder.build(new JSONObject(theData), theType);
	}

	public static <T> T build(JSONObject theData, Class<T> theType) {
		return builder.build(theData, theType);
	}

	public static <T> Class<T> typeArgument(Class theType) {
		return (Class<T>)	
						((ParameterizedType)theType.getGenericSuperclass()).
							getActualTypeArguments()[0];
	}
  
}

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

import java.util.function.Function;
import java.util.function.BiFunction;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

import org.json.JSONObject;

import org.onap.sdc.dcae.catalog.commons.Proxy;
import org.onap.sdc.dcae.catalog.commons.ProxyBuilder;


public class ProxyBuilder {

	private Map<String, ?> context;
	private Map<String, BiFunction<Proxy, Object[], Object>> extensions; 

	public ProxyBuilder() {
	}

	public <T> T build(Map theData, Class<T> theType) {
		return build(new JSONObject(theData), theType);
	}

	public <T> T build(JSONObject theData, Class<T> theType) {
		return (T)java.lang.reflect.Proxy.newProxyInstance(
				ProxyBuilder.class.getClassLoader(),
				new Class[] { theType },
				new Proxy(theData, this));
	}

	public ProxyBuilder withConverter(final Function<Object, ?> theConverter, Class theType) {
		ConvertUtils.register(new Converter() {
														public Object convert(Class theToType, Object theValue) {
															return theConverter.apply(theValue);
														}
													},
													theType);
		return this;
	}
	
	/*
  plug in an extension to the proxy default behaviour.
	*/
	public ProxyBuilder withExtensions(Map<String, BiFunction<Proxy, Object[], Object>> theExtensions) {
		this.extensions = theExtensions;
		return this;
	}

	public ProxyBuilder withContext(Map<String, ?> theContext) {
		this.context = theContext;
		return this;
	}

	protected Object context(String theName) {
		return this.context == null ? null : this.context.get(theName);
	}

	protected BiFunction<Proxy, Object[], Object> extension(String theName) {
		return this.extensions == null ? null : this.extensions.get(theName);
	}

	protected boolean hasExtension(String theName) {
		return this.extensions == null ? false : this.extensions.containsKey(theName);
	}
}

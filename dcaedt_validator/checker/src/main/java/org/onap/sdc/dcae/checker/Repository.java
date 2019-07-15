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

package org.onap.sdc.dcae.checker;


import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.Map;

/**
 * Represents a 'container' of (yaml) TOSCA documents
 */
public abstract class Repository {

	protected OnapLoggerError errLogger = OnapLoggerError.getInstance();
	protected OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	private String 				name,
												description;
	protected URI					 	rootURI;
	protected Map 					credential;	//TOSCA type tosca.datatype.Credential

	public Repository(String theName, URI theRoot) {
		this.name = theName;
		this.rootURI = theRoot;
	}

	public String getName() {
		return this.name;
	}

	public URI getRoot() {
		return this.rootURI;
	}

	/** optional */
	public abstract Iterable<Target> targets();

	/** */
	public abstract Target resolve(URI theURI);

	@Override
	public String toString() {
		return "Repository " + this.name + " at " + this.rootURI;
	}
}


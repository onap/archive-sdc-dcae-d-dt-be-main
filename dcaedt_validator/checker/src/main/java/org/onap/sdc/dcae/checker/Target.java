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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

/**
 * Represents a yaml document to be parsed/validated/checked
 */
public class Target {

	private String 				name;			//maintained mainly for logging
	private URI					 	location;
	private Object 				target;		//this is the parsed form of the target

	private	Report	report = new Report(); //collects the errors related to this target

	public Target(String theName, URI theLocation) {
		this.name = theName;
		this.location = theLocation;
	}

	public String getName() {
		return this.name;
	}

	public URI getLocation() {
		return this.location;
	}

	public Report getReport() {
		return this.report;
	}

	public void report(Throwable theError) {
		this.report.add(theError); 
	}
	
	public void report(String theErrMsg) {
		this.report.add(new Exception(theErrMsg)); 
	}

	public void setTarget(Object theTarget) {
		this.target = theTarget;
	}

	public Object getTarget() {
		return this.target;
	}

	/*
	 * @return a reader for the source or null if failed
	 */
	public Reader open() throws IOException {

    return new BufferedReader(
						new InputStreamReader(
          		this.location.toURL().openStream()));
	}

	public String toString() {
		return String.format("Target %s at %s", this.name, this.location);

	}
}


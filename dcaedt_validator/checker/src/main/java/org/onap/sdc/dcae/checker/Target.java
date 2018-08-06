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


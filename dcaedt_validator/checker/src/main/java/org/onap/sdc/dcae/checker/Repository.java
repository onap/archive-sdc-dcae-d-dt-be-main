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


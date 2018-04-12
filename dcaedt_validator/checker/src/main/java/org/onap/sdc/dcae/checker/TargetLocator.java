package org.onap.sdc.dcae.checker;

import java.net.URI;


public interface TargetLocator {
	
	/** */
	public boolean addSearchPath(URI theURI); 

	/** */
	public boolean addSearchPath(String thePath);

	/** */
	public Iterable<URI> searchPaths();
	
	/** */
	public Target resolve(String theName);

}

package org.onap.sdc.dcae.checker;

import java.util.Set;


/**
 * Exposes target properties. How they are obtained/calculated not of importance here.
 */
public interface TargetInfo {
	
	/** */
	public Set<String>	entryNames();

	/** */
	public boolean	hasEntry(String theName);

	/** */
	public Object	getEntry(String theName);

}

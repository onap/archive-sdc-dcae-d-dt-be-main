package org.onap.sdc.dcae.checker;


/**
 * A target error represents an error in target the resource being checked.
 * We only represent it as a Throwable because the libraries that perform parsing and syntax validation
 * represent their errors as such ..
 */
public class TargetError extends Throwable {

	/*
	public static enum Level {
		error,
		warning	
	}
	*/

  private String		location; //we might need an more detailed representation
															//here: it could be a YAML document jpath or
															//document location (line).
  private String		target;

  public TargetError(String theTarget, String theLocation, String theMessage, Throwable theCause) {
		super(theMessage, theCause);
    this.target = theTarget;
		this.location = theLocation;
  }

  public TargetError(String theTarget, String theLocation, String theMessage) {
    this(theTarget, theLocation, theMessage, null);
  }

	public String getTarget() {
		return this.target;
	}

	public String getLocation() {
		return this.location;
	}


}


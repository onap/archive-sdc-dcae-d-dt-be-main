package org.onap.sdc.dcae.checker;


/**
 */
public class ProcessorException extends CheckerException {

	private Target	target;

  public ProcessorException(Target theTarget, String theMsg, Throwable theCause) {
    super(theMsg, theCause);
		this.target = theTarget;
  }

  public ProcessorException(Target theTarget, String theMsg) {
    super(theMsg);
		this.target = theTarget;
  }

	public Target getTarget() {
		return this.target;
	}

	@Override
	public String getMessage() {
		return this.target + ":" + super.getMessage() + (getCause() == null ? "" : ("(" + getCause() + ")"));
	}
}

package org.onap.sdc.dcae.checker;


/**
 * A checker exception represents an error that stops the checker from
 * completing its task.
 */
public class CheckerException extends Exception {

  public CheckerException(String theMsg, Throwable theCause) {
    super(theMsg, theCause);
  }

  public CheckerException(String theMsg) {
    super(theMsg);
  }

}

package org.onap.sdc.dcae.checker;


public class CatalogException extends Exception {

  public CatalogException(String theMsg, Throwable theCause) {
    super(theMsg, theCause);
  }

  public CatalogException(String theMsg) {
    super(theMsg);
  }

}

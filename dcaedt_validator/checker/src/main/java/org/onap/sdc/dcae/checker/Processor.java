package org.onap.sdc.dcae.checker;


/**
 * Just in case you might want to do something with a template (set) once it was checked
 */
public interface Processor<T extends Processor<T>> {

  /* */
	public ProcessBuilder<T> process(Catalog theCatalog);
}

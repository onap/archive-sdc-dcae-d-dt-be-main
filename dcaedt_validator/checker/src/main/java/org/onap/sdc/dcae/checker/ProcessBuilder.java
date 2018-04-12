package org.onap.sdc.dcae.checker;


/**
 * Just in case you might want to do something with a template (set) once it was checked
 */
public interface ProcessBuilder<T extends Processor> {

	/* */
	public ProcessBuilder<T> with(String theName, Object theValue);
		
	/* */
	public ProcessBuilder<T> withOpt(String theName, Object theValue);
  
	/* */
	public Process<T> process();

	/* */
	default public Report run() {
		return process()
						.run();
	}

}

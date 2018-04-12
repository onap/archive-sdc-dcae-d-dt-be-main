package org.onap.sdc.dcae.checker;

/**
 * 
 */
public interface Process<T extends Processor> {

	public static final int	PROCESS_SCOPE = 100;

	/**
   * the processor running this process
   */
	public T processor();

  /* */
	public boolean hasNext();

  /* */
	public Process runNext() throws ProcessorException;

	/* execute all steps to completion
   */
	public Report run();

	/* execution report
   */
	public Report report(); 

}

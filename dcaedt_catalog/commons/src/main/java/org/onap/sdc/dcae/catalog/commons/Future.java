package org.onap.sdc.dcae.catalog.commons;

import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.FutureHandler;

/**
 * Modeled after the vertx future
 */
public interface Future<T> {

	public T result();
	
	public Future<T> result(T theResult);

//rename 'cause' to 'failure'
	
	public Throwable cause();
		
	public Future<T> cause(Throwable theError);
	
	public boolean succeeded();

	public boolean failed();

	public boolean complete();

	public T waitForResult() throws Exception;
	
	//public T waitForResult(long theTimeout) throws Exception;

	public Future<T> waitForCompletion() throws InterruptedException;
	
 	public Future<T> setHandler(FutureHandler<T> theHandler);

}

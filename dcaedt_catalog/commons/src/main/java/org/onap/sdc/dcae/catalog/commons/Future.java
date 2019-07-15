/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.sdc.dcae.catalog.commons;

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

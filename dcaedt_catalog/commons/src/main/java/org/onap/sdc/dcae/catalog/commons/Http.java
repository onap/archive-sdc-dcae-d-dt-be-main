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

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class Http {

	protected Http() {
	}
	
	
	public static <T> Future<T> exchange(String theUri, HttpMethod theMethod, HttpEntity theRequest, Class<T> theResponseType) {
	
		AsyncRestTemplate restTemplate = new AsyncRestTemplate();

		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		converters.add(0, new JSONHttpMessageConverter());
		restTemplate.setMessageConverters(converters);

		HttpFuture<T> result = new HttpFuture<T>();
		try {
			restTemplate
				.exchange(theUri, theMethod, theRequest, theResponseType)
					.addCallback(result.callback);
		}
		catch (RestClientException rcx) {
			return Futures.failedFuture(rcx);
		}
		catch (Exception x) {
			return Futures.failedFuture(x);
		}
	 
		return result;
	}
	
	/**
	 * 
	 * @param theUri
	 * @param theMethod
	 * @param theRequest
	 * @param theResponseType
	 * @param readTimeOut pass -1 if you dont need to customize the read time out interval
	 * @return
	 */
	public static <T> ResponseEntity<T> exchangeSync(String theUri, HttpMethod theMethod, HttpEntity theRequest, Class<T> theResponseType, int readTimeOut) {
		
		RestTemplate restTemplate = new RestTemplate();
		
		if(readTimeOut!=-1){
			SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
			rf.setReadTimeout(1 * readTimeOut);
		}

		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		converters.add(0, new JSONHttpMessageConverter());
		restTemplate.setMessageConverters(converters);
		ResponseEntity<T> result = null;

		try {
			result = restTemplate.exchange(theUri, theMethod, theRequest, theResponseType);
		}
		catch (RestClientException rcx) {
			return new ResponseEntity<T>((T) rcx.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch (Exception x) {
			return new ResponseEntity<T>((T) x.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	 
		return result;
	}



	public static class HttpFuture<T> extends Futures.BasicFuture<T> {

		HttpFuture() {
		}

		ListenableFutureCallback<ResponseEntity<T>> callback = new ListenableFutureCallback<ResponseEntity<T>>() {

			public void	onSuccess(ResponseEntity<T> theResult) {
				HttpFuture.this.result(theResult.getBody());
			}

			public void	onFailure(Throwable theError) {
				if (theError instanceof HttpClientErrorException) {
					HttpFuture.this.cause(new Exception((HttpClientErrorException)theError));
				}
				else {
					HttpFuture.this.cause(theError);
				}
			}
		};

	}
}

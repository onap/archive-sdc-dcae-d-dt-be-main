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

package org.onap.sdc.dcae.utils;

import com.google.gson.Gson;
import org.onap.sdc.dcae.catalog.asdc.ASDCException;
import org.onap.sdc.dcae.errormng.RequestError;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class SDCResponseErrorHandler implements ResponseErrorHandler {

	private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

	private static Gson gson = new Gson();

	public void handleError(ClientHttpResponse response) throws IOException {
		try{
			errorHandler.handleError(response);
		} catch (HttpClientErrorException e) {
			RequestError re = extractRequestError(e);
			throw null == re ? e : new ASDCException(e.getStatusCode(), re);
		}
	}

	public boolean hasError(ClientHttpResponse response) throws IOException{
		return errorHandler.hasError(response);
	}

	private RequestError extractRequestError(HttpClientErrorException error) {
		try {
			String body = error.getResponseBodyAsString();
			ResponseFormat responseFormat = gson.fromJson(body, ResponseFormat.class);
			return responseFormat.getRequestError();
		} catch (Exception e) {
			return null;
		}
	}

}

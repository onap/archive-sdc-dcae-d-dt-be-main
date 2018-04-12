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

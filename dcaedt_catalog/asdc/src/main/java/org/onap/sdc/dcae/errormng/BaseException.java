package org.onap.sdc.dcae.errormng;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class BaseException extends HttpClientErrorException {

	private static Gson gson = new Gson();

	protected RequestError requestError;

	public RequestError getRequestError() {
		return requestError;
	}

	public void setRequestError(RequestError requestError) {
		this.requestError = requestError;
	}

	public BaseException(HttpClientErrorException theError) {
		super(theError.getStatusCode());
		String body = theError.getResponseBodyAsString();
		if (body != null) {
			requestError = extractRequestError(body);
		}
	}

	public BaseException(HttpStatus status, RequestError re){
		super(status);
		requestError = re;
	}

	private RequestError extractRequestError(String error) {
		ResponseFormat responseFormat = gson.fromJson(error, ResponseFormat.class);
		return responseFormat.getRequestError();
	}

	@JsonIgnore
	public String getMessageId() {
		return requestError.getMessageId();
	}

	@JsonIgnore
	public String[] getVariables() {
		return requestError.getVariables();
	}

	@JsonIgnore
	public String getText(){
		return requestError.getText();
	}

	@Override
	@JsonIgnore
	public String getMessage() {
		return requestError.getFormattedMessage();
	}

}
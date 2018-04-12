package org.onap.sdc.dcae.errormng;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseFormat {

	@JsonIgnore
	private int status;
	private RequestError requestError;
	private String notes = "";

	public String getNotes() {
		return notes;
	}

	void setNotes(String notes) {
		this.notes = notes;
	}

	public ResponseFormat() {
		super();
	}

	public ResponseFormat(int status) {
		super();
		this.status = status;
	}


	public void setStatus(int status) {
		this.status = status;
	}

	public Integer getStatus() {
		return status;
	}

	public RequestError getRequestError() {
		return requestError;
	}

	public void setRequestError(RequestError requestError) {
		this.requestError = requestError;
	}

	void setPolicyException(PolicyException policyException) {
		this.requestError = new RequestError();
		requestError.setPolicyException(policyException);
	}

	void setServiceException(ServiceException serviceException) {
		this.requestError = new RequestError();
		requestError.setServiceException(serviceException);
	}

	void setOkResponseInfo(OkResponseInfo okResponseInfo) {
		this.requestError = new RequestError();
		requestError.setOkResponseInfo(okResponseInfo);
	}

	void setServiceExceptions(List<ServiceException> serviceExceptions) {
		this.requestError = new RequestError();
		requestError.setServiceExceptions(serviceExceptions);
	}

	@Override
	public String toString() {
		return "ResponseFormat[" + "status=" + status + ", requestError=" + requestError + ']';
	}

}

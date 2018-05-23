package org.onap.sdc.dcae.errormng;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestError {
	private PolicyException policyException;
	private ServiceException serviceException;
	private OkResponseInfo okResponseInfo;

	public ServiceException getServiceException() {
		return serviceException;
	}

	public void setPolicyException(PolicyException policyException) {
		this.policyException = policyException;
	}

	public void setServiceException(ServiceException serviceException) {
		this.serviceException = serviceException;
	}

	void setOkResponseInfo(OkResponseInfo okResponseInfo) {
		this.okResponseInfo = okResponseInfo;
	}

	void setServiceExceptions(List<ServiceException> serviceExceptions) {
		// no one asks for these exception ever
	}
	
	String getFormattedMessage() {
		return getError().getFormattedErrorMessage();
	}

	String getMessageId() {
		return getError().getMessageId();
	}

	String[] getVariables() {
		return getError().getVariables();
	}

	String getText() {
		return getError().getText();
	}

	AbstractSdncException getError() {
		if (null != policyException) {
			return (null != serviceException) ? serviceException : policyException;
		}
		else {
			return (null != serviceException) ? serviceException : okResponseInfo;
		}
	}
}
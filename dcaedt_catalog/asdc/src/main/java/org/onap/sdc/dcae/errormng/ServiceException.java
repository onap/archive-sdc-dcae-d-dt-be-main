package org.onap.sdc.dcae.errormng;

public class ServiceException extends AbstractSdncException {

	public ServiceException(String messageId, String text, String[] variables) {
		super(messageId, text, variables);
	}

	public ServiceException() {
	}

}
package org.onap.sdc.dcae.errormng;

public class PolicyException extends AbstractSdncException {

	public PolicyException(String messageId, String text, String[] variables) {
		super(messageId, text, variables);
	}
}

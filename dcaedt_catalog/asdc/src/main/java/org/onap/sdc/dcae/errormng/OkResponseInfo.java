package org.onap.sdc.dcae.errormng;

public class OkResponseInfo extends AbstractSdncException {

	OkResponseInfo(String messageId, String text, String[] variables) {
		super(messageId, text, variables);
	}
}

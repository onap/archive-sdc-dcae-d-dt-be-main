package org.onap.sdc.dcae.errormng;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class DcaeException extends BaseException {

//	public DcaeException(HttpClientErrorException theError) {
//		super(theError);
//	}

	public DcaeException(HttpStatus status, RequestError re){
		super(status, re);
	}
}

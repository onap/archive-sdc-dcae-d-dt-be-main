package org.onap.sdc.dcae.catalog.asdc;

import org.onap.sdc.dcae.errormng.BaseException;
import org.onap.sdc.dcae.errormng.RequestError;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class ASDCException extends BaseException {

	ASDCException(HttpClientErrorException error) {
		super(error);
	}

	public ASDCException(HttpStatus status, RequestError re){
		super(status, re);
	}

}

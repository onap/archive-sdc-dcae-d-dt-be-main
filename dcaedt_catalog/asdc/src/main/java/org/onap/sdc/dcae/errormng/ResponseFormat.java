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

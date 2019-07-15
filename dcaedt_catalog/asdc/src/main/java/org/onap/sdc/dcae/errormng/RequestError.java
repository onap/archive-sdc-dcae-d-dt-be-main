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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestError {
	private PolicyException policyException;
	private ServiceException serviceException;
	private OkResponseInfo okResponseInfo;
	private List<ServiceException> serviceExceptions;

	public PolicyException getPolicyException() {
		return policyException;
	}

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

	public List<ServiceException> getServiceExceptions() {
		return serviceExceptions;
	}
	void setServiceExceptions(List<ServiceException> serviceExceptions) {
		this.serviceExceptions = serviceExceptions;
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
		if (null != serviceException) {
			return serviceException;
		}
		return (null != policyException) ? policyException : okResponseInfo;
	}
}

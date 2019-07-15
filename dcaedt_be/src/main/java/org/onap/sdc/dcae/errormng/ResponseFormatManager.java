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

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.errormng.ErrorInfo.ErrorInfoType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseFormatManager {

	private volatile static ResponseFormatManager instance;
	private static ErrorConfiguration errorConfiguration;
	private static Map<String, ActionStatus> msgIdToActionStatusMap = new HashMap<>();
	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();


	public static ResponseFormatManager getInstance() {
		if (instance == null) {
			instance = init();
		}
		return instance;
	}

	private static synchronized ResponseFormatManager init() {
		if (instance == null) {
			instance = new ResponseFormatManager();
			errorConfiguration = ErrorConfigurationLoader.getErrorConfigurationLoader().getErrorConfiguration();
			convertToActionMap();
		}
		return instance;
	}

	ResponseFormat getResponseFormat(ActionStatus actionStatus, String notes, String... variables) {
		ErrorInfo errorInfo = errorConfiguration.getErrorInfo(actionStatus.name());
		if (errorInfo == null) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "failed to locate {} in error configuration", actionStatus.name());
			errorInfo = errorConfiguration.getErrorInfo(ActionStatus.GENERAL_ERROR.name());
		}
		
		ResponseFormat responseFormat = new ResponseFormat(errorInfo.getCode());
		String errorMessage = errorInfo.getMessage();
		String errorMessageId = errorInfo.getMessageId();
		ErrorInfoType errorInfoType = errorInfo.getErrorInfoType();
		responseFormat.setNotes(notes);
		
		if (errorInfoType==ErrorInfoType.SERVICE_EXCEPTION) {
			responseFormat.setServiceException(new ServiceException(errorMessageId, errorMessage, variables));
		} 
		else if (errorInfoType==ErrorInfoType.POLICY_EXCEPTION) {
			responseFormat.setPolicyException(new PolicyException(errorMessageId, errorMessage, variables));
		} 
		else if (errorInfoType==ErrorInfoType.OK) {
			responseFormat.setOkResponseInfo(new OkResponseInfo(errorMessageId, errorMessage, variables));
		}
		return responseFormat;
	}

	ResponseFormat getResponseFormat(BaseException baseException) {

		ResponseFormat responseFormat = new ResponseFormat(baseException.getRawStatusCode());
		AbstractSdncException e = baseException.getRequestError().getError();

		if (e instanceof ServiceException) {
			responseFormat.setServiceException((ServiceException)e);
		}
		else if (e instanceof PolicyException) {
			responseFormat.setPolicyException((PolicyException)e);
		}
		else  {
			responseFormat.setOkResponseInfo((OkResponseInfo)e);
		}
		return responseFormat;
	}

	ResponseFormat getResponseFormat(List<ServiceException> errors) {
		ResponseFormat responseFormat = new ResponseFormat(400);
		responseFormat.setServiceExceptions(errors);
		return responseFormat;
	}

	public Map<String, ActionStatus> getMsgIdToActionStatusMap() {
		return msgIdToActionStatusMap;
	}

	private static void convertToActionMap() {
		Map<String, ErrorInfo> errors = errorConfiguration.getErrors();

		if(errors!=null){
			errors.forEach((k, v) -> {
				debugLogger.log(LogLevel.DEBUG, ResponseFormatManager.class.getName(), "{}, {}", v.getMessageId(), k);
				msgIdToActionStatusMap.put(v.getMessageId(), ActionStatus.valueOf(k));
			});
		}
	}

	private ResponseFormatManager(){

	}
}

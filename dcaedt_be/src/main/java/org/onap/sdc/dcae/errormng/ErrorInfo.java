/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

public class ErrorInfo {

	private Integer code;
	private String message;
	private String messageId;
	private ErrorInfoType errorInfoType;

	private static final String SVC_PREFIX = "SVC";
	private static final String POL_PREFIX = "POL";

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();


	public ErrorInfo() {
		this.errorInfoType = ErrorInfoType.OK;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		// Determining the type of error
		if (messageId == null || "200".equals(messageId) || "201".equals(messageId) || "204".equals(messageId)) {
			this.errorInfoType = ErrorInfoType.OK;
		} else if (messageId.startsWith(SVC_PREFIX)) {
			this.errorInfoType = ErrorInfoType.SERVICE_EXCEPTION;
		} else if (messageId.startsWith(POL_PREFIX)) {
			this.errorInfoType = ErrorInfoType.POLICY_EXCEPTION;
		} else {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Error: unexpected error message ID {}, should start with {} or {}", messageId, SVC_PREFIX, POL_PREFIX);
		}
		this.messageId = messageId;
	}

	public ErrorInfoType getErrorInfoType() {
		return this.errorInfoType;
	}

	public void cloneData(ErrorInfo other) {
		this.code = other.getCode();
		this.message = other.getMessage();
		this.messageId = other.getMessageId();
		this.errorInfoType = other.errorInfoType;
	}

	@Override
	public String toString() {
		return "ErrorInfo [code=" + code + ", messageId=" + messageId + ", message=" + message + "]";
	}

	public enum ErrorInfoType {
		OK, POLICY_EXCEPTION, SERVICE_EXCEPTION
	}

}
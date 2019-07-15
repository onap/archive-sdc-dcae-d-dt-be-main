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

import java.util.Map;

/**
 * Example:
 * VES_SCHEMA_INVALID: {
        code: 500,
        message: "Error – Failed to parse VES Schema file '%1'. [%2]",
        messageId: "SVC6007"
    }
    
    key will be "VES_SCHEMA_INVALID"
    value is the json object containing code, message, messageId
 */

import org.onap.sdc.dcae.errormng.BasicConfiguration;

public class ErrorConfiguration extends BasicConfiguration {

	private Map<String, ErrorInfo> errors;

	public Map<String, ErrorInfo> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, ErrorInfo> errors) {
		this.errors = errors;
	}

	public ErrorInfo getErrorInfo(String key) {
		ErrorInfo clone = null;
		ErrorInfo other = errors.get(key);
		if (other != null) {
			clone = new ErrorInfo();
			clone.cloneData(other);
		}
		return clone;
	}

	@Override
	public String toString() {
		return "ErrorConfiguration [errors=" + errors + "]";
	}

}

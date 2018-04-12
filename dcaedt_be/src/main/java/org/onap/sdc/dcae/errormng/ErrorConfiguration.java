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

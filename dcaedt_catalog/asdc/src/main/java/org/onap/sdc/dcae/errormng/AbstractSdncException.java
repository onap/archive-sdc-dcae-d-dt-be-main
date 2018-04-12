package org.onap.sdc.dcae.errormng;


import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;

import java.util.Arrays;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbstractSdncException {
	private String messageId;

	private String text;

	private String[] variables;

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	private final static Pattern ERROR_PARAM_PATTERN = Pattern.compile("%\\d");

	public AbstractSdncException() {
	}

	public AbstractSdncException(String messageId, String text, String[] variables) {
		super();
		this.messageId = messageId;
		this.text = text;
		this.variables = validateParameters(messageId, text, variables);
	}

	private String[] validateParameters(String messageId, String text, String[] variables) {
		String[] res = null;
		Matcher m = ERROR_PARAM_PATTERN.matcher(text);
		int expectedParamsNum = 0;
		while (m.find()) {
			expectedParamsNum += 1;
		}
		int actualParamsNum = (variables != null) ? variables.length : 0;
		if (actualParamsNum < expectedParamsNum) {
			errLogger.log(LogLevel.WARN, this.getClass().getName(),
					"Received less parameters than expected for error with messageId {}, expected: {}, actual: {}. Missing parameters are padded with null values.",
					messageId, expectedParamsNum, actualParamsNum);
		} else if (actualParamsNum > expectedParamsNum) {
			errLogger.log(LogLevel.WARN, this.getClass().getName(),
					"Received more parameters than expected for error with messageId {}, expected: {}, actual: {}. Extra parameters are ignored.",
					messageId, expectedParamsNum, actualParamsNum);
		}
		if (variables != null) {
			res = Arrays.copyOf(variables, expectedParamsNum);
		}

		return res;
	}

	public String getMessageId() {
		return this.messageId;
	}

	public String getText() {
		return text;
	}

	public String[] getVariables() {
		return variables;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setVariables(String[] variables) {
		this.variables = variables;
	}

	public String getFormattedErrorMessage() {
		String res;
		if (variables != null && variables.length > 0) {
			Formatter formatter = new Formatter();
			try {
				res = formatter.format(this.text.replaceAll("%\\d", "%s"), (Object[]) this.variables).toString();
			} finally {
				formatter.close();
			}
		} else {
			res = this.text;
		}
		return res;
	}
}

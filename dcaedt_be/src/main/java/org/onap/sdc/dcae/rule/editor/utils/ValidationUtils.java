package org.onap.sdc.dcae.rule.editor.utils;

import org.apache.commons.lang3.StringUtils;


public class ValidationUtils {

	private static final String EXPLICIT_EMPTY = "\"\"";

	public static boolean validateNotEmpty(String value){
		return StringUtils.isNoneBlank(value);
	}

	public static boolean validateTargetField(String value) {
		return validateNotEmpty(value) && !EXPLICIT_EMPTY.equals(value);
	}



}

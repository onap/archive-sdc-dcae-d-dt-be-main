package org.onap.sdc.dcae.rule.editor.enums;

import java.util.Arrays;

public enum ConditionTypeEnum {
	ALL("And"), ANY("Or");

	public String getFilterClass() {
		return filterClass;
	}

	private String filterClass;

	ConditionTypeEnum(String filterClass) {

		this.filterClass = filterClass;
	}

	public static ConditionTypeEnum getTypeByName(String name) {
		return Arrays.stream(ConditionTypeEnum.values()).filter(type -> name.equalsIgnoreCase(type.name())).findAny().orElse(null);
	}
}

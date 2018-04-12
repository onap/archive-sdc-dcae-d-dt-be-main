package org.onap.sdc.dcae.rule.editor.enums;

import java.util.Arrays;

public enum OperatorTypeEnum {
	EQUALS("Equals", "OneOf"),
	NOT_EQUAL("NotEqual", "NotOneOf"),
	CONTAINS("Contains", null),
	ENDS_WITH("EndsWith", null),
	STARTS_WITH("StartsWith", null);

	private String type;
	private String modifiedType;

	OperatorTypeEnum(String type, String modifiedType) {
		this.type = type;
		this.modifiedType = modifiedType;
	}

	public String getType() {
		return type;
	}

	public String getModifiedType() {
		return modifiedType;
	}

	public static OperatorTypeEnum getTypeByName(String name) {
		return Arrays.stream(OperatorTypeEnum.values()).filter(type -> name.replaceAll(" ", "").equalsIgnoreCase(type.getType())).findAny().orElse(null);
	}

}

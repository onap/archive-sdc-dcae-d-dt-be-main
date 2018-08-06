package org.onap.sdc.dcae.rule.editor.enums;

import java.util.Arrays;

public enum OperatorTypeEnum {

	EQUALS("Equals"), NOT_EQUAL("NotEqual"), CONTAINS("Contains"), ENDS_WITH("EndsWith"), STARTS_WITH("StartsWith"), ONE_OF("OneOf"), NOT_ONE_OF("NotOneOf"), ASSIGNED("Assigned"), UNASSIGNED("Unassigned");

	private String type;

	OperatorTypeEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public OperatorTypeEnum getModifiedType() {
		switch (this) {
		case EQUALS:
		case ONE_OF:
			return ONE_OF;
		case NOT_EQUAL:
		case NOT_ONE_OF:
			return NOT_ONE_OF;
		default:
			return null;
		}
	}

	public String getConditionType() {
		switch (this) {
		case CONTAINS:
		case STARTS_WITH:
		case ENDS_WITH:
		    return "Condition"; // comparing strings
		default:
			return "FieldCondition"; // comparing any type
		}
	}

	public static OperatorTypeEnum getTypeByName(String name) {
		return Arrays.stream(OperatorTypeEnum.values()).filter(type -> name.replaceAll(" ", "").equalsIgnoreCase(type.getType())).findAny().orElse(null);
	}

}

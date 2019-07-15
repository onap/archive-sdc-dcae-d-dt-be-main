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

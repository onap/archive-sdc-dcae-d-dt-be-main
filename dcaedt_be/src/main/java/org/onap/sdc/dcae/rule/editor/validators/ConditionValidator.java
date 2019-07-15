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

package org.onap.sdc.dcae.rule.editor.validators;

import org.apache.commons.lang.StringUtils;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class ConditionValidator extends BaseConditionValidator<Condition> {

	private static ConditionValidator conditionValidator = new ConditionValidator();

	public static ConditionValidator getInstance() {
		return conditionValidator;
	}

	private ConditionValidator(){}

	@Override
	public boolean validate(Condition condition, List<ResponseFormat> errors) {
		return validateConditionalAction(condition, errors) && super.validate(condition, errors);
	}


	public boolean validateConditionalAction(Condition condition, List<ResponseFormat> errors) {
		boolean valid = true;
		if(!ValidationUtils.validateNotEmpty(condition.getLeft())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_OPERAND, null, "left"));
		}
		OperatorTypeEnum operatorTypeEnum = StringUtils.isNotEmpty(condition.getOperator()) ? OperatorTypeEnum.getTypeByName(condition.getOperator()) : null;
		if(null == operatorTypeEnum) {
			valid = false;
			String operatorValue = StringUtils.isNotEmpty(condition.getOperator()) ? condition.getOperator() : "empty";
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_OPERATOR, null, operatorValue));
		}
		if(OperatorTypeEnum.ASSIGNED != operatorTypeEnum && OperatorTypeEnum.UNASSIGNED != operatorTypeEnum && (condition.getRight().isEmpty() || !condition.getRight().stream().allMatch(ValidationUtils::validateNotEmpty))) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_OPERAND, null, "right"));
		}
		return valid;
	}

}

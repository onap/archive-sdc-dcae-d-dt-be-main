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

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.enums.RuleEditorElementType;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class RuleValidator implements IRuleElementValidator<Rule> {

	private static RuleValidator ruleValidator = new RuleValidator();

	public static RuleValidator getInstance() {
		return ruleValidator;
	}

	private RuleValidator(){}

	public boolean validate(Rule rule, List<ResponseFormat> errors) {
		boolean valid = !rule.isConditionalRule() || getConditionValidator(rule.getCondition()).validate(rule.getCondition(), errors);
		// 1810 US427299 phase grouping - support user defined phase names
		if(ValidationUtils.validateNotEmpty(rule.getGroupId()) && !ValidationUtils.validateNotEmpty(rule.getPhase())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_RULE_FORMAT, "", "please define group name"));
		}
		if(!ValidationUtils.validateNotEmpty(rule.getDescription())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_RULE_DESCRIPTION, null, null));
		}
		if(CollectionUtils.isEmpty(rule.getActions())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION, null, null));
		} else {
			valid = rule.getActions().stream()
					.map(a -> getActionValidator(a).validate(a, errors))
					.reduce(true, (x,y) -> x && y) && valid;
		}
		return valid;
	}


	private IRuleElementValidator getActionValidator(BaseAction action) {
		ActionTypeEnum type = ActionTypeEnum.getTypeByName(action.getActionType());
		return RuleEditorElementType.getElementTypeByName(type.getType()).getValidator();
	}

	private IRuleElementValidator getConditionValidator(BaseCondition condition) {
		return RuleEditorElementType.getElementTypeByName(condition.getClass().getSimpleName()).getValidator();
	}
}

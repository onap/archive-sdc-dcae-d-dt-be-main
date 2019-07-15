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

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.TopoSearchAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class TopoSearchValidator extends BaseActionValidator<TopoSearchAction> {

	private static TopoSearchValidator topoSearchValidator = new TopoSearchValidator();

	public static TopoSearchValidator getInstance() {
		return topoSearchValidator;
	}

	private TopoSearchValidator() {
	}

	private ConditionValidator conditionValidator = ConditionValidator.getInstance();

	@Override
	public boolean validate(TopoSearchAction action, List<ResponseFormat> errors) {

		boolean valid = super.validate(action, errors);
		if (action.conditionalSearch() && searchFilterHasNoneEmptyFields(action.searchFilter()) && !conditionValidator.validateConditionalAction(action.searchFilter(), errors)) {
			valid = false;
		}
		if (!ValidationUtils.validateNotEmpty(action.searchField())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "search field", action.getActionType(), action.strippedTarget()));
		}
		if (!ValidationUtils.validateNotEmpty(action.searchValue())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "search value", action.getActionType(), action.strippedTarget()));
		}
		return validateEnrichOrUpdates(action, errors) && valid;
	}

	private boolean searchFilterHasNoneEmptyFields(Condition searchFilter) {
		return ValidationUtils.validateNotEmpty(searchFilter.getLeft()) || ValidationUtils.validateNotEmpty(searchFilter.getOperator()) || !CollectionUtils.isEmpty(searchFilter.getRight()) && searchFilter.getRight().stream().anyMatch(ValidationUtils::validateNotEmpty);
	}

	private boolean validateEnrichOrUpdates(TopoSearchAction action, List<ResponseFormat> errors) {
		if (!action.doEnrich()) {
			return validateUpdatesMap(action, errors);
		}
		if (action.enrichFields().isEmpty() || !action.enrichFields().stream().allMatch(ValidationUtils::validateNotEmpty)) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "enrich fields", action.getActionType(), action.strippedTarget()));
			return false;
		}
		return true;
	}

	private boolean validateUpdatesMap(TopoSearchAction action, List<ResponseFormat> errors) {
		boolean valid = true;
		try {
			if (!action.updatesMap().entrySet().stream().allMatch(p -> ValidationUtils.validateNotEmpty(p.getKey()) && ValidationUtils.validateNotEmpty(p.getValue()))) {
				valid = false;
				errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "key/value", action.getActionType(), action.strippedTarget()));
			}
		} catch (IllegalStateException err) {
			valid = false;
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "updates validation error: {}", err);
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.DUPLICATE_KEY, null));
		}
		return valid;
	}
}

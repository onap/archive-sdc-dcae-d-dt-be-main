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

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.DateFormatterAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class DateFormatterValidator extends CopyActionValidator<DateFormatterAction> {
	private static DateFormatterValidator dateFormatterValidator = new DateFormatterValidator();

	public static DateFormatterValidator getInstance() {
		return dateFormatterValidator;
	}

	private DateFormatterValidator(){}

	@Override
	public boolean validate(DateFormatterAction action, List<ResponseFormat> errors) {
		boolean valid = super.validate(action, errors);
		if(!ValidationUtils.validateNotEmpty(action.fromFormat())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from format", action.getActionType(), action.getTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.fromTz())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "from timezone", action.getActionType(), action.getTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.toFormat())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "to format", action.getActionType(), action.getTarget()));
		}
		if(!ValidationUtils.validateNotEmpty(action.toTz())){
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_ACTION_FIELD, null, "to timezone", action.getActionType(), action.getTarget()));
		}
		return valid;
	}
}

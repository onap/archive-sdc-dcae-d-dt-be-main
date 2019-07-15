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

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCopyAction;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.List;

public class ConcatActionValidator extends CopyActionValidator<BaseCopyAction> {

	private static ConcatActionValidator concatActionValidator = new ConcatActionValidator();

	public static ConcatActionValidator getInstance() {
		return concatActionValidator;
	}

	private ConcatActionValidator(){}

	@Override
	protected boolean validateFromValue(BaseCopyAction action, List<ResponseFormat> errors) {
		if(!ValidationUtils.validateNotEmpty(action.fromValue()) || 2 > action.fromValues().size()) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.MISSING_CONCAT_VALUE, null, action.getTarget()));
			return false;
		}
		return true;
	}
}

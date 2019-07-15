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

package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ActionTypeEnum;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.UnaryFieldAction;

import java.util.List;

public class ClearActionTranslator extends ActionTranslator<UnaryFieldAction> {

	private static ClearActionTranslator clearActionTranslator = new ClearActionTranslator();

	public static ClearActionTranslator getInstance() {
		return clearActionTranslator;
	}

	private ClearActionTranslator(){}

	public Object translateToHpJson(UnaryFieldAction action) {
		return ActionTypeEnum.CLEAR == ActionTypeEnum.getTypeByName(action.getActionType()) ? new ClearActionTranslation(action) : new ClearNSFActionTranslation(action);
	}


	private class ClearActionTranslation extends ProcessorTranslation {
		private List<String> fields;

		ClearActionTranslation(UnaryFieldAction action) {
			clazz = "Clear";
			fields = action.fromValues();
		}
	}


	private class ClearNSFActionTranslation extends ProcessorTranslation {
		private List<String> reservedFields;

		ClearNSFActionTranslation(UnaryFieldAction action) {
			clazz = "ClearNoneStandardFields";
			reservedFields = action.fromValues();
		}
	}
}

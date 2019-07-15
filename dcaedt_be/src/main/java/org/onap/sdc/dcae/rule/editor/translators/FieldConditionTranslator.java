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

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Condition;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;

import java.util.List;

public class FieldConditionTranslator implements IRuleElementTranslator<Condition> {

	private static FieldConditionTranslator fieldConditionTranslator = new FieldConditionTranslator();

	public static FieldConditionTranslator getInstance() {
		return fieldConditionTranslator;
	}

	private FieldConditionTranslator(){}

	private class FieldFilterTranslation extends ProcessorTranslation {
		private String field;
		private String value;

		private FieldFilterTranslation(Condition condition, OperatorTypeEnum operatorType) {
			clazz = operatorType.getType();
			field = condition.getLeft();
			value = condition.getRight().get(0);
		}
	}

	private class UnaryFilterTranslation extends ProcessorTranslation {
		private String field;
		private boolean emptyIsAssigned;

		private UnaryFilterTranslation(Condition condition, OperatorTypeEnum operatorType) {
			clazz = operatorType.getType();
			field = condition.getLeft();
			emptyIsAssigned = condition.isEmptyIsAssigned();
		}
	}

	private class MultiFieldFilterTranslation extends ProcessorTranslation {
		private String field;
		private List<String> values;

		private MultiFieldFilterTranslation(Condition condition, OperatorTypeEnum operatorType) {
			field = condition.getLeft();
			values = condition.getRight();
			clazz = operatorType.getModifiedType().getType();
		}
	}

	public Object translateToHpJson(Condition condition) {
		OperatorTypeEnum operatorType = OperatorTypeEnum.getTypeByName(condition.getOperator());
		if(OperatorTypeEnum.UNASSIGNED == operatorType || OperatorTypeEnum.ASSIGNED == operatorType) {
			return new UnaryFilterTranslation(condition, operatorType);
		}
		return 1 == condition.getRight().size() && !alwaysUseMultipleRightValues(operatorType)? new FieldFilterTranslation(condition, operatorType) : new MultiFieldFilterTranslation(condition, operatorType);
	}

	private boolean alwaysUseMultipleRightValues(OperatorTypeEnum operatorType) {
		return operatorType.equals(operatorType.getModifiedType());
	}

}

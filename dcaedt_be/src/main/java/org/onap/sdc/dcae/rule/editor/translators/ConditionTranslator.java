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
import org.onap.sdc.dcae.rule.editor.enums.ConditionTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;

import java.util.stream.Collectors;

public class ConditionTranslator implements IRuleElementTranslator<Condition> {

	private static ConditionTranslator conditionTranslator = new ConditionTranslator();

	public static ConditionTranslator getInstance() {
		return conditionTranslator;
	}

	private ConditionTranslator(){}

	private class StringFilterTranslation extends ProcessorTranslation {
		private String string;
		private String value;

		private StringFilterTranslation(String clazz, String string, String value){
			this.clazz = clazz;
			this.string = string;
			this.value = value;
		}

		private StringFilterTranslation(Condition condition, String value) {
			this(OperatorTypeEnum.getTypeByName(condition.getOperator()).getType(), condition.getLeft(), value);
		}

		private StringFilterTranslation(Condition condition){
			this(condition, condition.getRight().get(0));
		}

	}

	public Object translateToHpJson(Condition condition) {
		return 1 == condition.getRight().size() ? new StringFilterTranslation(condition) : new FiltersTranslation(ConditionTypeEnum.ANY.getFilterClass(), condition.getRight().stream()
				.map(r -> new StringFilterTranslation(condition, r)).collect(Collectors.toList()));
	}

	Object notifyOidTranslation(String notifyOid) {
		return new StringFilterTranslation(OperatorTypeEnum.STARTS_WITH.getType(),"${notify OID}", notifyOid);
	}
}

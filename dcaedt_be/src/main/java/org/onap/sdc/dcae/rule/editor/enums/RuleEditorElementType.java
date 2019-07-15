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

import org.onap.sdc.dcae.rule.editor.translators.*;
import org.onap.sdc.dcae.rule.editor.validators.*;

import java.util.Arrays;

public enum RuleEditorElementType {
	COPY("Copy", CopyActionValidator.getInstance(), CopyActionTranslator.getInstance()),
	CONCAT("Concat", ConcatActionValidator.getInstance(), CopyActionTranslator.getInstance()),
	MAP("Map", MapActionValidator.getInstance(), MapActionTranslator.getInstance()),
	REGEX("Regex", CopyActionValidator.getInstance(), RegexActionTranslator.getInstance()),
	DATE_FORMATTER("DateFormatter", DateFormatterValidator.getInstance(), DateFormatterTranslator.getInstance()),
	//1806 US390049 additional hp processors support
	CLEAR("Clear", ClearActionValidator.getInstance(), ClearActionTranslator.getInstance()),
	REPLACE_TEXT("ReplaceText", ReplaceActionValidator.getInstance(), ReplaceActionTranslator.getInstance()),
	LOG_EVENT("LogEvent", LogEventValidator.getInstance(), LogEventTranslator.getInstance()),
	LOG_TEXT("LogText", LogTextValidator.getInstance(), LogTextTranslator.getInstance()),
	//1810 US420763 additional hp processors support
	CLEAR_NSF("ClearNSF", ClearActionValidator.getInstance(), ClearActionTranslator.getInstance()),
	HP_METRIC("HpMetric", HpMetricValidator.getInstance(), HpMetricTranslator.getInstance()),
	STRING_TRANSFORM("StringTransform", StringTransformValidator.getInstance(), StringTransformTranslator.getInstance()),
	TOPO_SEARCH("TopologySearch", TopoSearchValidator.getInstance(), TopoSearchTranslator.getInstance()),

	CONDITION("Condition", ConditionValidator.getInstance(), ConditionTranslator.getInstance()),
	FIELD_CONDITION("FieldCondition", ConditionValidator.getInstance(), FieldConditionTranslator.getInstance()),
	CONDITION_GROUP("ConditionGroup", ConditionGroupValidator.getInstance(), ConditionGroupTranslator.getInstance()),

	RULE("Rule", RuleValidator.getInstance(), RuleTranslator.getInstance()),
	MAPPING_RULES("MappingRules", MappingRulesValidator.getInstance(), MappingRulesTranslator.getInstance());

	private String elementType;
	private IRuleElementValidator validator;
	private IRuleElementTranslator translator;

	public IRuleElementValidator getValidator() {
		return validator;
	}

	public IRuleElementTranslator getTranslator() {
		return translator;
	}

	RuleEditorElementType(String elementType, IRuleElementValidator validator, IRuleElementTranslator translator) {
		this.elementType = elementType;
		this.validator = validator;
		this.translator = translator;
	}

	public static RuleEditorElementType getElementTypeByName(String name) {
		return Arrays.stream(RuleEditorElementType.values()).filter(p -> p.elementType.equalsIgnoreCase(name))
				.findAny().orElse(null);
	}
}

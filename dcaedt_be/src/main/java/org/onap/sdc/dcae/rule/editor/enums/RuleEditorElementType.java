package org.onap.sdc.dcae.rule.editor.enums;

import java.util.Arrays;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.LogEventAction;
import org.onap.sdc.dcae.rule.editor.translators.*;
import org.onap.sdc.dcae.rule.editor.validators.*;

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

	CONDITION("Condition", ConditionValidator.getInstance(), ConditionTranslator.getInstance()),
	FIELD_CONDITION("FieldCondition", ConditionValidator.getInstance(), FieldConditionTranslator.getInstance()),
	CONDITION_GROUP("ConditionGroup", ConditionGroupValidator.getInstance(), ConditionGroupTranslator.getInstance()),

	RULE("Rule", RuleValidator.getInstance(), RuleTranslator.getInstance()),
	MAPPING_RULES("MappingRules", null, MappingRulesTranslator.getInstance());

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

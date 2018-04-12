package org.onap.sdc.dcae.rule.editor.enums;

import java.util.Arrays;

import org.onap.sdc.dcae.rule.editor.translators.ConditionGroupTranslator;
import org.onap.sdc.dcae.rule.editor.translators.ConditionTranslator;
import org.onap.sdc.dcae.rule.editor.translators.CopyActionTranslator;
import org.onap.sdc.dcae.rule.editor.translators.DateFormatterTranslator;
import org.onap.sdc.dcae.rule.editor.translators.FieldConditionTranslator;
import org.onap.sdc.dcae.rule.editor.translators.IRuleElementTranslator;
import org.onap.sdc.dcae.rule.editor.translators.MapActionTranslator;
import org.onap.sdc.dcae.rule.editor.translators.MappingRulesTranslator;
import org.onap.sdc.dcae.rule.editor.translators.RegexActionTranslator;
import org.onap.sdc.dcae.rule.editor.translators.RuleTranslator;
import org.onap.sdc.dcae.rule.editor.validators.ActionValidator;
import org.onap.sdc.dcae.rule.editor.validators.ConcatActionValidator;
import org.onap.sdc.dcae.rule.editor.validators.ConditionGroupValidator;
import org.onap.sdc.dcae.rule.editor.validators.ConditionValidator;
import org.onap.sdc.dcae.rule.editor.validators.DateFormatterValidator;
import org.onap.sdc.dcae.rule.editor.validators.IRuleElementValidator;
import org.onap.sdc.dcae.rule.editor.validators.MapActionValidator;
import org.onap.sdc.dcae.rule.editor.validators.RuleValidator;

public enum RuleEditorElementType {
	COPY("Copy", ActionValidator.getInstance(), CopyActionTranslator.getInstance()),
	CONCAT("Concat", ConcatActionValidator.getInstance(), CopyActionTranslator.getInstance()),
	MAP("Map", MapActionValidator.getInstance(), MapActionTranslator.getInstance()),
	REGEX("Regex", ActionValidator.getInstance(), RegexActionTranslator.getInstance()),
	DATE_FORMATTER("DateFormatter", DateFormatterValidator.getInstance(), DateFormatterTranslator.getInstance()),
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

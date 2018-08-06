package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRules;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Rule;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.onap.sdc.dcae.ves.VesStructureLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MappingRulesValidator implements IRuleElementValidator<MappingRules> {

	private RuleValidator ruleValidator = RuleValidator.getInstance();

	private static MappingRulesValidator mappingRulesValidator = new MappingRulesValidator();

	public static MappingRulesValidator getInstance() {
		return mappingRulesValidator;
	}

	private MappingRulesValidator(){}

	public boolean validate(MappingRules rules, List<ResponseFormat> errors) {
	    return validateVersionAndType(rules, errors) && validateImportedGroupDefinitions(rules, errors) && rules.getRules().values().stream().map(r -> ruleValidator.validate(r, errors))
				.reduce(true, (x,y) -> x && y);
	    // TODO consider using 'allMatch' which will stop on the first error
	}

	private boolean validateVersionAndType(MappingRules rules, List<ResponseFormat> errors) {
		Map<String, Set<String>> supportedVersions = VesStructureLoader.getAvailableVersionsAndEventTypes();
		boolean valid = ValidationUtils.validateNotEmpty(rules.getVersion()) && supportedVersions.containsKey(rules.getVersion()) && ValidationUtils.validateNotEmpty(rules.getEventType()) && supportedVersions.get(rules.getVersion()).contains(rules.getEventType());
		errLogger.log(LogLevel.INFO, this.getClass().getName(), "validate mapping rules fields: eventType/version {}", valid);
		if(!valid) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.VES_SCHEMA_NOT_FOUND, null));
		}
		return valid;
	}

	private boolean validateImportedGroupDefinitions(MappingRules rules, List<ResponseFormat> errors) {
		boolean valid = !rules.isEmpty() && (rules.getRules().values().stream().noneMatch(r -> ValidationUtils.validateNotEmpty(r.getGroupId()) || ValidationUtils.validateNotEmpty(r.getPhase())) || validateGroupDefinitions(rules));
		errLogger.log(LogLevel.INFO, this.getClass().getName(), "validate group definitions {}", valid);
		if(!valid) {
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_RULE_FORMAT, null, "invalid phase definitions"));
		}
		return valid;
	}

	private boolean validateGroupDefinitions(MappingRules rules) {
		return rules.getRules().values().stream().allMatch(r -> ValidationUtils.validateNotEmpty(r.getGroupId()) && ValidationUtils.validateNotEmpty(r.getPhase()))
				&& rules.getRules().values().stream().collect(Collectors.groupingBy(Rule::getGroupId, Collectors.mapping(Rule::getPhase, Collectors.toSet()))).values().stream().allMatch(p -> 1 == p.size());
	}


	public boolean validateTranslationPhaseNames(MappingRules rules, List<ResponseFormat> errors) {
		boolean valid = true;
		Set<String> phases = rules.getRules().values().stream().map(Rule::getPhase).collect(Collectors.toSet());
		if(phases.contains(rules.getEntryPhase())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.TRANSLATE_FAILED, null, "entry phase name already exists"));
		}
		if(phases.contains(rules.getPublishPhase())) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.TRANSLATE_FAILED, null, "publish phase name already exists"));
		}
		return valid;
	}
}

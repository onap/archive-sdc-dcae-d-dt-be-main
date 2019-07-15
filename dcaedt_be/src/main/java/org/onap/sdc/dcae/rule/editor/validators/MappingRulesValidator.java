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
		boolean valid = true;
		if(rules.isEmpty()) {
			valid = false;
			errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.INVALID_RULE_FORMAT, "", "no rules found"));
		}
		return rules.getRules().values().stream().map(r -> ruleValidator.validate(r, errors))
				.reduce(true, (x,y) -> x && y) && valid;
		// TODO consider using 'allMatch' which will stop on the first error
	}

	public boolean validateVersionAndType(MappingRules rules) {
		Map<String, Set<String>> supportedVersions = VesStructureLoader.getAvailableVersionsAndEventTypes();
		return ValidationUtils.validateNotEmpty(rules.getVersion()) && supportedVersions.containsKey(rules.getVersion()) && ValidationUtils.validateNotEmpty(rules.getEventType()) && supportedVersions.get(rules.getVersion()).contains(rules.getEventType());
	}


	public boolean validateGroupDefinitions(MappingRules rules) {
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

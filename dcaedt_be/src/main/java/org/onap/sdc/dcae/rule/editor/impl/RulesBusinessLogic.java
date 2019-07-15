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

package org.onap.sdc.dcae.rule.editor.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.rule.editor.enums.RuleEditorElementType;
import org.onap.sdc.dcae.rule.editor.translators.MappingRulesTranslator;
import org.onap.sdc.dcae.rule.editor.utils.EmptyStringTranslationSerializer;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.onap.sdc.dcae.rule.editor.validators.MappingRulesValidator;
import org.onap.sdc.dcae.rule.editor.validators.RuleValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RulesBusinessLogic {

	protected OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
	private RuleValidator ruleValidator = RuleValidator.getInstance();
	private MappingRulesValidator mappingRulesValidator = MappingRulesValidator.getInstance();
	private MappingRulesTranslator mappingRulesTranslator = MappingRulesTranslator.getInstance();
	private static Gson gsonTranslator = new GsonBuilder().registerTypeAdapter(String.class, new EmptyStringTranslationSerializer()).enableComplexMapKeySerialization().create();


	public List<ServiceException> validateFilter(BaseCondition filter) {
		List<ResponseFormat> errors = new ArrayList<>();
		RuleEditorElementType.getElementTypeByName(filter.getClass().getSimpleName()).getValidator().validate(filter, errors);
		return errors.stream().map(r -> r.getRequestError().getServiceException()).collect(Collectors.toList());
	}

	public List<ServiceException> validateRule(Rule rule) {
		List<ResponseFormat> errors = new ArrayList<>();
		if(ruleValidator.validate(rule, errors)) {
			detectAndResolveActionDependencies(rule, errors);
		}
		return errors.stream().map(r -> r.getRequestError().getServiceException()).collect(Collectors.toList());
	}

	public List<ServiceException> validateImportedRules(MappingRules rules) {
		List<ResponseFormat> errors = new ArrayList<>();
		if(mappingRulesValidator.validate(rules, errors)) {
			rules.getRules().forEach((k,v) -> {
				v.setUid(k);
				detectAndResolveActionDependencies(v, errors);
			});
		}
		return errors.stream().map(r -> r.getRequestError().getServiceException()).collect(Collectors.toList());
	}


	public List<ServiceException> validateRulesBeforeTranslate(MappingRules rules) {
		List<ResponseFormat> errors = new ArrayList<>();
		if(mappingRulesValidator.validateTranslationPhaseNames(rules, errors)) {
			detectAndResolveRuleDependencies(rules, errors);
		}
		return errors.stream().map(r -> r.getRequestError().getServiceException()).collect(Collectors.toList());
	}

	public String translateRules(MappingRules rules) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Start translating mapping rules");
		return gsonTranslator.toJson(mappingRulesTranslator.translateToHpJson(rules));
	}

	public boolean addOrEditRule(MappingRules rules, Rule rule, boolean supportGroups) {
		// in case the rule id is passed but the rule doesn't exist on the mapping rule file:
		if(StringUtils.isNotBlank(rule.getUid()) && !rules.ruleExists(rule)) {
			return false;
		}
		// 1810 US427299 support user defined phase names - propagate update to all group members
		if(supportGroups) {
			rules.getRules().values().stream().filter(p -> rule.getGroupId().equals(p.getGroupId())).forEach(r -> r.setPhase(rule.getPhase()));
		}
		rules.addOrReplaceRule(rule);
		return true;
	}


	// when saving a single rule its declared format (supportGroups) must match the existing rules format (a single match is enough as all previously saved rules were already validated)
	public boolean validateGroupDefinitions(MappingRules rules, boolean supportGroups) {
		return supportGroups == rules.getRules().values().stream().anyMatch(r -> ValidationUtils.validateNotEmpty(r.getGroupId()));
	}

	public Rule deleteRule(MappingRules rules, String ruleUid) {
		return rules.removeRule(ruleUid);
	}

	public List<Rule> deleteGroupOfRules(MappingRules rules, String groupId) {
		List<Rule> rulesByGroupId = rules.getRules().values().stream().filter(p -> groupId.equals(p.getGroupId())).collect(Collectors.toList());
		return rulesByGroupId.stream().map(rule -> rules.removeRule(rule.getUid())).collect(Collectors.toList());
	}

	private <T> List<T> detectDependentItemsByDependencyDefinition(Collection<T> allItems, BiFunction<T, Collection<T>, Boolean> dependencyDefinition) {
		return allItems.stream().filter(i -> dependencyDefinition.apply(i, allItems)).collect(Collectors.toList());
	}

	//	 if all dependencies are resolvable returns empty list
	//	 else returns list of non resolvable items (circular dependent items)
	//	 iterate through all dependentItems removing resolvable items each iteration.

	private <T> List<T> detectCircularDependenciesByDependencyDefinition(List<T> dependentItems, BiFunction<T, Collection<T>, Boolean> dependencyDetector) {
		while(!CollectionUtils.isEmpty(dependentItems)) {
			List<T> resolvable = dependentItems.stream()
					.filter(i -> !dependencyDetector.apply(i, dependentItems))
					.collect(Collectors.toList());
			if(CollectionUtils.isEmpty(resolvable)) {
				break;
			}
			dependentItems.removeAll(resolvable);
		}
		return dependentItems;
	}

	private <T> List<T> reorderItemsByDependencyDefinition(Collection<T> allItems, BiFunction<T, T, Boolean> dependencyDetector) {
		List<T> ordered = new ArrayList<>(allItems);
		allItems.forEach(i -> {
			List<T> dependencies = allItems.stream().filter(o -> dependencyDetector.apply(i, o)).collect(Collectors.toList());
			dependencies.forEach(d -> {
				if(ordered.indexOf(d) > ordered.indexOf(i)) {
					ordered.remove(d);
					ordered.add(ordered.indexOf(i), d);
				}
			});
		});
		return ordered;
	}

	private void detectAndResolveActionDependencies(Rule rule, List<ResponseFormat> errors) {
		List<BaseAction> dependentActions = detectDependentItemsByDependencyDefinition(rule.getActions(), BaseAction::hasDependencies);
		if(!CollectionUtils.isEmpty(dependentActions)) {
			List<BaseAction> nonResolvable = detectCircularDependenciesByDependencyDefinition(dependentActions, BaseAction::hasDependencies);
			if (!CollectionUtils.isEmpty(nonResolvable)) {
				errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.ACTION_DEPENDENCY, null, nonResolvable.stream().map(BaseAction::strippedTarget).collect(Collectors.joining(", "))));
				return;
			}
			List<BaseAction> actions = reorderItemsByDependencyDefinition(rule.getActions(), BaseAction::referencesTarget);
			rule.setActions(actions);
		}
	}

	// first identify dependent rules
	// if no dependencies found return true
	// if non resolvable dependencies found return false
	// else reorder and return true

	private void detectAndResolveRuleDependencies(MappingRules rules, List<ResponseFormat> errors) {
		List<Rule> dependentRules = detectDependentItemsByDependencyDefinition(rules.getRules().values(), Rule::referencesOtherRules);
		if(!CollectionUtils.isEmpty(dependentRules)) {
			List<Rule> nonResolvable = detectCircularDependenciesByDependencyDefinition(dependentRules, Rule::referencesOtherRules);
			if (!CollectionUtils.isEmpty(nonResolvable)) {
				String nonResolvableRuleIds = nonResolvable.stream().map(Rule::getUid).collect(Collectors.joining(", "));
				errors.add(ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.RULE_DEPENDENCY, null, nonResolvableRuleIds, extractDependentActionTargetsFromRules(nonResolvable)));
				return;
			}
			reorderRulesByDependency(rules);
		}
	}


	private String extractDependentActionTargetsFromRules(List<Rule> dependentRules) {
		List<BaseAction> allActions = dependentRules.stream().map(Rule::getActions).flatMap(List::stream).collect(Collectors.toList());
		// option 1: circular dependency between actions
		List<BaseAction> nonResolvable = detectCircularDependenciesByDependencyDefinition(allActions, BaseAction::hasDependencies);
		if(CollectionUtils.isEmpty(nonResolvable)) {
			// option 2: circular dependency between rules - collect dependent actions and condition dependencies
			nonResolvable = dependentRules.stream()
					.map(r -> r.findDependencies(dependentRules))
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
		return nonResolvable.stream()
				.map(BaseAction::strippedTarget)
				.collect(Collectors.joining(", "));
	}

	private void reorderRulesByDependency(MappingRules rules) {
		List<Rule> ordered = reorderItemsByDependencyDefinition(rules.getRules().values(), Rule::referencesOtherRule);
		Map<String, Rule> rulesMap = ordered.stream().collect(Collectors.toMap(Rule::getUid, Function.identity(), (u, v) -> {
			throw new IllegalStateException(String.format("Duplicate key %s", u));
		}, LinkedHashMap::new));
		rules.setRules(rulesMap);
	}

	public void updateGlobalTranslationFields(MappingRules mappingRules, TranslateRequest request, String vfcmtName) {
		mappingRules.setEntryPhase(request.getEntryPhase());
		mappingRules.setPublishPhase(request.getPublishPhase());
		mappingRules.setNotifyId(request.getNotifyId());
		if(validateGroupDefinitions(mappingRules, false)) {
			// 1806 US349308 assign Vfcmt name as rule phaseName
			mappingRules.getRules().forEach((k,v) -> v.setPhase(vfcmtName));
		}
	}
}

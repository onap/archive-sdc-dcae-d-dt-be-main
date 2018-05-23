package org.onap.sdc.dcae.rule.editor.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.rule.editor.translators.MappingRulesTranslator;
import org.onap.sdc.dcae.rule.editor.utils.EmptyStringTranslationSerializer;
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
	private MappingRulesTranslator mappingRulesTranslator = MappingRulesTranslator.getInstance();
	private static Gson gsonTranslator = new GsonBuilder().registerTypeAdapter(String.class, new EmptyStringTranslationSerializer()).enableComplexMapKeySerialization().create();

	public List<ServiceException> validateRule(Rule rule) {
		List<ResponseFormat> errors = new ArrayList<>();
		if(ruleValidator.validate(rule, errors)) {
			detectAndResolveActionDependencies(rule, errors);
		}
		return errors.stream().map(r -> r.getRequestError().getServiceException()).collect(Collectors.toList());
	}

	public List<ServiceException> validateRules(MappingRules rules) {
		List<ResponseFormat> errors = new ArrayList<>();
		detectAndResolveRuleDependencies(rules, errors);
		return errors.stream().map(r -> r.getRequestError().getServiceException()).collect(Collectors.toList());
	}

	public String translateRules(MappingRules rules, String entryPointPhase, String lastPhase, String runPhase) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Start translating mapping rules");
		return gsonTranslator.toJson(mappingRulesTranslator.translateToHpJson(rules, entryPointPhase, lastPhase, runPhase));
	}

	public boolean addOrEditRule(MappingRules rules, Rule rule) {
		// in case the rule id is passed but the rule doesn't exist on the mapping rule file:
		if(StringUtils.isNotBlank(rule.getUid()) && !rules.ruleExists(rule)) {
			return false;
		}
		rules.addOrReplaceRule(rule);
		return true;
	}

	public Rule deleteRule(MappingRules rules, String ruleUid) {
		return rules.removeRule(ruleUid);
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
}

package org.onap.sdc.dcae.rule.editor.translators;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRules;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Rule;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MappingRulesTranslator implements IRuleElementTranslator<MappingRules> {

	private static MappingRulesTranslator mappingRulesTranslator = new MappingRulesTranslator();

	public static MappingRulesTranslator getInstance() {
		return mappingRulesTranslator;
	}

	private MappingRulesTranslator() {
	}

	private RuleTranslator ruleTranslator = RuleTranslator.getInstance();
	private ConditionTranslator conditionTranslator = ConditionTranslator.getInstance();

	public Object translateToHpJson(MappingRules mappingRules) {
		return new MappingRulesTranslation(mappingRules);
	}

	private List<Object> getPhaseTranslation(Collection<Rule> currentPhase) {
		return currentPhase.stream().map(ruleTranslator::translateToHpJson).collect(Collectors.toList());
	}

	private class MappingRulesTranslation {

		private List<Object> processing;

		private MappingRulesTranslation(MappingRules mappingRules) {
			String firstRunPhase;
			// single phase
			if(mappingRules.getRules().values().stream().noneMatch(r -> ValidationUtils.validateNotEmpty(r.getGroupId()))) {
				processing = getPhaseTranslation(mappingRules.getRules().values());
				firstRunPhase = mappingRules.getRules().values().iterator().next().getPhase();
			} else {
				// 1810 US427299 phase grouping - support user defined phase names
				Map<String, List<Rule>> reorderByGroupId = mappingRules.getRules().values().stream().collect(Collectors.groupingBy(Rule::getGroupId, LinkedHashMap::new, Collectors.toList()));
				List<String> phaseNames = new ArrayList<>();
				processing = new ArrayList<>();
				reorderByGroupId.forEach((k,v) -> {
					String currentPhaseName = v.get(0).getPhase();
					// if phase name already triggered no need to call RunPhase processor again.
					if(!processing.isEmpty() && !phaseNames.contains(currentPhaseName)) {
						((RuleTranslation)processing.get(processing.size()-1)).processors.add(new RunPhaseProcessorsTranslation(currentPhaseName));
					}
					processing.addAll(getPhaseTranslation(v));
					phaseNames.add(currentPhaseName);
				});
				firstRunPhase = phaseNames.get(0);
			}
			//hardcoded entry point processor - added as a phase unit
			processing.add(0, new RunPhaseRuleTranslation(mappingRules.getEntryPhase(), firstRunPhase, mappingRules.getNotifyId()));
			//hardcoded map_publish processor - added as processor unit to last phase unit
			((RuleTranslation)processing.get(processing.size()-1)).processors.add(new RunPhaseProcessorsTranslation(mappingRules.getPublishPhase()));
		}
	}


	private class RunPhaseRuleTranslation extends RuleTranslation {

		private RunPhaseRuleTranslation(String phaseName, String runPhase) {
			phase = phaseName;
			processors.add(new RunPhaseProcessorsTranslation(runPhase));
		}

		private RunPhaseRuleTranslation(String phaseName, String runPhase, String notifyOid) {
			this(phaseName, runPhase);
			if("snmp_map".equals(phaseName)) {
				processors.add(0, new SnmpConvertor());
			}
			if(ValidationUtils.validateTargetField(notifyOid)) {
				filter = conditionTranslator.notifyOidTranslation(notifyOid);
			}
		}
	}

	// hardcoded SNMP processor
	private class SnmpConvertor extends ProcessorTranslation {
		private String array = "varbinds";
		private String datacolumn = "varbind_value";
		private String keycolumn = "varbind_oid";

		private SnmpConvertor() {
			clazz = "SnmpConvertor";
		}
	}

}

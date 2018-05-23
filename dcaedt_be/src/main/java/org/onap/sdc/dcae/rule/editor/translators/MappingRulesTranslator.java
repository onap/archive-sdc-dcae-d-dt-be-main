package org.onap.sdc.dcae.rule.editor.translators;

import java.util.List;
import java.util.stream.Collectors;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRules;

public class MappingRulesTranslator implements IRuleElementTranslator<MappingRules> {

	private static MappingRulesTranslator mappingRulesTranslator = new MappingRulesTranslator();

	public static MappingRulesTranslator getInstance() {
		return mappingRulesTranslator;
	}

	private MappingRulesTranslator() {
	}

	private RuleTranslator ruleTranslator = RuleTranslator.getInstance();

	public Object translateToHpJson(MappingRules mappingRules) {
		return new MappingRulesTranslation(mappingRules);
	}

	public Object translateToHpJson(MappingRules mappingRules, String entryPointPhaseName, String lastPhaseName, String runPhase) {
		// 1806 US349308 assign Vfcmt name as rule phaseName
		mappingRules.getRules().forEach((k,v) -> v.setPhase(runPhase));
		return new MappingRulesTranslation(mappingRules, entryPointPhaseName, lastPhaseName, runPhase);
	}

	private class MappingRulesTranslation {

		private List<Object> processing;

		private MappingRulesTranslation(MappingRules mappingRules) {
			processing = mappingRules.getRules().values().stream().map(ruleTranslator::translateToHpJson).collect(Collectors.toList());
		}

		private MappingRulesTranslation(MappingRules mappingRules, String entryPointPhaseName, String lastPhaseName, String runPhase) {
			this(mappingRules);
			//hardcoded entry point processor
			processing.add(0, new RunPhaseRuleTranslation(entryPointPhaseName, runPhase));
			//hardcoded map_publish processor
			processing.add(new RunPhaseRuleTranslation(runPhase, lastPhaseName));
		}
	}

	private class RunPhaseRuleTranslation extends RuleTranslation {

		private RunPhaseRuleTranslation(String phaseName, String runPhase) {
			phase = phaseName;
			if("snmp_map".equals(phaseName)) {
				processors.add(new SnmpConvertor());
			}
			processors.add(new RunPhaseProcessorsTranslation(runPhase));
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

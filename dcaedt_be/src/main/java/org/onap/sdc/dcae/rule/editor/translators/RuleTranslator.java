package org.onap.sdc.dcae.rule.editor.translators;

import com.google.gson.Gson;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.rule.editor.enums.OperatorTypeEnum;
import org.onap.sdc.dcae.rule.editor.enums.RuleEditorElementType;
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;

public class RuleTranslator implements IRuleElementTranslator<Rule> {

	private static RuleTranslator ruleTranslator = new RuleTranslator();

	public static RuleTranslator getInstance() {
		return ruleTranslator;
	}

	private RuleTranslator() {
	}

	private class ActionRuleTranslation extends RuleTranslation {
		private ActionRuleTranslation(Rule rule) {
			phase = rule.getPhase();
			filter = rule.isConditionalRule() ? getConditionTranslator(rule.getCondition()).translateToHpJson(rule.getCondition()) : null;
			boolean asNewProcessor = true;
			for (BaseAction action : rule.getActions()) {
				// consecutive copy actions are aggregated into a single processor
				asNewProcessor = getActionTranslator(action).addToHpJsonProcessors(action, processors, asNewProcessor);
			}
		}
	}

	private class EntryPhaseTranslation extends RuleTranslation {

		private EntryPhaseTranslation(String phaseName, String runPhase) {
			phase = phaseName;
			processors.add(new RunPhaseProcessorsTranslation(runPhase));
		}

		private EntryPhaseTranslation(String phaseName, String runPhase, BaseCondition entryPhaseFilter) {
			this(phaseName, runPhase);
			if("snmp_map".equals(phaseName)) {
				processors.add(0, new SnmpConvertor());
			}
			if(null != entryPhaseFilter) {
				filter = getConditionTranslator(entryPhaseFilter).translateToHpJson(entryPhaseFilter);
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

	public Object translateToHpJson(Rule rule) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Start translating rule {}", rule.getUid());
		Object translation = new ActionRuleTranslation(rule);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished translation for rule {}. Result: {}", rule.getUid(), new Gson().toJson(translation));
		return translation;
	}

	public Object entryPhaseTranslation(String entryPhase, String runPhase, BaseCondition entryPhaseFilter) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Start translating entry phase {}", entryPhase);
		Object translation = new EntryPhaseTranslation(entryPhase, runPhase, entryPhaseFilter);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished translation for entry phase {}. Result: {}", entryPhase, new Gson().toJson(translation));
		return translation;
	}

	private IRuleElementTranslator getConditionTranslator(BaseCondition condition){
		return condition instanceof ConditionGroup ? ConditionGroupTranslator.getInstance() :
				getSimpleConditionTranslator((Condition) condition);
	}

	private IRuleElementTranslator getSimpleConditionTranslator(Condition condition) {
		String conditionType = OperatorTypeEnum.getTypeByName(condition.getOperator()).getConditionType();
		return RuleEditorElementType.getElementTypeByName(conditionType).getTranslator();
	}


	private ActionTranslator getActionTranslator(BaseAction action) {
		ActionTypeEnum type = ActionTypeEnum.getTypeByName(action.getActionType());
		if(ActionTypeEnum.COPY == type && ValidationUtils.validateNotEmpty(((BaseCopyAction)action).regexValue())) {
			return RegexActionTranslator.getInstance();
		}
		return (ActionTranslator) RuleEditorElementType.getElementTypeByName(type.getType()).getTranslator();
	}
}
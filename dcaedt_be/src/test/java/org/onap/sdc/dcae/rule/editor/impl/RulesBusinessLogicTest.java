package org.onap.sdc.dcae.rule.editor.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormatManager;
import org.onap.sdc.dcae.errormng.ServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RulesBusinessLogicTest {
	private static Gson gson = new GsonBuilder()
			.registerTypeAdapter(BaseAction.class, new ActionDeserializer())
			.registerTypeAdapter(BaseCondition.class, new ConditionDeserializer()).create();

	@InjectMocks
	private RulesBusinessLogic rulesBusinessLogic = new RulesBusinessLogic();
	private ResponseFormatManager responseFormatManager = null;

	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
		ErrorConfigurationLoader errorConfigurationLoader = new ErrorConfigurationLoader(System.getProperty("user.dir")+"/src/main/webapp/WEB-INF");
		responseFormatManager = ResponseFormatManager.getInstance();
	}

	@Test
	public void translateSingleRuleSingleCopyActionAddSnmpHeader() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\"},\"class\":\"Set\"}]},"
				+ "{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildCopyAction("2.0","event.commonEventHeader.version"));
		rule.setDescription("description");
		MappingRules mr = new MappingRules(rule);
		List<ServiceException> errors = rulesBusinessLogic.validateRules(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void translateSingleRuleSingleDateFormatterActionSnmpFlow() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"fromFormat\":\"fromFormat\",\"fromTz\":\"fromTZ\",\"toField\":\"targetField\",\"toFormat\":\"toFormat\",\"toTz\":\"toTz\",\"value\":\"fromField\",\"class\":\"DateFormatter\"}]},"
				+ "{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildDateFormatterAction());
		rule.setDescription("description");
		MappingRules mr = new MappingRules(rule);
		List<ServiceException> errors = rulesBusinessLogic.validateRules(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void translateSingleRuleMultipleCopyActionsAddSnmpHeader() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\","
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		List<ServiceException> errors = rulesBusinessLogic.validateRules(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void translateMultipleRulesMultipleCopyActionsAddSnmpHeader() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\","
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\","
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		mr.addOrReplaceRule(buildRuleWithMultipleCopyActions());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void emptyStringTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"map\":{\"\":\"\"},\"field\":\"\",\"toField\":\"mapTargetField\",\"default\":\"\",\"class\":\"MapAlarmValues\"}]},"
				+ "{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String ruleRequestBody = "{version:4.1,eventType:syslogFields,description:description,actions:[{actionType:map,from:{value:'\"\"'},target:mapTargetField,map:{values:[{key:'\"\"',value:'\"\"'}],haveDefault:true,default:'\"\"'}}]}";
		Rule myRule = gson.fromJson(ruleRequestBody, Rule.class);
		MappingRules mr = new MappingRules(myRule);
		List<ServiceException> errors = rulesBusinessLogic.validateRules(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void singleStringConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"syslog_map\",\"processors\":[{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"filter\":{\"string\":\"left\",\"value\":\"right\",\"class\":\"Contains\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:contains,left:left,right:[right]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "syslog_map", "map_publish", "phase_1"));
	}

	@Test
	public void multiStringConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"foi_map\",\"processors\":[{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},"
				+ "{\"phase\":\"phase_1\",\"filter\":{\"filters\":[{\"string\":\"left\",\"value\":\"right1\",\"class\":\"Contains\"},{\"string\":\"left\",\"value\":\"right2\",\"class\":\"Contains\"}],\"class\":\"Or\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:contains,left:left,right:[right1, right2]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "foi_map", "map_publish", "phase_1"));
	}

	@Test
	public void singleFieldConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"filter\":{\"field\":\"left\",\"value\":\"right\",\"class\":\"Equals\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:equals,left:left,right:[right]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void multiFieldConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"filter\":{\"field\":\"left\",\"values\":[\"right1\",\"right2\"],\"class\":\"NotOneOf\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:notequal,left:left,right:[right1,right2]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr, "snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void reorderRuleActionsDuringValidationSuccessTest() {
		Rule rule1 = buildValidRuleWithDependentActions();
		Rule rule2 = buildValidRuleWithDependentActions();
		assertEquals(rule1, rule2);
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule1);
		assertTrue(errors.isEmpty());
		assertNotEquals(rule1, rule2);
		//after validation actions are reordered: 1, 3, 4, 2, 5
		rule2.getActions().add(1, rule2.getActions().get(2)); // 1, 2, 3, 4, 5 -> 1, 3, 2, 3, 4, 5
		rule2.getActions().remove(3); // 1, 3, 2, 3, 4, 5 -> 1, 3, 2, 4, 5
		rule2.getActions().add(2, rule2.getActions().get(3)); // 1, 3, 2, 4, 5 -> 1, 3, 4, 2, 4, 5
		rule2.getActions().remove(4); // 1, 3, 4, 2, 4, 5 -> 1, 3, 4, 2, 5
		assertEquals(rule1, rule2);
	}

	@Test
	public void reorderRuleActionsDuringValidationFailureTest() {
		String expectedError = "A circular dependency was detected between actions. The following fields should be resolved: event.commonEventHeader.eventId, event.commonEventHeader.sourceName, invalidSelfDependency, circularDependencyTarget_3";
		Rule rule1 = buildRuleWithCircularActionDependencies();
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule1);
		assertEquals(expectedError, errors.get(0).getFormattedErrorMessage());
	}


	@Test
	public void reorderMappingRulesByDependencySuccessTest() {
		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		Rule rule = new Rule();
		rule.setDescription("description");
		rule.setActions(new ArrayList<>());
		// create a dependency between rules
		rule.getActions().add(buildCopyAction("${event.commonEventHeader.someField}","event.commonEventHeader.sourceName"));
		mr.addOrReplaceRule(rule);
		List<String> ruleUids = new ArrayList<>(mr.getRules().keySet());
		String translateBefore = rulesBusinessLogic.translateRules(mr,"snmp_map", "map_publish", "phase_1");
		List<ServiceException> errors = rulesBusinessLogic.validateRules(mr);
		assertTrue(errors.isEmpty());
		List<String> ruleUidsMod = new ArrayList<>(mr.getRules().keySet());
		assertEquals(ruleUids.get(0), ruleUidsMod.get(1));
		assertEquals(ruleUids.get(1), ruleUidsMod.get(0));
		assertNotEquals(translateBefore,  rulesBusinessLogic.translateRules(mr,"snmp_map", "map_publish", "phase_1"));
	}

	@Test
	public void reorderMappingRulesCircularDependencyFailureTest() {

		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		List<ServiceException> errors = rulesBusinessLogic.validateRules(mr);
		assertTrue(errors.isEmpty());
		Rule rule = new Rule();
		rule.setDescription("description");
		rule.setActions(new ArrayList<>());
		// create a circular dependency between rules
		rule.getActions().add(buildCopyAction("${event.commonEventHeader.version}","event.commonEventHeader.sourceName"));
		String input = "{operator:equals,left:\"${event.commonEventHeader.version}\",right:[\"${event.commonEventHeader.eventId}\"]}";
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		assertTrue(rulesBusinessLogic.addOrEditRule(mr, rule));
		errors = rulesBusinessLogic.validateRules(mr);
		assertFalse(errors.isEmpty());
		String expectedError = String.format("A circular dependency was detected between rules: %s, %s within fields: event.commonEventHeader.sourceName, event.commonEventHeader.version, event.commonEventHeader.eventId", mr.getRules().keySet().toArray());
		assertEquals(expectedError, errors.get(0).getFormattedErrorMessage());
	}


	@Test
	public void translateNestedComplexConditionSuccessTest() {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"foi_map\",\"processors\":[{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},"
				+ "{\"phase\":\"phase_1\",\"filter\":{\"filters\":[{\"field\":\"${event.commonEventHeader.version}\",\"value\":\"${event.commonEventHeader.eventId}\",\"class\":\"Equals\"},"
				+ "{\"filters\":[{\"field\":\"left\",\"value\":\"right\",\"class\":\"NotEqual\"},{\"string\":\"${XXX}\",\"value\":\"right1\",\"class\":\"Contains\"},"
				+ "{\"string\":\"${XXX}\",\"value\":\"right2\",\"class\":\"Contains\"}],\"class\":\"Or\"}],\"class\":\"And\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\"},\"class\":\"Set\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildCopyAction("2.0","event.commonEventHeader.version"));
		rule.setDescription("description");
		String condition = "{type:All,children:[{operator:equals,left:\"${event.commonEventHeader.version}\",right:[\"${event.commonEventHeader.eventId}\"]},"
				+ "{type:Any,children:[{operator:contains,left:\"${XXX}\",right:[right1,right2]},{operator:notEqual,left:left,right:[right]}]}]}";
		rule.setCondition(gson.fromJson(condition, BaseCondition.class));
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(new MappingRules(rule),"foi_map", "map_publish", "phase_1"));
	}

	private Rule buildRuleWithMultipleCopyActions() {
		Rule rule = new Rule();
		rule.setDescription("description");
		List<BaseAction> actions = new ArrayList<>();
		actions.add(buildCopyAction("2.0","event.commonEventHeader.version"));
		actions.add(buildConcatAction(Arrays.asList("${event.commonEventHeader.sourceName}","_","${eventGroup}"), "event.commonEventHeader.eventId"));
		actions.add(buildRegexAction("extractFromHere", "targetField", "([^:]*):.*"));
		rule.setActions(actions);
		return rule;
	}

	private Rule buildValidRuleWithDependentActions() {
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.getActions().add(buildConcatAction(Arrays.asList("${targetField}","_","${circularDependencyTarget_3}"), "event.commonEventHeader.sourceName"));
		rule.getActions().add(buildConcatAction(Arrays.asList("${validSelfDependency}","_","${event.commonEventHeader.version}"), "validSelfDependency"));
		return rule;
	}

	private Rule buildRuleWithCircularActionDependencies() {
		Rule rule = buildValidRuleWithDependentActions();
		rule.getActions().add(buildCopyAction("${invalidSelfDependency}", "invalidSelfDependency"));
		rule.getActions().add(buildCopyAction("${event.commonEventHeader.eventId}", "circularDependencyTarget_3"));
		return rule;
	}

	private BaseAction buildCopyAction(String from, String to) {
		BaseAction action = new BaseAction();
		action.setActionType("copy");
		action.setFrom(from);
		action.setTarget(to);
		return action;
	}

	private BaseAction buildConcatAction(List<String> from, String to) {
		BaseAction action = new BaseAction();
		action.setActionType("concat");
		action.setFrom(from);
		action.setTarget(to);
		return action;
	}

	private BaseAction buildRegexAction(String from, String to, String regex) {
		BaseAction action = new BaseAction();
		action.setActionType("copy");
		action.setFrom(from, regex);
		action.setTarget(to);
		return action;
	}

	private DateFormatterAction buildDateFormatterAction() {
		DateFormatterAction action = new DateFormatterAction();
		action.setActionType("date formatter");
		action.setFrom("fromField");
		action.setTarget("targetField");
		action.setFromFormat("fromFormat");
		action.setToFormat("toFormat");
		action.setFromTz("fromTZ");
		action.setToTz("toTz");
		return action;
	}
}
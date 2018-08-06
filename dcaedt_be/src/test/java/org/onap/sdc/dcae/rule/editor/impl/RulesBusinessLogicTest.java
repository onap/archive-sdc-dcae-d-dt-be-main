package org.onap.sdc.dcae.rule.editor.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormatManager;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.rule.editor.validators.MappingRulesValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class RulesBusinessLogicTest {
	private static Gson gson = new GsonBuilder()
			.registerTypeAdapter(BaseAction.class, new ActionDeserializer())
			.registerTypeAdapter(BaseCondition.class, new ConditionDeserializer()).create();

	private String mockUiInput = "mockUiInput";
	private MappingRulesValidator mappingRulesValidator = Mockito.mock(MappingRulesValidator.class);

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
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\"},\"class\":\"Set\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildCopyAction("2.0","event.commonEventHeader.version"));
		rule.setDescription("description");
		rule.setPhase("phase_1");
		MappingRules mr = new MappingRules(rule);
		mr.setEntryPhase("snmp_map");
		mr.setPublishPhase("map_publish");
		List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void translateSingleRuleSingleCopyActionWithNotifyOidFilter() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"foi_map\",\"filter\":{\"string\":\"${notify OID}\",\"value\":\"someValue\",\"class\":\"StartsWith\"},\"processors\":[{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},"
				+ "{\"phase\":\"phase_1\",\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\"},\"class\":\"Set\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildCopyAction("2.0","event.commonEventHeader.version"));
		rule.setDescription("description");
		rule.setPhase("phase_1");
		rule.setNotifyId("someValue");
		rule.setEntryPhase("foi_map");
		rule.setPublishPhase("map_publish");
		MappingRules mr = new MappingRules(rule);
		List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void translateSingleRuleSingleDateFormatterActionSnmpFlow() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"fromFormat\":\"fromFormat\",\"fromTz\":\"fromTZ\",\"toField\":\"targetField\",\"toFormat\":\"toFormat\",\"toTz\":\"toTz\",\"value\":\"fromField\",\"class\":\"DateFormatter\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildDateFormatterAction());
		rule.setDescription("description");
		rule.setPhase("phase_1");
		rule.setNotifyId("");
		rule.setEntryPhase("snmp_map");
		rule.setPublishPhase("map_publish");
		MappingRules mr = new MappingRules(rule);
		List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void translateSingleRuleMultipleCopyActionsAddSnmpHeader() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\","
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void translateMultipleRulesMultipleCopyActionsAddSnmpHeader() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\","
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"}]},{\"phase\":\"phase_1\","
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		mr.addOrReplaceRule(buildRuleWithMultipleCopyActions());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void emptyStringTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"processors\":[{\"map\":{\"\":\"\"},\"field\":\"\",\"toField\":\"mapTargetField\",\"default\":\"\",\"class\":\"MapAlarmValues\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String ruleRequestBody = "{entryPhase:snmp_map,publishPhase:map_publish,phase:phase_1,version:4.1,eventType:syslogFields,description:description,actions:[{actionType:map,from:{value:'\"\"'},target:mapTargetField,map:{values:[{key:'\"\"',value:'\"\"'}],haveDefault:true,default:'\"\"'}}]}";
		Rule myRule = gson.fromJson(ruleRequestBody, Rule.class);
		MappingRules mr = new MappingRules(myRule);
		List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void singleStringConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"syslog_map\",\"processors\":[{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"filter\":{\"string\":\"left\",\"value\":\"right\",\"class\":\"Contains\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:contains,left:left,right:[right]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		mr.setEntryPhase("syslog_map");
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void multiStringConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"foi_map\",\"processors\":[{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},"
				+ "{\"phase\":\"phase_1\",\"filter\":{\"filters\":[{\"string\":\"left\",\"value\":\"right1\",\"class\":\"Contains\"},{\"string\":\"left\",\"value\":\"right2\",\"class\":\"Contains\"}],\"class\":\"Or\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:contains,left:left,right:[right1, right2]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		mr.setEntryPhase("foi_map");
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void singleFieldConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"filter\":{\"field\":\"left\",\"value\":\"right\",\"class\":\"Equals\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:equals,left:left,right:[right]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void multiFieldConditionTranslationTest() throws Exception {
		String expectedTranslation = "{\"processing\":[{\"phase\":\"snmp_map\",\"processors\":[{\"array\":\"varbinds\",\"datacolumn\":\"varbind_value\",\"keycolumn\":\"varbind_oid\",\"class\":\"SnmpConvertor\"},"
				+ "{\"phase\":\"phase_1\",\"class\":\"RunPhase\"}]},{\"phase\":\"phase_1\",\"filter\":{\"field\":\"left\",\"values\":[\"right1\",\"right2\"],\"class\":\"NotOneOf\"},"
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\",\"event.commonEventHeader.eventId\":\"${event.commonEventHeader.sourceName}_${eventGroup}\"},\"class\":\"Set\"},"
				+ "{\"regex\":\"([^:]*):.*\",\"field\":\"targetField\",\"value\":\"extractFromHere\",\"class\":\"ExtractText\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";
		String input = "{operator:notequal,left:left,right:[right1,right2]}";
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		MappingRules mr = new MappingRules(rule);
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(mr));
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
	public void reorderRuleWithConditionalActionDuringValidationSuccessTest() {
		Rule rule1 = buildValidRuleWithDependentSearchAndTransformActions();
		Rule rule2 = buildValidRuleWithDependentSearchAndTransformActions();
		assertEquals(rule1, rule2);
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule1);
		assertTrue(errors.isEmpty());
		assertNotEquals(rule1, rule2);
		//after validation actions are reordered: 5, 2, 4, 1, 3
		rule2.getActions().add(0, rule2.getActions().get(4)); // 1, 2, 3, 4, 5 -> 5, 1, 2, 3, 4, 5
		rule2.getActions().remove(5); // 5, 1, 2, 3, 4, 5 -> 5, 1, 2, 3, 4
		rule2.getActions().add(1, rule2.getActions().get(2)); // 5, 1, 2, 3, 4 -> 5, 2, 1, 2, 3, 4
		rule2.getActions().remove(3); // 5, 2, 1, 2, 3, 4 -> 5, 2, 1, 3, 4
		rule2.getActions().add(2, rule2.getActions().get(4)); // 5, 2, 1, 3, 4 -> 5, 2, 4, 1, 3, 4
		rule2.getActions().remove(5); // 5, 2, 4, 1, 3, 4 -> 5, 2, 4, 1, 3
		assertEquals(rule1, rule2);
	}

	@Test
	public void importMappingRulesAndReorderActionsDuringImportValidationSuccessTest() {
		// as this top level validator uses external ves configuration it is mocked.
		// dependency validations are conducted in the class under test and verified with the control rule
		when(mappingRulesValidator.validate(any(), any())).thenReturn(true);
		Rule importedRule = buildValidRuleWithDependentActions();
		Rule controlRule = buildValidRuleWithDependentActions();
		MappingRules mappingRules = new MappingRules(importedRule);
		// copying the generated uuid to the control rule to sustain equality
        controlRule.setUid(importedRule.getUid());
		assertEquals(importedRule, controlRule);

		List<ServiceException> errors = rulesBusinessLogic.validateImportedRules(mappingRules);
		assertTrue(errors.isEmpty());
		assertNotEquals(importedRule, controlRule);
		//after validation actions are reordered: 1, 3, 4, 2, 5
		controlRule.getActions().add(1, controlRule.getActions().get(2)); // 1, 2, 3, 4, 5 -> 1, 3, 2, 3, 4, 5
		controlRule.getActions().remove(3); // 1, 3, 2, 3, 4, 5 -> 1, 3, 2, 4, 5
		controlRule.getActions().add(2, controlRule.getActions().get(3)); // 1, 3, 2, 4, 5 -> 1, 3, 4, 2, 4, 5
		controlRule.getActions().remove(4); // 1, 3, 4, 2, 4, 5 -> 1, 3, 4, 2, 5
		assertEquals(importedRule, controlRule);
	}

	@Test
	public void supportGroupDefinitionTest() {

		Rule rule = buildRuleWithMultipleCopyActions();
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule);
		assertTrue(errors.isEmpty());
		MappingRules mappingRules = new MappingRules(rule);
		// first rule dictates whether or not user defined phases should be supported (supportGroups = false)
		assertTrue(rulesBusinessLogic.validateGroupDefinitions(mappingRules, false));
		assertFalse(rulesBusinessLogic.validateGroupDefinitions(mappingRules, true));
		// add group definitions (supportGroups = true)
		rule.setGroupId("mapPhaseId");
		errors = rulesBusinessLogic.validateRule(rule);
		assertTrue(errors.isEmpty());
		assertTrue(rulesBusinessLogic.validateGroupDefinitions(mappingRules, true));
		assertFalse(rulesBusinessLogic.validateGroupDefinitions(mappingRules, false));
	}


	@Test
	public void reorderRuleActionsDuringValidationFailureTest() {
		String expectedError = "A circular dependency was detected between actions. The following fields should be resolved: event.commonEventHeader.eventId, event.commonEventHeader.sourceName, invalidSelfDependency, circularDependencyTarget_3";
		Rule rule1 = buildRuleWithCircularActionDependencies();
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule1);
		assertEquals(expectedError, errors.get(0).getFormattedErrorMessage());
	}


	@Test
	public void reorderMappingRulesByDependencyOnlyInSamePhaseSuccessTest() {
		when(mappingRulesValidator.validateTranslationPhaseNames(any(), any())).thenReturn(true);
		when(mappingRulesValidator.validate(any(), any())).thenReturn(true);
		Rule rule1 = buildRuleWithMultipleCopyActions();
		MappingRules mr = new MappingRules(rule1);
		Rule rule2 = new Rule();
		rule2.setDescription("description");
		rule2.setActions(new ArrayList<>());
		// create a dependency between rules
		rule2.getActions().add(buildCopyAction("${event.commonEventHeader.someField}","event.commonEventHeader.sourceName"));
		rule2.setPhase("phase_1");
		mr.addOrReplaceRule(rule2);
		mr.setPublishPhase("map_publish");
		mr.setEntryPhase("snmp_map");
		List<String> ruleUids = new ArrayList<>(mr.getRules().keySet());
		String translateBefore = rulesBusinessLogic.translateRules(mr);
		//separate the rules into two phases, call import validator and translate
		rule1.setGroupId("group_1");
		rule2.setGroupId("group_2");
		List<ServiceException> errors = rulesBusinessLogic.validateImportedRules(mr);
		assertTrue(errors.isEmpty());
		errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		assertEquals(translateBefore, rulesBusinessLogic.translateRules(mr));
		//revert to single phase
		rule1.setGroupId("");
		rule2.setGroupId("");
		errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		List<String> ruleUidsMod = new ArrayList<>(mr.getRules().keySet());
		assertEquals(ruleUids.get(0), ruleUidsMod.get(1));
		assertEquals(ruleUids.get(1), ruleUidsMod.get(0));
		assertNotEquals(translateBefore,  rulesBusinessLogic.translateRules(mr));
	}

	@Test
	public void missingLogTextFailureTest() {
		Rule rule = new Rule();
		rule.setDescription("description");
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildLogTextMissingTextAction());
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule);
		String expectedError = "Please fill the text field of Log Text action to ";
		assertEquals(expectedError, errors.get(0).getFormattedErrorMessage());
	}

	@Test
	public void reorderMappingRulesCircularDependencyFailureTest() {
		when(mappingRulesValidator.validateTranslationPhaseNames(any(), any())).thenReturn(true);
		MappingRules mr = new MappingRules(buildRuleWithMultipleCopyActions());
		List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
		assertTrue(errors.isEmpty());
		Rule rule = new Rule();
		rule.setDescription("description");
		rule.setActions(new ArrayList<>());
		// create a circular dependency between rules
		rule.getActions().add(buildCopyAction("${event.commonEventHeader.version}","event.commonEventHeader.sourceName"));
		String input = "{operator:equals,left:\"${event.commonEventHeader.version}\",right:[\"${event.commonEventHeader.eventId}\"]}";
		rule.setCondition(gson.fromJson(input, BaseCondition.class));
		rule.setPhase("phase_1");
		assertTrue(rulesBusinessLogic.addOrEditRule(mr, rule, false));
		errors = rulesBusinessLogic.validateRulesBeforeTranslate(mr);
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
				+ "\"processors\":[{\"updates\":{\"event.commonEventHeader.version\":\"2.0\"},\"class\":\"Set\"},{\"phase\":\"map_publish\",\"class\":\"RunPhase\"}]}]}";

		Rule rule = new Rule();
		rule.setActions(new ArrayList<>());
		rule.getActions().add(buildCopyAction("2.0","event.commonEventHeader.version"));
		rule.setDescription("description");
		String condition = "{id:123456,level:1,name:elvis,type:All,children:[{id:123456,level:1,name:elvis,operator:equals,left:\"${event.commonEventHeader.version}\",right:[\"${event.commonEventHeader.eventId}\"]},"
				+ "{id:123456,level:1,name:elvis,type:Any,children:[{id:123456,level:1,name:elvis,operator:contains,left:\"${XXX}\",right:[right1,right2]},{id:123456,level:1,name:elvis,operator:notEqual,left:left,right:[right]}]}]}";
		rule.setCondition(gson.fromJson(condition, BaseCondition.class));
		rule.setPublishPhase("map_publish");
		rule.setEntryPhase("foi_map");
		rule.setPhase("phase_1");
		List<ServiceException> errors = rulesBusinessLogic.validateRule(rule);
		assertTrue(errors.isEmpty());
		assertEquals(expectedTranslation, rulesBusinessLogic.translateRules(new MappingRules(rule)));
	}

	private Rule buildRuleWithMultipleCopyActions() {
		Rule rule = new Rule();
		rule.setDescription("description");
		List<BaseAction> actions = new ArrayList<>();
		actions.add(buildCopyAction("2.0","event.commonEventHeader.version"));
		actions.add(buildConcatAction(Arrays.asList("${event.commonEventHeader.sourceName}","_","${eventGroup}"), "event.commonEventHeader.eventId"));
		actions.add(buildRegexAction("extractFromHere", "targetField", "([^:]*):.*"));
		rule.setActions(actions);
		rule.setPhase("phase_1");
		rule.setEntryPhase("snmp_map");
		rule.setPublishPhase("map_publish");
		return rule;
	}

	private Rule buildValidRuleWithDependentActions() {
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.getActions().add(buildConcatAction(Arrays.asList("${targetField}","_","${circularDependencyTarget_3}"), "event.commonEventHeader.sourceName"));
		rule.getActions().add(buildConcatAction(Arrays.asList("${validSelfDependency}","_","${event.commonEventHeader.version}"), "validSelfDependency"));
		return rule;
	}

	private Rule buildValidRuleWithDependentSearchAndTransformActions() {
		Rule rule = buildRuleWithMultipleCopyActions();
		rule.getActions().add(0, buildStringTransformAction());
		rule.getActions().add(0, buildConditionalTopoSearchAction());
		return rule;
	}

	private StringTransformAction buildStringTransformAction() {
		String stringTransform = "{actionType:\"string Transform\",id:76,target:searchString,stringTransform:{targetCase:same,startValue:\"${event.otherFields.otherSiteId}${targetField}${event.commonEventHeader.sourceName}\"}}";
		return gson.fromJson(stringTransform, StringTransformAction.class);
	}

	private TopoSearchAction buildConditionalTopoSearchAction() {
		String topoSearch = "{actionType:\"Topology Search\",id:76,search:{searchField:sourceToSearch,searchValue:\"${searchString}\",radio:'',searchFilter:{left:\"${event.commonEventHeader.eventId}\",right:[rightO],operator:OneOf},enrich:{fields:[{value:e_field1},{value:e_field2}],prefix:e_prefix}}}";
		return gson.fromJson(topoSearch, TopoSearchAction.class);
	}

	private Rule buildRuleWithCircularActionDependencies() {
		Rule rule = buildValidRuleWithDependentActions();
		rule.getActions().add(buildCopyAction("${invalidSelfDependency}", "invalidSelfDependency"));
		rule.getActions().add(buildCopyAction("${event.commonEventHeader.eventId}", "circularDependencyTarget_3"));
		return rule;
	}

	private BaseCopyAction buildCopyAction(String from, String to) {
		BaseCopyAction action = new BaseCopyAction();
		action.setActionType("copy");
		action.setFrom(from);
		action.setTarget(to);
		mockUiGeneratedFields(action);
		return action;
	}

	private LogTextAction buildLogTextMissingTextAction(){
		LogTextAction logTextAction = new LogTextAction();
		logTextAction.setActionType("Log Text");
		logTextAction.setLogText("a name", "a level", "");
		logTextAction.setId(mockUiInput);
		return logTextAction;
	}

	private BaseCopyAction buildConcatAction(List<String> from, String to) {
		BaseCopyAction action = new BaseCopyAction();
		action.setActionType("concat");
		action.setFrom(from);
		action.setTarget(to);
		mockUiGeneratedFields(action);
		return action;
	}

	private BaseCopyAction buildRegexAction(String from, String to, String regex) {
		BaseCopyAction action = new BaseCopyAction();
		action.setActionType("copy");
		action.setFrom(from, regex);
		action.setTarget(to);
		mockUiGeneratedFields(action);
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
		mockUiGeneratedFields(action);
		return action;
	}

	private void mockUiGeneratedFields(UnaryFieldAction action) {
		action.setId(mockUiInput);
		action.regexState(mockUiInput);
	}
}
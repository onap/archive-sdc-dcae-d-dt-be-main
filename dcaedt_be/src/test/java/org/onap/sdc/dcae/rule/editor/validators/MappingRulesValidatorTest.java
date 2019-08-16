/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 Samsung. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ActionDeserializer;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCondition;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ConditionDeserializer;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRules;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Rule;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.ves.VesStructureLoader;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MappingRulesValidatorTest {

    private static final MappingRulesValidator validator = MappingRulesValidator.getInstance();
    private List<ResponseFormat> errors;

    private static final String FORMAT_ERROR_MESSAGE_ID = "SVC6035";
    private static final String FORMAT_ERROR_TEXT = "Error - Rule format is invalid: %1.";
    private static final String FORMAT_ERROR_VARIABLE_TEXT = "no rules found";

    private static final String TRANSLATION_ERROR_MESSAGE_ID = "SVC6116";
    private static final String TRANSLATION_ERROR_TEXT = "Translation failed. Reason: %1";
    private static final String TRANSLATION_ERROR_ENTRY_VARIABLE_TEXT =
        "entry phase name already exists";
    private static final String TRANSLATION_ERROR_PUBLISH_VARIABLE_TEXT =
        "publish phase name already exists";

    private static final String BAD_RULE_VERSION = "1234";
    private static final String RULE_VERSION = "4.1";
    private static final String RULE_EVENT_TYPE = "syslogFields";
    private static final String RULE_GROUP_ID = "4321";
    private static final String RULE_PHASE = "test";

    private static final String BASE_JSON =
        String.format("{version:%s,eventType:%s,notifyId:.1.3.6.1.4.1.26878.200.2}", RULE_VERSION,
            RULE_EVENT_TYPE);

    private static final String RULE_OBJECT_TEMPLATE =
        "{version:%s,eventType:%s,notifyId:.1.3.6.1.4.1.26878.200.2,description:newRule,"
            + "actions:[{from:{state:closed,value:fromField,regex:\"\"},target:event.commonEventHeader.target,id:id,actionType:copy},"
            + "{actionType:concat,id:id1,from:{state:closed,values:[{value:concat1},{value:_concat2}]},target:concatTargetField},"
            + "{actionType:copy,id:id2,from:{state:open,value:extractFromHere,regex:\"([^:]*):.*\"},target:regexTargetField}],"
            + "condition:{id:idc,level:0,name:elvis,left:\"${leftOperand}\",operator:contains,right:[rightOperand1,rightOperand2]},"
            + "groupId: %s, phase:%s, entryPhase:%s, publishPhase:%s}";

    private static final String RULE_JSON = createRuleJSON(null, null, null, null);

    private static final String RULE_WITH_GROUP_JSON =
        createRuleJSON(RULE_GROUP_ID, RULE_PHASE, null, null);

    @BeforeClass
    public static void setup() {
        new ErrorConfigurationLoader(System.getProperty("user.dir") + "/src/main/webapp/WEB-INF");
    }

    @Before
    public void prepare() {
        errors = new ArrayList<>();
    }

    @Test
    public void validateMappingRulesTest() {
        // given
        MappingRules mappingRules = createMappingRules();
        // when - then
        assertTrue(validator.validate(mappingRules, errors));
    }

    @Test
    public void validateMappingRulesWithNoRulesTest() {
        // given
        MappingRules mappingRules = createMappingRulesWithoutRules();
        // when
        boolean result = validator.validate(mappingRules, errors);
        ResponseFormat error = errors.get(0);
        ServiceException exception = error.getRequestError().getServiceException();
        // then
        assertFalse(result);
        assertEquals(400, error.getStatus().intValue());
        assertEquals(FORMAT_ERROR_MESSAGE_ID, exception.getMessageId());
        assertEquals(FORMAT_ERROR_TEXT, exception.getText());
        assertEquals(FORMAT_ERROR_VARIABLE_TEXT, exception.getVariables()[0]);
    }

    @Test
    @PrepareForTest(VesStructureLoader.class)
    public void validateVersionAndTypeTest() {
        // given
        MappingRules mappingRules = createMappingRules();
        // when
        mockAvailableVersionsAndEventTypes(RULE_VERSION);
        boolean shouldValidate = validator.validateVersionAndType(mappingRules);
        mockAvailableVersionsAndEventTypes(BAD_RULE_VERSION);
        boolean shouldNotValidate = validator.validateVersionAndType(mappingRules);
        // then
        assertTrue(shouldValidate);
        assertFalse(shouldNotValidate);
    }

    @Test
    public void validateGroupDefinitionsTest() {
        // given
        MappingRules mappingRules = createMappingRules();
        MappingRules mappingRulesWithGroup = createMappingRulesWithGroup();
        // then
        assertFalse(validator.validateGroupDefinitions(mappingRules));
        assertTrue(validator.validateGroupDefinitions(mappingRulesWithGroup));
    }

    @Test
    public void validateTranslationPhaseNamesTest() {
        // given
        MappingRules mappingRules = createMappingRulesWithGroup();

        Rule rule = createRule(createRuleJSON(RULE_GROUP_ID, RULE_PHASE, RULE_PHASE, null));
        MappingRules mappingRulesWithEntryPhase = new MappingRules(rule);
        rule = createRule(createRuleJSON(RULE_GROUP_ID, RULE_PHASE, null, RULE_PHASE));
        MappingRules mappingRulesWithPublishPhase = new MappingRules(rule);

        // when
        boolean normalRulesValidation =
            validator.validateTranslationPhaseNames(mappingRules, errors);

        boolean rulesWithTheSameEntryPhase =
            validator.validateTranslationPhaseNames(mappingRulesWithEntryPhase, errors);
        ResponseFormat errorEntryPhase = errors.get(0);
        ServiceException exceptionEntryPhase =
            errorEntryPhase.getRequestError().getServiceException();

        boolean rulesWithTheSamePublishPhase =
            validator.validateTranslationPhaseNames(mappingRulesWithPublishPhase, errors);
        ResponseFormat errorPublishPhase = errors.get(1);
        ServiceException exceptionPublishPhase =
            errorPublishPhase.getRequestError().getServiceException();
        // then
        assertTrue(normalRulesValidation);

        assertFalse(rulesWithTheSameEntryPhase);
        assertEquals(400, errorEntryPhase.getStatus().intValue());
        assertEquals(TRANSLATION_ERROR_MESSAGE_ID, exceptionEntryPhase.getMessageId());
        assertEquals(TRANSLATION_ERROR_TEXT, exceptionEntryPhase.getText());
        assertEquals(TRANSLATION_ERROR_ENTRY_VARIABLE_TEXT, exceptionEntryPhase.getVariables()[0]);

        assertFalse(rulesWithTheSamePublishPhase);
        assertEquals(400, errorPublishPhase.getStatus().intValue());
        assertEquals(TRANSLATION_ERROR_MESSAGE_ID, exceptionPublishPhase.getMessageId());
        assertEquals(TRANSLATION_ERROR_TEXT, exceptionPublishPhase.getText());
        assertEquals(TRANSLATION_ERROR_PUBLISH_VARIABLE_TEXT,
            exceptionPublishPhase.getVariables()[0]);
    }

    private void mockAvailableVersionsAndEventTypes(String version) {
        PowerMockito.mockStatic(VesStructureLoader.class);
        Map<String, Set<String>> availableVersionsAndEventTypes = new HashMap<>();
        Set<String> eventTypes = new HashSet<>();
        eventTypes.add(RULE_EVENT_TYPE);
        availableVersionsAndEventTypes.put(version, eventTypes);
        PowerMockito.when(VesStructureLoader.getAvailableVersionsAndEventTypes())
            .thenReturn(availableVersionsAndEventTypes);
    }

    private Rule createRule(String body) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(BaseAction.class, new ActionDeserializer());
        gsonBuilder.registerTypeAdapter(BaseCondition.class, new ConditionDeserializer());
        return gsonBuilder.create().fromJson(body, Rule.class);
    }

    private MappingRules createMappingRulesWithoutRules() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(BaseAction.class, new ActionDeserializer());
        gsonBuilder.registerTypeAdapter(BaseCondition.class, new ConditionDeserializer());
        return gsonBuilder.create().fromJson(BASE_JSON, MappingRules.class);
    }

    private MappingRules createMappingRules() {
        Rule rule = createRule(RULE_JSON);
        return new MappingRules(rule);
    }

    private MappingRules createMappingRulesWithGroup() {
        Rule rule = createRule(RULE_WITH_GROUP_JSON);
        return new MappingRules(rule);
    }

    private static String createRuleJSON(String ruleGroupId, String rulePhase,
        String ruleEntryPhase, String rulePublishPhase) {
        return String.format(RULE_OBJECT_TEMPLATE, RULE_VERSION, RULE_EVENT_TYPE, ruleGroupId,
            rulePhase, ruleEntryPhase, rulePublishPhase);
    }
}

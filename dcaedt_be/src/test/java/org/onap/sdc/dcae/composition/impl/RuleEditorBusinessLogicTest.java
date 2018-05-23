package org.onap.sdc.dcae.composition.impl;

import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.CompositionConfig;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.rule.editor.impl.RulesBusinessLogic;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RuleEditorBusinessLogicTest {

    // DEFAULT PROPERTIES
    private String  justAString = "aStringForAllSeasons";
    private String userId = "gc786h";
    private String vfcmtUuid = "26e8d4b5-f087-4821-a75a-0b9514b5a7ab";
    private String dcaeCompLabel = "dMp.DockerMap";
    private String nId = "n.1525864440166.30";
    private String resourceUuid = "26e8d4b5-f087-4821-a75a-0b9514b5a7ab";
    private String artifactUuid = "9b00ba74-da02-4706-8db0-ac3c11d1d47b";
    private String configParam = "aaf_username";
    private String requestId = "9a89b5c7-33b2-4f7e-a404-66bf4115f510";
    private String flowTypeName = "SNMP MSE";
    private String ruleUuid = "sadsads";
    private String categoryName = "Template";
    private String resourceType = "VFCMT";
    private String saveRulesJsonRequest = "{\n\"version\":\"4.1\",\n\"eventType\":\"syslogFields\",\n\"uid\":\"\",\n\"description\":\"sfasfsaf\",\n\"actions\":[\n{\n\"id\":\"6e0175a0-581f-11e8-82eb-53bb060b790a\",\n\"actionType\":\"copy\",\n\"from\":{\n\"value\":\"asfsf\",\n\"regex\":\"\",\n\"state\":\"closed\",\n\"values\":[\n{\n" + "\"value\":\"\"\n" + "},\n" + "{\n\"value\":\"\"\n}\n]\n},\n\"target\":\"event.commonEventHeader.eventType\",\n\"map\":{\n\"values\":[\n{\n\"key\":\"\",\n\"value\":\"\"\n}\n],\n\"haveDefault\":false,\n\"default\":\"\"\n},\n\"dateFormatter\":{\n\"fromFormat\":\"\",\n\"toFormat\":\"\",\n\"fromTimezone\":\"\",\n\"toTimezone\":\"\"\n},\n\"replaceText\":{\n\"find\":\"\",\n\"replace\":\"\"\n},\n\"logText\":{\n\"name\":\"\",\n\"level\":\"\",\n\"text\":\"\"\n},\n\"logEvent\":{\n\"title\":\"\"\n}\n}\n],\n\"condition\":null\n}";
    private String artifactJson = "{\n \"artifactName\":\"composition.yml\",\n \"artifactType\":\"DCAE_TOSCA\",\n \"artifactURL\":\"/sdc/v1/catalog/resources/c2877686-616a-48ca-a37b-7e311bf83adc/artifacts/9b00ba74-da02-4706-8db0-ac3c11d1d47b\",\n \"artifactDescription\":\"createReferenceArtifact\",\n \"artifactTimeout\":null,\n \"artifactChecksum\":\"MjhhYTAwOTIxZGZkMGMyMmFjYmEzYjI1NTIwYjA3YzM=\",\n \"artifactUUID\":\"9b00ba74-da02-4706-8db0-ac3c11d1d47b\",\n \"artifactVersion\":\"1\",\n \"generatedFromUUID\":null,\n \"artifactLabel\":\"servicereference\",\n \"artifactGroupType\":\"DEPLOYMENT\",\n \"payloadData\":null,\n \"description\":null\n" + "}";
    private String defaultPayload = "{eventType:syslogFields,version:4.1,rules:{'test':{'version':'4.1'}}}";


    // MOCKS
    private ISdcClient sdcClientMock = Mockito.mock(ISdcClient.class);
    private ResourceDetailed vfcmt = Mockito.mock(ResourceDetailed.class);
    private SchemaInfo schemaInfo = Mockito.mock(SchemaInfo.class);
    private CompositionConfig compositionConfig = Mockito.mock(CompositionConfig.class);
    private RulesBusinessLogic rulesBusinessLogic = Mockito.mock(RulesBusinessLogic.class);
    private CompositionConfig.FlowType flowType = Mockito.mock(CompositionConfig.FlowType.class);

    @InjectMocks
    private RuleEditorBusinessLogic ruleEditorBusinessLogic = new RuleEditorBusinessLogic();

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        ruleEditorBusinessLogic.setSdcRestClient(sdcClientMock);

        new ErrorConfigurationLoader(System.getProperty("user.dir") + "/src/main/webapp/WEB-INF");
        when(vfcmt.getUuid()).thenReturn(vfcmtUuid);
        when(vfcmt.getName()).thenReturn(justAString);
        when(vfcmt.getDescription()).thenReturn(justAString);
        when(vfcmt.getResourceType()).thenReturn(resourceType);
        when(vfcmt.getCategory()).thenReturn(categoryName);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResource(anyString(), anyString())).thenReturn(vfcmt);
        when(schemaInfo.getVersion()).thenReturn("0.2");

  /*      PowerMockito.doReturn(vs).when(VesStructureLoader.class);
        when(vs.getEventListenerDefinitionByVersion(anyString())).thenReturn(null);*/
    }

    @Test
    public void test_saveRules() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);
        when(rulesBusinessLogic.addOrEditRule(any(MappingRules.class), any(Rule.class))).thenReturn(true);

        ResponseEntity result = ruleEditorBusinessLogic.saveRule(saveRulesJsonRequest, requestId, userId, vfcmtUuid, dcaeCompLabel, nId, configParam);
        assertEquals(200,result.getStatusCodeValue());
        Assert.assertTrue(result.getBody().toString().contains("6e0175a0-581f-11e8-82eb-53bb060b790a"));
        verify(rulesBusinessLogic,times(1)).addOrEditRule(any(MappingRules.class), any(Rule.class));

    }

    @Test
    public void test_saveRules_artifactNotFound() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, false);

        when(rulesBusinessLogic.addOrEditRule(any(MappingRules.class), any(Rule.class))).thenReturn(true);
        String payload = "{eventType:syslogFields,version:4.1,rules:{'test':{'version':'4.1'}},\"nid\":\"n.1525864440166.30}";
        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(anyString(),anyString(), anyString())).thenReturn(payload);

        ResponseEntity result = ruleEditorBusinessLogic.saveRule(saveRulesJsonRequest, requestId, userId, vfcmtUuid, dcaeCompLabel, nId, configParam);
        assertEquals(200,result.getStatusCodeValue());
        Assert.assertTrue(result.getBody().toString().contains("6e0175a0-581f-11e8-82eb-53bb060b790a"));
        verify(rulesBusinessLogic,times(0)).addOrEditRule(any(MappingRules.class), any(Rule.class));

    }

    @Test
    public void test_saveRules_artifactNotFound_Error() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, false);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(anyString(),anyString(), anyString())).thenReturn(defaultPayload);

        ResponseEntity<ResponseFormat> result = ruleEditorBusinessLogic.saveRule(saveRulesJsonRequest, requestId, userId, vfcmtUuid, dcaeCompLabel, nId, configParam);
        assertEquals(400,result.getStatusCodeValue());
        assertEquals("SVC6114",result.getBody().getRequestError().getServiceException().getMessageId());
        assertEquals("DCAE component %1 not found in composition",result.getBody().getRequestError().getServiceException().getText());
        verify(rulesBusinessLogic,times(0)).addOrEditRule(any(MappingRules.class), any(Rule.class));

    }

    @Test
    public void test_getRules() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(resourceUuid, artifactUuid, requestId))
                .thenReturn(defaultPayload);

        ResponseEntity result = ruleEditorBusinessLogic.getRules(vfcmtUuid, dcaeCompLabel, nId, configParam, requestId);
        assertEquals(200,result.getStatusCodeValue());
        Assert.assertTrue(result.getBody().toString().contains("eventType:syslogFields,version:4.1,rules:{'test':{'version':'4.1'"));

    }

    @Test
    public void test_getExistingRuleTargets() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);

        ResponseEntity result = ruleEditorBusinessLogic.getExistingRuleTargets(vfcmtUuid, requestId, dcaeCompLabel, nId);
        assertEquals(200,result.getStatusCodeValue());
        assertNotEquals(null,result.getBody());

    }

    @Test
    public void test_translate() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);
        Map<String, CompositionConfig.FlowType> flowTypeMap = new HashMap<>();
        flowTypeMap.put("SNMP MSE", flowType);
        when(compositionConfig.getFlowTypesMap()).thenReturn(flowTypeMap);
        when(compositionConfig.getFlowTypesMap().get("SNMP MSE").getEntryPointPhaseName()).thenReturn("testName");
        when(compositionConfig.getFlowTypesMap().get("SNMP MSE").getLastPhaseName()).thenReturn("testLastName");

        when(rulesBusinessLogic.translateRules(any(MappingRules.class), anyString(), anyString(), anyString())).thenReturn("testLastName");
        ResponseEntity result = ruleEditorBusinessLogic.translateRules(vfcmtUuid, requestId, dcaeCompLabel, nId, configParam, flowTypeName);
        verify(compositionConfig,times(6)).getFlowTypesMap();
        verify(rulesBusinessLogic,times(1)).translateRules(any(MappingRules.class), anyString(), anyString(), anyString());

        assertEquals(200,result.getStatusCodeValue());

    }

    @Test
    public void test_deleteRule() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);

        when(rulesBusinessLogic.deleteRule(any(MappingRules.class), anyString())).thenReturn(new Rule());
        ResponseEntity result = ruleEditorBusinessLogic.deleteRule(userId, vfcmtUuid, dcaeCompLabel, nId, configParam, ruleUuid, requestId);
        assertEquals(200,result.getStatusCodeValue());

    }

    @Test
    public void test_getDefinition(){


/*
        PowerMockito.mockStatic(VesStructureLoader.class);
        when(VesStructureLoader.getEventListenerDefinitionByVersion(anyString())).thenReturn(null);
*/

        ResponseEntity res = ruleEditorBusinessLogic.getDefinition("4.1","syslogFields");

    }

    private void emulateMockListOfArtifacts(String dcaeCompLabel, String nid, String configParam, boolean isApprovedArtifact) {
        List<Artifact> listOfArtifactCompositionYml = new ArrayList<>();
        Artifact artifact =  Mockito.mock(Artifact.class);//gson.fromJson(artifactJson, Artifact.class);

        if (isApprovedArtifact) {
            when(artifact.getArtifactLabel()).thenReturn(dcaeCompLabel + nid + configParam);
            when(artifact.getArtifactName()).thenReturn(dcaeCompLabel + "_" + nid + "_" + DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX);
        }else {
            when(artifact.getArtifactLabel()).thenReturn("servicereference");
            when(artifact.getArtifactName()).thenReturn("composition.yml");
        }
        when(artifact.getArtifactType()).thenReturn("DCAE_TOSCA");
        when(artifact.getArtifactUUID()).thenReturn(artifactUuid);
        when(artifact.getArtifactDescription()).thenReturn("createmapArtifact");

        listOfArtifactCompositionYml.add(artifact);
        when(vfcmt.getArtifacts()).thenReturn(listOfArtifactCompositionYml);
    }


}

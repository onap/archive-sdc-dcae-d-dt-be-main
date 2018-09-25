package org.onap.sdc.dcae.composition.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.catalog.asdc.ASDCException;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRules;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRulesResponse;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.Rule;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.SchemaInfo;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.RequestError;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.rule.editor.impl.RulesBusinessLogic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RuleEditorBusinessLogicTest {

    // DEFAULT PROPERTIES
    private String justAString = "aStringForAllSeasons";
    private String userId = "gc786h";
    private String vfcmtUuid = "26e8d4b5-f087-4821-a75a-0b9514b5a7ab";
    private String dcaeCompLabel = "dMp.DockerMap";
    private String nId = "n.1525864440166.30";
    private String resourceUuid = "26e8d4b5-f087-4821-a75a-0b9514b5a7ab";
    private String artifactUuid = "9b00ba74-da02-4706-8db0-ac3c11d1d47b";
    private String configParam = "aaf_username";
    private String requestId = "9a89b5c7-33b2-4f7e-a404-66bf4115f510";
    private String ruleUuid = "sadsads";
    private String categoryName = "Template";
    private String resourceType = "VFCMT";
    private String saveRulesJsonRequest = "{\n\"version\":\"4.1\",\n\"eventType\":\"syslogFields\",\n\"uid\":\"\",\n\"description\":\"sfasfsaf\",\n\"actions\":[\n{\n\"id\":\"6e0175a0-581f-11e8-82eb-53bb060b790a\",\n\"actionType\":\"copy\",\n\"from\":{\n\"value\":\"asfsf\",\n\"regex\":\"\",\n\"state\":\"closed\",\n\"values\":[\n{\n" + "\"value\":\"\"\n" + "},\n" + "{\n\"value\":\"\"\n}\n]\n},\n\"target\":\"event.commonEventHeader.eventType\",\n\"map\":{\n\"values\":[\n{\n\"key\":\"\",\n\"value\":\"\"\n}\n],\n\"haveDefault\":false,\n\"default\":\"\"\n},\n\"dateFormatter\":{\n\"fromFormat\":\"\",\n\"toFormat\":\"\",\n\"fromTimezone\":\"\",\n\"toTimezone\":\"\"\n},\n\"replaceText\":{\n\"find\":\"\",\n\"replace\":\"\"\n},\n\"logText\":{\n\"name\":\"\",\n\"level\":\"\",\n\"text\":\"\"\n},\n\"logEvent\":{\n\"title\":\"\"\n}\n}\n],\n\"condition\":null\n}";
    private String defaultPayload = "{eventType:syslogFields,version:4.1,rules:{'test':{'version':'4.1'}}}";


    // MOCKS
    private ISdcClient sdcClientMock = Mockito.mock(ISdcClient.class);
    private ResourceDetailed vfcmt = Mockito.mock(ResourceDetailed.class);
    private SchemaInfo schemaInfo = Mockito.mock(SchemaInfo.class);
    private RulesBusinessLogic rulesBusinessLogic = Mockito.mock(RulesBusinessLogic.class);

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

        when(sdcClientMock.getResource(anyString(), anyString())).thenReturn(vfcmt);
        when(schemaInfo.getVersion()).thenReturn("0.2");

    }

    @Test
    public void test_saveRules() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(sdcClientMock.getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);
        when(rulesBusinessLogic.addOrEditRule(any(MappingRules.class), any(Rule.class), anyBoolean())).thenReturn(true);
        when(rulesBusinessLogic.validateGroupDefinitions(any(MappingRules.class), anyBoolean())).thenReturn(true);

        ResponseEntity result = ruleEditorBusinessLogic.saveRule(saveRulesJsonRequest, requestId, userId, vfcmtUuid, dcaeCompLabel, nId, configParam);
        assertEquals(200,result.getStatusCodeValue());
        assertTrue(result.getBody().toString().contains("6e0175a0-581f-11e8-82eb-53bb060b790a"));
        verify(rulesBusinessLogic,times(1)).addOrEditRule(any(MappingRules.class), any(Rule.class), anyBoolean());

    }

	@Test
	public void test_exportRules_resourceNotFound() throws Exception {
		RequestError requestError = new RequestError();
		requestError.setServiceException(new ServiceException("SVC4063", "", null));
		when(sdcClientMock.getResource(resourceUuid, requestId)).thenThrow(new ASDCException(HttpStatus.NOT_FOUND, requestError));

		ResponseEntity result = ruleEditorBusinessLogic.downloadRules(vfcmtUuid, dcaeCompLabel, nId, configParam, requestId);
		assertEquals(404,result.getStatusCodeValue());
	}


	@Test
	public void incompatibleEditorVersionFailureTest() throws Exception {
    	Rule rule = new Rule();
    	rule.setGroupId("id_1");
		MappingRules rules = new MappingRules(rule);
		assertFalse(ruleEditorBusinessLogic.validateEditorVersion(rules, false));
	}

    @Test
    public void test_saveRules_artifactNotFound() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, false);

        when(rulesBusinessLogic.addOrEditRule(any(MappingRules.class), any(Rule.class), anyBoolean())).thenReturn(true);
        String payload = "{eventType:syslogFields,version:4.1,rules:{'test':{'version':'4.1'}},\"nid\":\"n.1525864440166.30}";
        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(anyString(),anyString(), anyString())).thenReturn(payload);

        ResponseEntity result = ruleEditorBusinessLogic.saveRule(saveRulesJsonRequest, requestId, userId, vfcmtUuid, dcaeCompLabel, nId, configParam);
        assertEquals(200,result.getStatusCodeValue());
        assertTrue(result.getBody().toString().contains("6e0175a0-581f-11e8-82eb-53bb060b790a"));
        verify(rulesBusinessLogic,times(0)).addOrEditRule(any(MappingRules.class), any(Rule.class), anyBoolean());

    }

    @Test
    public void test_saveRules_artifactNotFound_Error() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, false);

        when(ruleEditorBusinessLogic.getSdcRestClient().getResourceArtifact(anyString(),anyString(), anyString())).thenReturn(defaultPayload);

        ResponseEntity<ResponseFormat> result = ruleEditorBusinessLogic.saveRule(saveRulesJsonRequest, requestId, userId, vfcmtUuid, dcaeCompLabel, nId, configParam);
        assertEquals(400,result.getStatusCodeValue());
        assertEquals("SVC6114",result.getBody().getRequestError().getServiceException().getMessageId());
        assertEquals("DCAE component %1 not found in composition",result.getBody().getRequestError().getServiceException().getText());
        verify(rulesBusinessLogic,times(0)).addOrEditRule(any(MappingRules.class), any(Rule.class), anyBoolean());

    }

    @Test
    public void test_getRules() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        ResponseEntity result = ruleEditorBusinessLogic.getRulesAndSchema(vfcmtUuid, dcaeCompLabel, nId, configParam, requestId);
        assertEquals(200,result.getStatusCodeValue());
        assertTrue(result.getBody().toString().contains("eventType:syslogFields,version:4.1,rules:{'test':{'version':'4.1'"));

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
    public void test_deleteRule() throws Exception {

        emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);

        when(sdcClientMock.getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);

        when(rulesBusinessLogic.deleteRule(any(MappingRules.class), anyString())).thenReturn(new Rule());
        ResponseEntity result = ruleEditorBusinessLogic.deleteRule(userId, vfcmtUuid, dcaeCompLabel, nId, configParam, ruleUuid, requestId);
        assertEquals(200,result.getStatusCodeValue());

    }

	@Test
	public void test_deleteGroupOfRules() throws Exception {

		emulateMockListOfArtifacts(dcaeCompLabel, nId, configParam, true);
		when(sdcClientMock.getResourceArtifact(resourceUuid, artifactUuid, requestId)).thenReturn(defaultPayload);
		when(rulesBusinessLogic.deleteGroupOfRules(any(MappingRules.class), anyString())).thenReturn(Collections.singletonList(new Rule()));
		ResponseEntity result = ruleEditorBusinessLogic.deleteGroupOfRules(userId, vfcmtUuid, dcaeCompLabel, nId, configParam, ruleUuid, requestId);
		assertEquals(200,result.getStatusCodeValue());
	}

	private static final String BODY_JSON = "{\n" +
            "  \"version\": \"4.1\",\n" +
            "  \"eventType\": \"syslogFields\",\n" +
            "  \"filter\": {\n" +
            "    \"name\": \"condition\",\n" +
            "    \"left\": \"a\",\n" +
            "    \"right\": [\n" +
            "      \"b\"\n" +
            "    ],\n" +
            "    \"operator\": \"contains\",\n" +
            "    \"level\": 1,\n" +
            "    \"emptyIsAssigned\": false,\n" +
            "    \"id\": 6439610383188\n" +
            "  },\n" +
            "  \"vfcmtUuid\": \"6fcb27c8-3e87-4a04-b032-4de70fbd9d03\",\n" +
            "  \"dcaeCompLabel\": \"Highlandpark_18.06.006\",\n" +
            "  \"nid\": \"n.1532849382756.2\",\n" +
            "  \"configParam\": \"centralOrEdge\"\n" +
            "}\n";

    @Test
    public void test_applyFilter_missingCompLabel() throws Exception {
        String json = BODY_JSON.replaceAll("Highlandpark_18.06.006", "");

        ResponseEntity result = ruleEditorBusinessLogic.applyFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_applyFilter_missingNid() throws Exception {
        String json = BODY_JSON.replaceAll("n.1532849382756.2", "");

        ResponseEntity result = ruleEditorBusinessLogic.applyFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_applyFilter_missingVfcmtUuid() throws Exception {
        String json = BODY_JSON.replaceAll("6fcb27c8-3e87-4a04-b032-4de70fbd9d03", "");

        ResponseEntity result = ruleEditorBusinessLogic.applyFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_applyFilter_missingConfigParam() throws Exception {
        String json = BODY_JSON.replaceAll("centralOrEdge", "");

        ResponseEntity result = ruleEditorBusinessLogic.applyFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_deleteFilter_missingCompLabel() throws Exception {
        String json = BODY_JSON.replaceAll("Highlandpark_18.06.006", "");

        ResponseEntity result = ruleEditorBusinessLogic.deleteFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_deleteFilter_missingNid() throws Exception {
        String json = BODY_JSON.replaceAll("n.1532849382756.2", "");

        ResponseEntity result = ruleEditorBusinessLogic.deleteFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_deleteFilter_missingVfcmtUuid() throws Exception {
        String json = BODY_JSON.replaceAll("6fcb27c8-3e87-4a04-b032-4de70fbd9d03", "");

        ResponseEntity result = ruleEditorBusinessLogic.deleteFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_deleteFilter_missingConfigParam() throws Exception {
        String json = BODY_JSON.replaceAll("centralOrEdge", "");

        ResponseEntity result = ruleEditorBusinessLogic.deleteFilter(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_importPhase_missingCompLabel() throws Exception {
        String json = BODY_JSON.replaceAll("Highlandpark_18.06.006", "");

        ResponseEntity result = ruleEditorBusinessLogic.importPhase(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_importPhase_missingNid() throws Exception {
        String json = BODY_JSON.replaceAll("n.1532849382756.2", "");

        ResponseEntity result = ruleEditorBusinessLogic.importPhase(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_importPhase_missingVfcmtUuid() throws Exception {
        String json = BODY_JSON.replaceAll("6fcb27c8-3e87-4a04-b032-4de70fbd9d03", "");

        ResponseEntity result = ruleEditorBusinessLogic.importPhase(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
    }

    @Test
    public void test_importPhase_missingConfigParam() throws Exception {
        String json = BODY_JSON.replaceAll("centralOrEdge", "");

        ResponseEntity result = ruleEditorBusinessLogic.importPhase(json, requestId, userId);
        assertEquals(404,result.getStatusCodeValue());
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
        when(artifact.getPayloadData()).thenReturn(defaultPayload);

        listOfArtifactCompositionYml.add(artifact);
        when(vfcmt.getArtifacts()).thenReturn(listOfArtifactCompositionYml);
    }

}

package org.onap.sdc.dcae.composition.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.catalog.asdc.ASDCException;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.ImportVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ExternalReferencesMap;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.PolicyException;
import org.onap.sdc.dcae.errormng.RequestError;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum.CERTIFIED;
import static org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT;

public class VfcmtBusinessLogicTest {

	private ISdcClient sdcClientMock = Mockito.mock(ISdcClient.class);
	private ResourceDetailed templateMC = Mockito.mock(ResourceDetailed.class);

	private VfcmtBusinessLogic vfcmtBusinessLogic = new VfcmtBusinessLogic();
	private ImportVFCMTRequest request = new ImportVFCMTRequest();

	private String userId = "me";
	private String requestId = "1";

	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
		new ErrorConfigurationLoader(System.getProperty("user.dir")+"/src/main/webapp/WEB-INF");
		vfcmtBusinessLogic.setSdcRestClient(sdcClientMock);
		request.setTemplateUuid("577");
		request.setVfiName("vfi_XX");
		request.setDescription("description");
		request.setFlowType("SNMP");
		request.setName("newVfcmt");
		request.setServiceUuid("service99999");
		request.setContextType("services");
	}

	@Test
	public void sdcIsDown_creatingVfcmt_gotResponseWithError500() {
		RequestError requestError = new RequestError();
		requestError.setPolicyException(new PolicyException("POL5000", "Error: Internal Server Error. Please try again later.", null));
		when(sdcClientMock.createResource(userId,request,requestId)).thenThrow(new ASDCException(HttpStatus.INTERNAL_SERVER_ERROR, requestError));

		ResponseEntity res = vfcmtBusinessLogic.createMcFromTemplate(userId,request,requestId);
		verify(sdcClientMock).getResource("577",requestId);
		verify(sdcClientMock,times(0)).getResourceArtifact(anyString(),anyString(),anyString());
		Assert.assertEquals(500, res.getStatusCodeValue());
	}

	@Test
	public void uploadCloneCdumpFailed_creatingVfcmt_createVfcmtRolledBack() throws Exception {
		RequestError requestError = new RequestError();
		requestError.setPolicyException(new PolicyException("POL5000", "Error: Internal Server Error. Please try again later.", null));
		when(sdcClientMock.createResourceArtifact(anyString(),anyString(),any(),anyString())).thenThrow(new ASDCException(HttpStatus.INTERNAL_SERVER_ERROR, requestError));
		when(sdcClientMock.createResource(userId,request,requestId)).thenReturn(templateMC);
		when(templateMC.getUuid()).thenReturn("3");
        when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
        emulateListOfArtifactsWithCompositionYml();

        vfcmtBusinessLogic.createMcFromTemplate(userId, request, requestId);

		// making sure rollback is performed if exception is thrown
		verify(sdcClientMock).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}

	// happy happy joy joy
	@Test
	public void successfulCreationAndAttachmentOfVfcmt() throws Exception {
		when(templateMC.getUuid()).thenReturn("3");
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		ResourceDetailed mockedVfcmt = Mockito.mock(ResourceDetailed.class);
		when(mockedVfcmt.getUuid()).thenReturn("5");
		when(sdcClientMock.createResource(anyString(),any(),anyString())).thenReturn(mockedVfcmt);
		when(sdcClientMock.getResourceArtifact(anyString(),anyString(),anyString())).thenReturn("3243324");

		emulateListOfArtifactsWithCompositionYml();

		vfcmtBusinessLogic.createMcFromTemplate(userId, request,requestId);

		verify(sdcClientMock).createResource(userId, request,requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock).getResourceArtifact(anyString(),anyString(),anyString());
		verify(sdcClientMock, times(2)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock).addExternalMonitoringReference(anyString(),any(),any(),anyString());
		verify(sdcClientMock).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}

	@Test
	public void successfulImportAndAttachmentOfVfcmtAlreadyConnectedWithoutEditDoCheckin() throws Exception {
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		when(templateMC.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKOUT");
		emulateListOfArtifactsWithCompositionYmlAndSvcRef();
		request.setCloneVFCMT(false);
		request.setUpdateFlowType(false);
		vfcmtBusinessLogic.importMC(userId, request, requestId);

		verify(sdcClientMock, times(0)).createResource(userId, request, requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock).getResourceArtifact(anyString(),anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock, times(0)).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock).addExternalMonitoringReference(anyString(),any(),any(),anyString());
		verify(sdcClientMock).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}


	@Test
	public void successfulImportAndAttachmentOfVfcmtAlreadyConnectedUpdateFlowTypeCheckoutCheckin() throws Exception {
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		when(templateMC.getUuid()).thenReturn("3");
		when(sdcClientMock.changeResourceLifecycleState(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(templateMC);
		when(sdcClientMock.updateResourceArtifact(anyString(), anyString(), any(), anyString())).thenReturn(new Artifact());
		when(templateMC.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKIN").thenReturn("NOT_CERTIFIED_CHECKOUT");
		emulateListOfArtifactsWithCompositionYmlAndSvcRef();
		request.setCloneVFCMT(false);
		request.setUpdateFlowType(true);
		vfcmtBusinessLogic.importMC(userId, request, requestId);

		verify(sdcClientMock, times(0)).createResource(userId, request, requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock).getResourceArtifact(anyString(),anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock, times(1)).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock).addExternalMonitoringReference(anyString(),any(),any(),anyString());
		verify(sdcClientMock, times(2)).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}


	@Test
	public void invalidateMCRequestFields_returnError() {
		ResponseEntity response = vfcmtBusinessLogic.importMC(userId, new ImportVFCMTRequest(), requestId);
		Assert.assertEquals(response.getStatusCodeValue(), 400);
	}

    @Test
    public void cloneVfcmt_missingToscaFile_returnError() {
        when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
        request.setCloneVFCMT(true);
        ResponseEntity response = vfcmtBusinessLogic.importMC(userId, request, requestId);
        Assert.assertEquals(response.getStatusCodeValue(), 404);
    }

    @Test
    public void checkCatchingSdcExceptions_returnError() {
	    RequestError requestError = new RequestError();
        requestError.setPolicyException(new PolicyException("POL5000", "Error: Internal Server Error. Please try again later.", null));
        when(sdcClientMock.getResource(request.getTemplateUuid(), requestId)).thenThrow(new ASDCException(HttpStatus.INTERNAL_SERVER_ERROR, requestError));
        request.setCloneVFCMT(false);
        request.setUpdateFlowType(true);
        ResponseEntity response = vfcmtBusinessLogic.importMC(userId, request, requestId);
        Assert.assertEquals(response.getStatusCodeValue(), 500);
    }


	@Test
	public void successfulFetchVfcmtDataFull() {
		String templateUuid = "3";
		when(templateMC.getUuid()).thenReturn(templateUuid);
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		emulateListOfArtifactsWithCompositionYmlAndSvcRef();
		when(sdcClientMock.getResourceArtifact(templateUuid, "svcRefArtifactUuid", requestId)).thenReturn("thisIsTheServiceId/resources/thisIsTheVfiName");
		ResponseEntity<VfcmtData> result = vfcmtBusinessLogic.getVfcmtReferenceData(templateUuid, requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock,times(2)).getResourceArtifact(anyString(),anyString(),anyString());
		Assert.assertEquals(200, result.getStatusCodeValue());
		Assert.assertEquals("don't override", result.getBody().getFlowType());
		Assert.assertEquals("thisIsTheServiceId", result.getBody().getServiceUuid());
		Assert.assertEquals("thisIsTheVfiName", result.getBody().getVfiName());
	}

	@Test
	public void successfulFetchVfcmtDataPartial() {
		String templateUuid = "3";
		when(templateMC.getUuid()).thenReturn(templateUuid);
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		emulateListOfArtifactsWithCompositionYml();
		ResponseEntity<VfcmtData> result = vfcmtBusinessLogic.getVfcmtReferenceData(templateUuid, requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock,times(1)).getResourceArtifact(anyString(),anyString(),anyString());
		Assert.assertEquals(200, result.getStatusCodeValue());
		Assert.assertEquals("don't override", result.getBody().getFlowType());
		Assert.assertEquals(null, result.getBody().getServiceUuid());
		Assert.assertEquals(null, result.getBody().getVfiName());
	}

	@Test
	public void successfulFetchVfcmtDataEmpty() {

		String templateUuid = "3";
		when(templateMC.getUuid()).thenReturn(templateUuid);
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		emulateCdumpArtifactWithoutFlowtype();
		ResponseEntity<VfcmtData> result = vfcmtBusinessLogic.getVfcmtReferenceData(templateUuid, requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock,times(1)).getResourceArtifact(anyString(),anyString(),anyString());
		Assert.assertEquals(200, result.getStatusCodeValue());
		Assert.assertEquals(null, result.getBody().getFlowType());
		Assert.assertEquals(null, result.getBody().getServiceUuid());
		Assert.assertEquals(null, result.getBody().getVfiName());
	}

	@Test
	public void fetchVfcmtDataNoCompositionFound() {

		String templateUuid = "3";
		when(templateMC.getUuid()).thenReturn(templateUuid);
		when(templateMC.getName()).thenReturn(templateUuid);
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(templateMC);
		ResponseEntity<ResponseFormat> result = vfcmtBusinessLogic.getVfcmtReferenceData(templateUuid, requestId);
		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock,times(0)).getResourceArtifact(anyString(),anyString(),anyString());
		Assert.assertEquals(404, result.getStatusCodeValue());
		Assert.assertEquals("Error – Could not read component 3 details.", result.getBody().getRequestError().getServiceException().getFormattedErrorMessage());

	}

	@Test
	public void getVfcmtsForMigration() {
		ExternalReferencesMap connectedVfcmts = new ExternalReferencesMap();
		connectedVfcmts.put("11",Arrays.asList("Red", "Blue", "Yellow"));
		connectedVfcmts.put("22",Arrays.asList("Ibiza", "Bora Bora", "Mykonos"));
		connectedVfcmts.put("33",Arrays.asList("Large", "Medium", "Small"));
		connectedVfcmts.put("44",Arrays.asList("Basket", "Foot", "Volley"));

		when(sdcClientMock.getMonitoringReferences(anyString(),anyString(),anyString(),anyString())).thenReturn(connectedVfcmts);

		Resource myRedResource = new Resource();
		myRedResource.setUuid("Red");
		myRedResource.setLastUpdaterUserId("me");
		myRedResource.setLifecycleState(NOT_CERTIFIED_CHECKOUT.name());

		Resource herRaphaelResource = new Resource();
		herRaphaelResource.setUuid("Raphael");
		herRaphaelResource.setLastUpdaterUserId("her");
		herRaphaelResource.setLifecycleState(NOT_CERTIFIED_CHECKOUT.name());

		Resource myMediumResource = new Resource();
		myMediumResource.setUuid("Medium");
		myMediumResource.setLastUpdaterUserId("me");

		Resource herDonateloResource = new Resource();
		herDonateloResource.setUuid("Donatelo");
		herDonateloResource.setLastUpdaterUserId("her");
		herDonateloResource.setVersion("1.0");

		Resource hisMykonosResource = new Resource();
		hisMykonosResource.setUuid("Mykonos");
		hisMykonosResource.setLastUpdaterUserId("his");
		hisMykonosResource.setLifecycleState(NOT_CERTIFIED_CHECKOUT.name());

		Resource hisMichaelangeloResource = new Resource();
		hisMichaelangeloResource.setUuid("Michaelangelo");
		hisMichaelangeloResource.setLastUpdaterUserId("his");
		hisMykonosResource.setLifecycleState(CERTIFIED.name());
		hisMykonosResource.setVersion("1.1");

        // Versions and connectivity to service shouldn't be part of this test as these are passed to SDC to be
        // filtered by SDC requests (getMonitoringReference and getResource)

		List<Resource> theVfcmts = Arrays.asList(myRedResource,herRaphaelResource,myMediumResource,herDonateloResource,hisMykonosResource,hisMichaelangeloResource);

		when(sdcClientMock.getResources(anyString(),anyString(),anyString(),anyString())).thenReturn(theVfcmts);

		ResponseEntity<List<Resource>> response = vfcmtBusinessLogic.getVfcmtsForMigration(userId,"service","5544","1.0",requestId);

		Assert.assertEquals(2, response.getBody().size());
		Assert.assertEquals(200, response.getStatusCodeValue());
	}

	private void emulateListOfArtifactsWithCompositionYml() {
		List<Artifact> listOfArtifactCompositionYml = new ArrayList<>();
		Artifact compositionArtifact = Mockito.mock(Artifact.class);
		when(compositionArtifact.getArtifactName()).thenReturn(DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
		when(compositionArtifact.getArtifactUUID()).thenReturn("compositionArtifactUuid");
		when(compositionArtifact.getPayloadData()).thenReturn("{\"flowType\":\"don't override\"}");
		listOfArtifactCompositionYml.add(compositionArtifact);
		when(templateMC.getArtifacts()).thenReturn(listOfArtifactCompositionYml);
	}


	private void emulateCdumpArtifactWithoutFlowtype() {
		List<Artifact> listOfArtifactCompositionYml = new ArrayList<>();
		Artifact compositionArtifact = Mockito.mock(Artifact.class);
		when(compositionArtifact.getArtifactName()).thenReturn(DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
		when(compositionArtifact.getArtifactUUID()).thenReturn("compositionArtifactUuid");
		when(compositionArtifact.getPayloadData()).thenReturn("{\"cid\":\"xsssdaerrwr\"}\"");
		listOfArtifactCompositionYml.add(compositionArtifact);
		when(templateMC.getArtifacts()).thenReturn(listOfArtifactCompositionYml);
	}

	private void emulateListOfArtifactsWithCompositionYmlAndSvcRef() {
		List<Artifact> listOfArtifactCompositionYml = new ArrayList<>();
		Artifact compositionArtifact = Mockito.mock(Artifact.class);
		Artifact svcRefArtifact = Mockito.mock(Artifact.class);
		when(compositionArtifact.getArtifactName()).thenReturn(DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
		when(compositionArtifact.getArtifactUUID()).thenReturn("compositionArtifactUuid");
		when(compositionArtifact.getPayloadData()).thenReturn("{\"flowType\":\"don't override\"}");
		when(svcRefArtifact.getArtifactName()).thenReturn(DcaeBeConstants.Composition.fileNames.SVC_REF);
		when(svcRefArtifact.getArtifactUUID()).thenReturn("svcRefArtifactUuid");
		listOfArtifactCompositionYml.add(compositionArtifact);
		listOfArtifactCompositionYml.add(svcRefArtifact);
		when(templateMC.getArtifacts()).thenReturn(listOfArtifactCompositionYml);
	}

	@Test
	public void uiHasABug_creatingVfcmtWithBadRequestNoServiceUuid_gotResponseWithError400() {
		RequestError requestError = new RequestError();
		requestError.setPolicyException(new PolicyException("POL5000", "Error: Internal Server Error. Please try again later.", null));
		when(sdcClientMock.createResource(userId,request,requestId)).thenThrow(new ASDCException(HttpStatus.INTERNAL_SERVER_ERROR, requestError));
		CreateVFCMTRequest req = new CreateVFCMTRequest();
		req.setServiceUuid(null);
		ResponseEntity res = vfcmtBusinessLogic.createMcFromTemplate(userId,req,requestId);
		verify(sdcClientMock,times(0)).getResource(anyString(),anyString());
		verify(sdcClientMock,times(0)).getResourceArtifact(anyString(),anyString(),anyString());
		Assert.assertEquals(400, res.getStatusCodeValue());
	}
}
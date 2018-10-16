package org.onap.sdc.dcae.composition.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.catalog.asdc.ASDCException;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.RequestError;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.utils.Normalizers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CompositionBusinessLogicTest {

	private ISdcClient sdcClientMock = Mockito.mock(ISdcClient.class);
	private ResourceDetailed vfcmt = Mockito.mock(ResourceDetailed.class);
	private ServiceDetailed service = Mockito.mock(ServiceDetailed.class);
	private CompositionBusinessLogic compositionBusinessLogic = new CompositionBusinessLogic();
	private String justAString = "aStringForAllSeasons";

	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
		compositionBusinessLogic.setSdcRestClient(sdcClientMock);
		new ErrorConfigurationLoader(System.getProperty("user.dir")+"/src/main/webapp/WEB-INF");
		when(vfcmt.getUuid()).thenReturn(justAString);
		when(vfcmt.getName()).thenReturn(justAString);
		when(vfcmt.getDescription()).thenReturn(justAString);
		when(vfcmt.getInvariantUUID()).thenReturn(justAString);
	}

	@Test
	public void successfulSaveCompositionAndCheckInInitialStateCheckedOut() throws Exception {
		emulateListOfArtifactsWithCompositionYml();
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(vfcmt);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKOUT");

		compositionBusinessLogic.saveComposition(justAString, justAString, justAString, justAString, false);

		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}

	@Test
	public void successfulSaveCompositionAndCheckInInitialStateCheckedIn() throws Exception {
		emulateListOfArtifactsWithCompositionYml();
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(vfcmt);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKIN");
		when(sdcClientMock.changeResourceLifecycleState(anyString(), any(), anyString(), any(), anyString())).thenReturn(vfcmt);

		compositionBusinessLogic.saveComposition(justAString, justAString, justAString, justAString, false);

		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock, times(2)).changeResourceLifecycleState(anyString(),any(),anyString(),any(),anyString());
	}

	@Test
	public void saveCompositionCheckInFailureDoRollback() throws Exception {

		emulateListOfArtifactsWithCompositionYml();
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(vfcmt);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKIN");
		RequestError requestError = new RequestError();
		requestError.setServiceException(new ServiceException("SVC4086", "", null));
		when(sdcClientMock.changeResourceLifecycleState(anyString(), any(), anyString(), any(), anyString())).thenReturn(vfcmt).thenThrow(new ASDCException(HttpStatus.FORBIDDEN, requestError));

		ResponseEntity result = compositionBusinessLogic.saveComposition(justAString, justAString, justAString, justAString, false);

		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock, times(3)).changeResourceLifecycleState(anyString(),any(),anyString(),any(),anyString());
		assertEquals(403, result.getStatusCodeValue());
	}

	@Test
	public void saveCompositionUpdateArtifactFailureNoRollback() throws Exception {

		emulateListOfArtifactsWithCompositionYml();
		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(vfcmt);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKOUT");
		RequestError requestError = new RequestError();
		requestError.setServiceException(new ServiceException("SVC4127", "", null));
		when(sdcClientMock.updateResourceArtifact(anyString(), anyString(), any(), anyString())).thenThrow(new ASDCException(HttpStatus.BAD_REQUEST, requestError));
		ResponseEntity result = compositionBusinessLogic.saveComposition(justAString, justAString, justAString, justAString, false);

		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock, times(0)).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
		assertEquals(409, result.getStatusCodeValue());
	}

	@Test
	public void saveCompositionNoExistingCompositionFailure() throws Exception {

		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(vfcmt);

		ResponseEntity result = compositionBusinessLogic.saveComposition(justAString, justAString, justAString, justAString, false);

		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock, times(0)).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock, times(0)).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock, times(0)).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
		assertEquals(404, result.getStatusCodeValue());
	}


	//backward compatibility - create new composition
	@Test
	public void successfulSaveNewCompositionAndCheckIn() throws Exception {

		when(sdcClientMock.getResource(anyString(),anyString())).thenReturn(vfcmt);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKOUT");

		compositionBusinessLogic.saveComposition(justAString, justAString, justAString, justAString, true);

		verify(sdcClientMock).getResource(anyString(),anyString());
		verify(sdcClientMock).createResourceArtifact(anyString(),anyString(),any(),anyString());
		verify(sdcClientMock, times(0)).updateResourceArtifact(anyString(), anyString(), any(), anyString());
		verify(sdcClientMock).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}


	@Test
	public void submitCompositionSuccessNoPreviousBlueprint() throws Exception {

		String vfiName = "vfiName";
		mockVfiList(vfiName);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKOUT");
		when(sdcClientMock.getAssetMetadata(anyString(),anyString(), anyString())).thenReturn(service);
		VfcmtData vfcmtData = new VfcmtData(vfcmt, vfiName, justAString, justAString);

		compositionBusinessLogic.submitComposition(justAString, justAString, vfcmtData, justAString, justAString);

		verify(sdcClientMock).getAssetMetadata(anyString(), anyString(), anyString());
		verify(sdcClientMock).createInstanceArtifact(anyString(), anyString(), anyString(), anyString(), any(),anyString());
		verify(sdcClientMock, times(0)).updateInstanceArtifact(anyString(), anyString(), anyString(), anyString(), any(), anyString());
		verify(sdcClientMock, times(0)).getMonitoringReferences(anyString(), anyString(), anyString(), anyString());
		verify(sdcClientMock, times(2)).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}

	@Test
	public void submitCompositionSuccessDeletePreviousReference() throws Exception {

		String vfiName = "vfiName";
		String flowType = "flowType";
		mockVfiWithBlueprint(vfiName, flowType);
		when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKIN");
		when(sdcClientMock.getAssetMetadata(anyString(),anyString(), anyString())).thenReturn(service);
		when(service.getVersion()).thenReturn("0.3");
		when(service.getUuid()).thenReturn(justAString);
		ExternalReferencesMap referencesMap = new ExternalReferencesMap();
		ResourceDetailed previousMcVersion = new ResourceDetailed();
		previousMcVersion.setInvariantUUID(vfcmt.getInvariantUUID());
		previousMcVersion.setUuid("previousId");
		referencesMap.put(Normalizers.normalizeComponentInstanceName(vfiName), Arrays.asList(vfcmt.getUuid(), previousMcVersion.getUuid()));
		when(sdcClientMock.getMonitoringReferences(anyString(), anyString(), anyString(), anyString())).thenReturn(referencesMap);
		when(sdcClientMock.getResource(previousMcVersion.getUuid(), justAString)).thenReturn(previousMcVersion);
		VfcmtData vfcmtData = new VfcmtData(vfcmt, vfiName, flowType, justAString);

		compositionBusinessLogic.submitComposition(justAString, justAString, vfcmtData, justAString, justAString);

		verify(sdcClientMock).getAssetMetadata(anyString(), anyString(), anyString());
		verify(sdcClientMock).updateInstanceArtifact(anyString(), anyString(), anyString(), anyString(), any(),anyString());
		verify(sdcClientMock, times(0)).createInstanceArtifact(anyString(), anyString(), anyString(), anyString(), any(), anyString());
		verify(sdcClientMock).getMonitoringReferences(anyString(), anyString(), anyString(), anyString());
		verify(sdcClientMock).getResource(anyString(), anyString());
		verify(sdcClientMock).deleteExternalMonitoringReference(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
		verify(sdcClientMock).changeResourceLifecycleState(anyString(),anyString(),anyString(),anyString(),anyString());
	}

	private void emulateListOfArtifactsWithCompositionYml() {
		List<Artifact> listOfArtifactCompositionYml = new ArrayList<>();
		Artifact compositionArtifact = Mockito.mock(Artifact.class);
		when(compositionArtifact.getArtifactName()).thenReturn(DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
		listOfArtifactCompositionYml.add(compositionArtifact);
		when(vfcmt.getArtifacts()).thenReturn(listOfArtifactCompositionYml);
	}

	private void mockVfiList(String vfiName) {
		List<ResourceInstance> vfiList = new ArrayList<>();
		ResourceInstance vfi = Mockito.mock(ResourceInstance.class);
		when(vfi.getResourceInstanceName()).thenReturn(vfiName);
		vfiList.add(vfi);
		when(service.getResources()).thenReturn(vfiList);
	}

	private void mockVfiWithBlueprint(String vfiName, String flowType) {
		mockVfiList(vfiName);
		List<Artifact> instanceArtifacts = new ArrayList<>();
		Artifact blueprint = Mockito.mock(Artifact.class);
		String artifactName = compositionBusinessLogic.generateBlueprintFileName(flowType, vfcmt.getName());
		when(blueprint.getArtifactName()).thenReturn(artifactName);
		instanceArtifacts.add(blueprint);
		when(service.getResources().get(0).getArtifacts()).thenReturn(instanceArtifacts);
	}

}
package org.onap.sdc.dcae.composition.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.MonitoringComponent;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceBusinessLogicTest {
    private String userId = "me";
    private String requestId = "1";
    private String monitoringComponentName = "monitoringComponentName";
    private String serviceUuid = "serviceUuid";
    private String vfiName = "vfiName";

    @Mock
    private ISdcClient sdcClientMock;

    @InjectMocks
    private ReferenceBusinessLogic classUnderTest;

    @Before
    public void setup(){
        classUnderTest.setSdcRestClient(sdcClientMock);
        new ErrorConfigurationLoader(System.getProperty("user.dir")+"/src/main/webapp/WEB-INF");
    }

    @Test
    public void successfulFetchMonitoringComponents() throws Exception {
    	ResourceDetailed mock_b = mockMcDetails("a", "b", "NOT_CERTIFIED_CHECKOUT");
    	ResourceDetailed mock_u = mockMcDetails("u", "u", "CERTIFIED");
    	ResourceDetailed mock_w = mockMcDetails("w", "w", "NOT_CERTIFIED_CHECKIN");
		MonitoringComponent expected_b = new MonitoringComponent(mock_b, "vfi1");
		expected_b.setSubmittedUuid("a");
		expected_b.setStatus("Submitted");
		MonitoringComponent expected_u = new MonitoringComponent(mock_u, "vfi2");
		expected_u.setUuid("u/v");
		MonitoringComponent expected_w = new MonitoringComponent(mock_w, "vfi2");
        when(sdcClientMock.getResource(eq("a"),anyString())).thenReturn(mockMcDetails("a", "a", "CERTIFIED"));
		when(sdcClientMock.getResource(eq("b"),anyString())).thenReturn(mock_b);
		when(sdcClientMock.getResource(eq("u"),anyString())).thenReturn(mock_u);
		when(sdcClientMock.getResource(eq("v"),anyString())).thenReturn(mockMcDetails("u", "v", "NOT_CERTIFIED_CHECKIN"));
		when(sdcClientMock.getResource(eq("w"),anyString())).thenReturn(mock_w);
        ExternalReferencesMap refs = new ExternalReferencesMap();
        refs.put("vfi1", Arrays.asList("a","b"));
		refs.put("vfi2", Arrays.asList("u","v_reverted","w"));
        Map<String, Collection<MonitoringComponent>> result = classUnderTest.fetchMonitoringComponents(refs, requestId);
        verify(sdcClientMock,times(5)).getResource(anyString(),anyString());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(3, result.get("monitoringComponents").size());
        Assert.assertTrue(result.get("monitoringComponents").contains(expected_b));
		Assert.assertTrue(result.get("monitoringComponents").contains(expected_u));
		Assert.assertTrue(result.get("monitoringComponents").contains(expected_w));
    }

    @Test
    public void partialSuccessfulFetchMonitoringComponents() throws Exception {
        when(sdcClientMock.getResource(eq("abc"),anyString())).thenReturn(mockMcDetails("abc", "abc", "CERTIFIED"));
		when(sdcClientMock.getResource(eq("xyz"),anyString())).thenReturn(mockMcDetails("xyz", "xyz", "CERTIFIED"));
        when(sdcClientMock.getResource(eq("no_such_uuid"),anyString())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        ExternalReferencesMap refs = new ExternalReferencesMap();
        refs.put("vfi1", Collections.singletonList("abc"));
        refs.put("vfi2", Collections.singletonList("xyz"));
        refs.put("vfi3", Collections.singletonList("no_such_uuid"));
        Map<String, Collection<MonitoringComponent>> result = classUnderTest.fetchMonitoringComponents(refs, requestId);
        verify(sdcClientMock,times(3)).getResource(anyString(),anyString());
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(2, result.get("monitoringComponents").size());
        Assert.assertEquals(1, result.get("unavailable").size());
    }

    @Test(expected=RuntimeException.class)
    public void deleteVfcmtReference_deleteFailed() {
        doThrow(RuntimeException.class).when(sdcClientMock).deleteExternalMonitoringReference(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        classUnderTest.deleteVfcmtReference(userId, "", "", "", "", requestId);
    }
    @Test
    public void deleteVfcmtReference_deleteSuccess() {
        classUnderTest.deleteVfcmtReference(userId, "", "", "", "", requestId);
        verify(sdcClientMock).deleteExternalMonitoringReference(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    private void mockGetService() throws Exception {
        ServiceDetailed serviceDetailed = new ServiceDetailed();
        ResourceInstance resourceInstance = new ResourceInstance();
        Artifact artifact = new Artifact();
        artifact.setArtifactName("." + monitoringComponentName + "." + DcaeBeConstants.Composition.fileNames.EVENT_PROC_BP_YAML);
        resourceInstance.setArtifacts(Collections.singletonList(artifact));
        resourceInstance.setResourceInstanceName(vfiName);
        serviceDetailed.setResources(Collections.singletonList(resourceInstance));
        when(sdcClientMock.getService(serviceUuid, requestId)).thenReturn(serviceDetailed);
    }

    @Test
    public void deleteVfcmtReferenceBlueprint_deleteSuccess() throws Exception {
        mockGetService();
        ResponseEntity responseEntity = classUnderTest.deleteVfcmtReferenceBlueprint(userId, "", monitoringComponentName, serviceUuid, vfiName, "", requestId);
        verify(sdcClientMock).getService(serviceUuid, requestId);
        verify(sdcClientMock).deleteInstanceArtifact(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void deleteVfcmtReferenceBlueprint_exceptionSdcGetService() throws Exception {
        when(sdcClientMock.getService(serviceUuid, requestId)).thenThrow(new RuntimeException(""));

        ResponseEntity<ResponseFormat> responseEntity = classUnderTest.deleteVfcmtReferenceBlueprint(userId, "", monitoringComponentName, serviceUuid, vfiName, "", requestId);

        Assert.assertEquals("The request was partially successful. Removing the attached Blueprint from the service has failed. You must manually delete the artifact.", responseEntity.getBody().getRequestError().getServiceException().getFormattedErrorMessage());
    }

    @Test
    public void deleteVfcmtReferenceBlueprint_exceptionSdcdeleteInstanceResourceArtifact() throws Exception {
        mockGetService();
        doThrow(new RuntimeException("")).when(sdcClientMock).deleteInstanceArtifact(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        ResponseEntity<ResponseFormat> responseEntity = classUnderTest.deleteVfcmtReferenceBlueprint(userId, "", monitoringComponentName, serviceUuid, vfiName, "", requestId);

        Assert.assertEquals("The request was partially successful. Removing the attached Blueprint from the service has failed. You must manually delete the artifact.", responseEntity.getBody().getRequestError().getServiceException().getFormattedErrorMessage());
    }

    private ResourceDetailed mockMcDetails(String invariantUuid, String uuid, String lifecycleState) {
    	ResourceDetailed res = new ResourceDetailed();
    	res.setUuid(uuid);
    	res.setInvariantUUID(invariantUuid);
    	res.setLifecycleState(lifecycleState);
    	return res;
	}
}

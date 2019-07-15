/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.sdc.dcae.composition.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.AttachVFCMTServiceRequest;
import org.onap.sdc.dcae.composition.restmodels.DcaeMinimizedService;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ActionDeserializer;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCondition;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ConditionDeserializer;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotEquals;

public class ServiceBusinessLogicTest {

    private String uuid = "b632b1da-e6ab-419d-8853-420e259097d9";
    private String userId = "gc786h";
    private String requestId = "1";
    private String monitoringComponentName = "monitoringComponentName";
    private String serviceUuid = "serviceUuid";
    private String vfiName = "vfiName";
    private String vfcmtUuid = "26e8d4b5-f087-4821-a75a-0b9514b5a7ab";
    private ResourceDetailed vfcmt = Mockito.mock(ResourceDetailed.class);
    private String categoryName = "Template";
    private String resourceType = "VFCMT";
    private ISdcClient sdcClientMock = Mockito.mock(ISdcClient.class);


    private static Gson gson = new GsonBuilder()
            .registerTypeAdapter(BaseAction.class, new ActionDeserializer())
            .registerTypeAdapter(BaseCondition.class, new ConditionDeserializer()).create();

    @InjectMocks
    ServiceBusinessLogic serviceBusinessLogic = new ServiceBusinessLogic();


    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        serviceBusinessLogic.setSdcRestClient(sdcClientMock);

        new ErrorConfigurationLoader(System.getProperty("user.dir") + "/src/main/webapp/WEB-INF");

        setServicesMock();
        setVfcmtMock();
    }

    private void setVfcmtMock() {

        when(vfcmt.getResourceType()).thenReturn(resourceType);
        when(vfcmt.getCategory()).thenReturn(categoryName);
        when(vfcmt.getLifecycleState()).thenReturn("NOT_CERTIFIED_CHECKIN");
        when(vfcmt.getUuid()).thenReturn(vfcmtUuid);
        List<Artifact> artifactList = new ArrayList<>();
        Artifact artifact = Mockito.mock(Artifact.class);//gson.fromJson(artifactJson, Artifact.class);
        artifactList.add(artifact);
        when(vfcmt.getArtifacts()).thenReturn(artifactList);

        when(artifact.getArtifactName()).thenReturn(DcaeBeConstants.Composition.fileNames.SVC_REF);

        when(serviceBusinessLogic.getSdcRestClient().getResource(anyString(), anyString())).thenReturn(vfcmt);
        when(serviceBusinessLogic.getSdcRestClient().changeResourceLifecycleState(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(vfcmt);

    }

    private void setServicesMock() {

        ServiceDetailed serviceDetailed = new ServiceDetailed();
        ResourceInstance resourceInstance = new ResourceInstance();
        Artifact artifact = new Artifact();
        artifact.setArtifactName("." + monitoringComponentName + "." + DcaeBeConstants.Composition.fileNames.EVENT_PROC_BP_YAML);
        resourceInstance.setArtifacts(Collections.singletonList(artifact));
        resourceInstance.setResourceInstanceName(vfiName);
        resourceInstance.setResoucreType("VF");
        serviceDetailed.setResources(Collections.singletonList(resourceInstance));
        when(serviceBusinessLogic.getSdcRestClient().getService(anyString(), anyString())).thenReturn(serviceDetailed);

        String serviceJson = "{\n\"lastUpdaterFullName\":\"GALCOHEN\",\n\"resources\":[{\n\"resourceInstanceName\":\"LiavVf0\",\n\"resourceName\":\"LiavVf\",\n\"resourceInvariantUUID\":\"47d5c3d6-83d8-4cbc-831c-1c7e52bd2964\",\n\"resourceVersion\":\"0.1\",\n\"resoucreType\":\"VF\",\n\"resourceUUID\":\"6e3a2db2-213b-41a4-b9eb-afab3c3b1463\",\n\"artifacts\":null\n}],\n\"artifacts\":null,\n\"distributionStatus\":\"DISTRIBUTION_NOT_APPROVED\",\n\"uuid\":\"b632b1da-e6ab-419d-8853-420e259097d9\",\n\"invariantUUID\":\"4cc1f274-107c-48e7-a7c7-9768c88043f7\",\n\"name\":\"Rony7777777\",\n\"version\":\"0.2\",\n\"toscaModelURL\":\"/sdc/v1/catalog/services/b632b1da-e6ab-419d-8853-420e259097d9/toscaModel\",\n\"lastUpdaterUserId\":\"gc786h\",\n\"category\":\"NetworkL1-3\",\n\"lifecycleState\":\"NOT_CERTIFIED_CHECKOUT\"\n}\n";
        Service service = gson.fromJson(serviceJson, Service.class);
        List<Service> serviceList = new ArrayList<>();
        serviceList.add(service);
        when(serviceBusinessLogic.getSdcRestClient().getServices(requestId)).thenReturn(serviceList);

    }

    @Test
    public void test_Service() {

        ResponseEntity<ServiceDetailed> result = serviceBusinessLogic.service(uuid, requestId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("vfiName", result.getBody().getResources().get(0).getResourceInstanceName());
        assertEquals("VF", result.getBody().getResources().get(0).getResoucreType());
    }

    @Test
    public void test_Services() {

        ResponseEntity<List<Service>> result = serviceBusinessLogic.services(userId, vfcmtUuid, requestId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotEquals(0, result.getBody().size());
        assertEquals(uuid, result.getBody().get(0).getUuid());

    }

    @Test
    public void test_AttachService() {

        AttachVFCMTServiceRequest request = new AttachVFCMTServiceRequest();
        request.setServiceUuid(serviceUuid);
        request.setInstanceName(vfiName);


        ResponseEntity<MessageResponse> result = serviceBusinessLogic.attachService(vfcmtUuid, userId, request, requestId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody().getSuccessResponse()).isEqualTo("Artifact updated");
    }

    @Test
    public void test_AttachService_verifyVfiExists_Exception() {

        AttachVFCMTServiceRequest request = new AttachVFCMTServiceRequest();
        request.setServiceUuid(serviceUuid);
        request.setInstanceName("WrongName");
        when(serviceBusinessLogic.checkinVfcmt(userId, uuid, requestId)).thenReturn(null);

        ResponseEntity<ResponseFormat> result = serviceBusinessLogic.attachService(vfcmtUuid, userId, request, requestId);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("SVC6039", result.getBody().getRequestError().getServiceException().getMessageId());
    }

    @Test
    public void test_GetAttachedService() {

        when(serviceBusinessLogic.getSdcRestClient().getResourceArtifact(any(), any(), any())).thenReturn("artifact data");

        ResponseEntity<MessageResponse> result = serviceBusinessLogic.getAttachedService(vfcmtUuid, requestId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody().getSuccessResponse()).isEqualTo("artifact data");
    }


    @Test
    public void test_parseAndFliterServicesByUser_nullServices_TBD() {
//		fail("TODO Auto-generated method stub");
    }


    @Test
    public void test_parseAndFliterServicesByUser_emptyList_emptyList() {
        // arrange
        String user_id = "test";
        String lastUpdaterUserId = "test";
        List<LinkedHashMap<String, String>> services = new ArrayList<LinkedHashMap<String, String>>();
        // act
        List<DcaeMinimizedService> result = serviceBusinessLogic.parseAndFilterServicesByUser(lastUpdaterUserId, services, user_id);
        // assert
        assertThat(result).isEqualTo(new ArrayList<DcaeMinimizedService>());
    }


    @Test
    public void test_parseAndFliterServicesByUser_singleServicesAsMap_singleServiceParsed() {
        // arrange
        String user_id = "test";
        String lastUpdaterUserId = user_id;
        String uuid = "a";
        String invariantUUID = "1";
        String lifecycleState = DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name();
        String version = "0.1";
        String serviceName = "TestService";

        LinkedHashMap<String, String> service = createServiceAsMap(lastUpdaterUserId, uuid, invariantUUID,
                lifecycleState, version, serviceName);
        List<LinkedHashMap<String, String>> services = new ArrayList<LinkedHashMap<String, String>>(
                Arrays.asList(service));

        DcaeMinimizedService expected = new DcaeMinimizedService(uuid, serviceName, lastUpdaterUserId, lifecycleState,
                version, invariantUUID);
        // act
        List<DcaeMinimizedService> result = serviceBusinessLogic.parseAndFilterServicesByUser(lastUpdaterUserId, services, user_id);
        // assert
        assertThat(result).usingRecursiveFieldByFieldElementComparator().contains(expected);
    }


    @Test
    public void test_parseAndFliterServicesByUser_unsortedServices_sortedServices() {
        // arrange
        String user_id = "test";
        String lastUpdaterUserId = user_id;
        String uuid = "a";
        String lifecycleState = DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name();
        String version = "0.1";

        List<LinkedHashMap<String, String>> unsortedServices = Arrays.asList("d", "a", "c", "b").stream()
                .map(x -> createServiceAsMap(lastUpdaterUserId, uuid, UUID.randomUUID().toString(), lifecycleState, version, x))
                .collect(Collectors.toList());


        // act
        List<DcaeMinimizedService> result = serviceBusinessLogic.parseAndFilterServicesByUser(lastUpdaterUserId, unsortedServices,
                user_id);
        // assert
        assertThat(result).extracting("name").containsExactly("a", "b", "c", "d");
    }


    @Test
    public void test_parseAndFliterServicesByUser_allOptionsForLastUpdaterAndIsCheckout_allOptionsButIsCheckoutAndNotLastUpdater() {
        // ------------user == last_updater
        // -----------------True----False--
        // isCheckout----------------------
        // --------True------V--------X----
        // --------False-----V--------V----
        // --------------------------------
//		fail("TODO Auto-generated method stub");
    }


    @Test
    public void test_parseAndFliterServicesByUser_singleServiceWithMultiVersions_singleServiceWithLatestVersion() {
        // arrange
        String user_id = "test";
        String lastUpdaterUserId = user_id;
        String uuid = "a";
        String invariantUUID = "1";
        String lifecycleState = DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name();
        String serviceName = "TestService";

        List<LinkedHashMap<String, String>> singleServiceWithMultiVersions = Arrays.asList("1.0", "0.3", "11.0", "2.0", "1.8").stream()
                .map(x -> createServiceAsMap(lastUpdaterUserId, uuid, invariantUUID, lifecycleState, x, serviceName))
                .collect(Collectors.toList());

        // act
        List<DcaeMinimizedService> result = serviceBusinessLogic.parseAndFilterServicesByUser(lastUpdaterUserId, singleServiceWithMultiVersions, user_id);

        // assert
        assertThat(result).extracting("version").containsExactly("11.0");
    }


    private static LinkedHashMap<String, String> createServiceAsMap(String lastUpdaterUserId, String uuid,
                                                                    String invariantUUID, String lifecycleState, String version, String serviceName) {

        LinkedHashMap<String, String> service = new LinkedHashMap<String, String>() {
            {
                put("invariantUUID", invariantUUID);
                put("uuid", uuid);
                put("name", serviceName);
                put("lastUpdaterUserId", lastUpdaterUserId);
                put("lifecycleState", lifecycleState);
                put("version", version);
            }
        };

        return service;
    }
}

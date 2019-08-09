/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (c) 2019 Samsung
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.springframework.web.client.ResourceAccessException;

import com.google.gson.JsonObject;

import json.templateInfo.TemplateInfo;
import tools.DeployTemplate;

public class DeployTemplateTest extends BaseTest {

    @InjectMocks
    DeployTemplate deployTemplate;

    private Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap;
    private TemplateInfo vfcmtTemplateInfo;

    private static final String VERIFY_FAILED_MESSAGE =
        "Deployment verify finished with errors, found only: 1 of 2 vfcmts";
    private static final String RESOURCE_ACCESS_EXCEPTION_MESSAGE = "Permission denny!";
    private static final String CREATE_RESOURCE_FAILED_MESSAGE =
        String.format("Failed create vfcmt: %s, With general message: %s", TEMPLATE_INFO_NAME,
            RESOURCE_ACCESS_EXCEPTION_MESSAGE);
    private static final String UPDATE_RESOURCE_FAILED_MESSAGE =
        String.format("Failed update vfcmt: %s, With general message: %s", VFCMT_NAME1,
            RESOURCE_ACCESS_EXCEPTION_MESSAGE);
    private static final String NOT_UPDATE_RESOURCE_MESSAGE =
        String.format("vfcmt: %s found, but didn't update.", VFCMT_NAME1);
    private final ResourceAccessException createResourceException =
        new ResourceAccessException(RESOURCE_ACCESS_EXCEPTION_MESSAGE);
    private static final String BAD_USER_ID = "badUserId";
    private static final String OPERATION_NOT_ALLOWED_FOR_USER_MESSAGE = String.format(
        "User conflicts. Operation not allowed for user %s on resource checked out by %s",
        BAD_USER_ID, USER_ID);
    private static final String UPDATE_FAILED_MESSAGE =
        String.format("Failed update vfcmt: %s, cannot checkout vfcmt", VFCMT_NAME1);

    @Before
    @Override
    public void setup() {
        super.setup();
        super.mockGetAllVfcmt();
        super.mockCheckoutVfcmtAndCreateResource();
        when(dcaeRestClient.getUserId()).thenReturn(USER_ID);
        when(dcaeRestClient.saveComposition(any(), any())).thenReturn("Composition Created");

        templateInfoToJsonObjectMap = new HashMap<>();
        vfcmtTemplateInfo = new TemplateInfo();
        vfcmtTemplateInfo.setName(VFCMT_NAME1);
        vfcmtTemplateInfo.setFlowType(TEMPLATE_INFO_FLOWTYPE);
        vfcmtTemplateInfo.setCategory("category");
        vfcmtTemplateInfo.setSubCategory("subCategory");
        vfcmtTemplateInfo.setDescription("description");
        vfcmtTemplateInfo.setUpdateIfExist(true);
        templateInfoToJsonObjectMap.put(vfcmtTemplateInfo, new JsonObject());
        TemplateInfo templateInfo = new TemplateInfo();
        templateInfo.setName(TEMPLATE_INFO_NAME);
        templateInfo.setFlowType(TEMPLATE_INFO_FLOWTYPE);
        templateInfo.setCategory("category");
        templateInfo.setSubCategory("subCategory");
        templateInfo.setDescription("description");
        templateInfoToJsonObjectMap.put(templateInfo, new JsonObject());
    }

    @Test
    public void deployHappyFlow() {
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(0)).addErrorMessage(anyString());
    }

    @Test
    public void deployFailedSaving() {
        when(dcaeRestClient.saveComposition(anyString(), anyString())).thenReturn("failed");
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(4)).addErrorMessage(anyString());
    }

    @Test
    public void deployFailedVerify() {
        mockGetAllVfcmtWithBadResources();
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(1)).addErrorMessage(VERIFY_FAILED_MESSAGE);
    }

    @Test
    public void deployFailedCreateResource() {
        when(dcaeRestClient.createResource(any(CreateVFCMTRequest.class)))
            .thenThrow(createResourceException);
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(1)).addErrorMessage(CREATE_RESOURCE_FAILED_MESSAGE);
        verify(report, times(1)).setStatusCode(2);
    }

    @Test
    public void deployButOperationNotAllowForTheUser() {
        when(dcaeRestClient.getUserId()).thenReturn(BAD_USER_ID);
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(1)).addErrorMessage(OPERATION_NOT_ALLOWED_FOR_USER_MESSAGE);
        verify(report, times(1)).addErrorMessage(UPDATE_FAILED_MESSAGE);
    }

    @Test
    public void deployAndCheckVfcmt() {
        mockGetAllVfcmtWithCertifiedResource();
        deployTemplate.deploy(templateInfoToJsonObjectMap);
    }

    @Test
    public void deployAndCheckVfcmtFaild() {
        mockGetAllVfcmtWithCertifiedResource();
        when(dcaeRestClient.checkoutVfcmt(UUID1)).thenThrow(createResourceException);
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(1)).addErrorMessage(UPDATE_RESOURCE_FAILED_MESSAGE);
        verify(report, times(1)).setStatusCode(2);
    }

    @Test
    public void deployAndNotUpdate() {
        vfcmtTemplateInfo.setUpdateIfExist(false);
        deployTemplate.deploy(templateInfoToJsonObjectMap);
        verify(report, times(1)).addNotUpdatedMessage(NOT_UPDATE_RESOURCE_MESSAGE);
    }

    private void mockGetAllVfcmtWithBadResources() {
        List<ResourceDetailed> resourceDetaileds = new ArrayList<>();
        resourceDetaileds.add(createResource("NOT_CERTIFIED_CHECKOUT"));
        when(dcaeRestClient.getAllVfcmts()).thenReturn(resourceDetaileds);
    }

    private void mockGetAllVfcmtWithCertifiedResource() {
        List<ResourceDetailed> resourceDetaileds = new ArrayList<>();
        resourceDetaileds.add(createResource("READY_FOR_CERTIFICATION"));
        when(dcaeRestClient.getAllVfcmts()).thenReturn(resourceDetaileds);
    }

    private ResourceDetailed createResource(String status) {
        ResourceDetailed resourceDetailed = new ResourceDetailed();
        resourceDetailed.setName(VFCMT_NAME1);
        resourceDetailed.setUuid(UUID1);
        resourceDetailed.setLifecycleState(status);
        resourceDetailed.setLastUpdaterUserId(USER_ID);
        return resourceDetailed;
    }
}

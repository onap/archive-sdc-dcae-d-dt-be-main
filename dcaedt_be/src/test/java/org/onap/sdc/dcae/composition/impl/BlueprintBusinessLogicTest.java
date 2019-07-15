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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.springframework.http.ResponseEntity;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.onap.sdc.dcae.composition.util.DcaeBeConstants.Composition.fileNames.COMPOSITION_YML;

public class BlueprintBusinessLogicTest {

    private static final String USER_ID = "UserId";
    private static final String CONTEXT = "Context";
    private static final String VFCMT_UUID = "VfcmtUuid";
    private static final String SERVICE_UUID = "ServiceUuid";
    private static final String VFI_NAME = "VfiName";
    private static final String FLOW_TYPE = "FlowType";
    private static final String REQUEST_ID = "RequestId";

    @InjectMocks
    BlueprintBusinessLogic classUnderTest;
    @Mock
    private ISdcClient sdcClientMock;
    private ResourceDetailed resourceDetailed;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        new ErrorConfigurationLoader(System.getProperty("user.dir")+"/src/main/webapp/WEB-INF");

        resourceDetailed = new ResourceDetailed();
        resourceDetailed.setUuid(VFCMT_UUID);
        classUnderTest.setSdcRestClient(sdcClientMock);
        when(sdcClientMock.getResource(eq(VFCMT_UUID), eq(REQUEST_ID))).thenReturn(resourceDetailed);
    }

    @Test
    public void generateAndSaveBlueprint_compositionNotFound() {
        ResponseEntity responseEntity = classUnderTest.generateAndSaveBlueprint(USER_ID, CONTEXT, VFCMT_UUID, SERVICE_UUID, VFI_NAME, FLOW_TYPE, REQUEST_ID);
        Assert.assertEquals( 404, responseEntity.getStatusCodeValue());
    }

    @Test
    public void generateAndSaveBlueprint_toscaFailed() {
        Artifact artifact = new Artifact();
        artifact.setArtifactName(COMPOSITION_YML);
        artifact.setPayloadData("{\\\"version\\\":0,\\\"flowType\\\":\\\"templateInfoFlowType\\\",\\\"nodes\\\":[],\\\"inputs\\\":[],\\\"outputs\\\":[],\\\"relations\\\":[]}\"");
        ArrayList<Artifact> artifacts = new ArrayList<>();
        artifacts.add(artifact);
        resourceDetailed.setArtifacts(artifacts);
        ResponseEntity responseEntity = classUnderTest.generateAndSaveBlueprint(USER_ID, CONTEXT, VFCMT_UUID, SERVICE_UUID, VFI_NAME, FLOW_TYPE, REQUEST_ID);
        Assert.assertEquals( 500, responseEntity.getStatusCodeValue());
    }

}

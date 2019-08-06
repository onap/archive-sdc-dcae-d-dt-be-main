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

package org.onap.sdc.dcae.utils.utils;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.enums.ArtifactGroupType;
import org.onap.sdc.dcae.enums.SdcConsumerInfo;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.util.Base64Utils;

import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(MockitoJUnitRunner.class)
public class SdcRestClientUtilsTest {

    private static final String USER_INFO = "USER_INFO";
    private static final String INSTANCE_INFO = "321";
    private static final String SCHEMA = "testSchema";
    private static final String HOST = "testHost";
    private static final int PORT = 123;
    private static final String PATH = "/test/path";
    private static final String RESOURCE_TYPE = "testResourceType";
    private static final String CATEGORY = "testCategory";
    private static final String SUB_CATEGORY = "testSubcategory";
    private static final String DESCRIPTION = "testDescription";
    private static final String NAME = "testName";
    private static final String LABEL = "testLabel";
    private static final String PAYLOAD_STRING = "testPayload";
    private static final byte[] PAYLOAD = PAYLOAD_STRING.getBytes();

    @Test
    public void testExtractConsumerInfoFormat()
        throws URISyntaxException, NoSuchFieldException, IllegalAccessException {
        // given
        URI configUri = new URI(SCHEMA, USER_INFO, HOST, PORT, PATH, null, INSTANCE_INFO);

        // when
        EnumMap<SdcConsumerInfo, String> userInfoMap =
            SdcRestClientUtils.extractConsumerInfoFromUri(configUri);

        // then
        assertEquals("Basic " + Base64Utils.encodeToString(USER_INFO.getBytes()),
            userInfoMap.get(SdcConsumerInfo.AUTH));
        assertEquals(INSTANCE_INFO, userInfoMap.get(SdcConsumerInfo.INSTANCE_ID));

        assertEquals(String.format("%s://%s:%d%s", SCHEMA, HOST, PORT, PATH) + getSdcCatalogPath(),
            userInfoMap.get(SdcConsumerInfo.CATALOG_URL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractConsumerWithoutInstanceInfo() throws URISyntaxException {
        // given
        URI configUri = new URI(SCHEMA, USER_INFO, HOST, PORT, PATH, null, null);
        // when - then
        SdcRestClientUtils.extractConsumerInfoFromUri(configUri);
    }

    @Test
    public void testBuildResourceFilterQuery() {
        // when
        String query =
            SdcRestClientUtils.buildResourceFilterQuery(RESOURCE_TYPE, CATEGORY, SUB_CATEGORY);
        // then
        assertEquals(String.format("?resourceType=%s&category=%s&subCategory=%s", RESOURCE_TYPE,
            CATEGORY, SUB_CATEGORY), query);
        // when
        query = SdcRestClientUtils.buildResourceFilterQuery(RESOURCE_TYPE, CATEGORY, null);
        // then
        assertEquals(String.format("?resourceType=%s&category=%s", RESOURCE_TYPE, CATEGORY), query);
        // when
        query = SdcRestClientUtils.buildResourceFilterQuery(RESOURCE_TYPE, null, null);
        // then
        assertEquals("?resourceType=" + RESOURCE_TYPE, query);
    }

    @Test
    public void testArtifactGeneration() {
        // when
        Artifact artifact = SdcRestClientUtils.generateDeploymentArtifact(DESCRIPTION, NAME,
            RESOURCE_TYPE, LABEL, PAYLOAD);

        // then
        assertEquals(DESCRIPTION, artifact.getDescription());
        assertEquals(NAME, artifact.getArtifactName());
        assertEquals(ArtifactGroupType.DEPLOYMENT.name(), artifact.getArtifactGroupType());
        assertEquals(RESOURCE_TYPE, artifact.getArtifactType());
        assertEquals(LABEL, artifact.getArtifactLabel());
        assertEquals(Base64Utils.encodeToString(PAYLOAD), artifact.getPayloadData());

        // when
        artifact = SdcRestClientUtils.generateCatalogDcaeToscaArtifact(NAME, PATH, PAYLOAD);

        // then
        assertEquals(NAME, artifact.getArtifactName());
        assertEquals(PATH, artifact.getArtifactURL());
        assertEquals(PAYLOAD_STRING, artifact.getPayloadData());
    }

    @Test
    public void testConvertArtifactToString() throws JsonProcessingException {
        // when
        Artifact artifact = SdcRestClientUtils.generateDeploymentArtifact(DESCRIPTION, NAME,
            RESOURCE_TYPE, LABEL, PAYLOAD);
        JSONObject resultJson = new JSONObject(SdcRestClientUtils.artifactToString(artifact));

        // then
        assertEquals(DESCRIPTION, resultJson.get("description"));
        assertEquals(NAME, resultJson.get("artifactName"));
        assertEquals(ArtifactGroupType.DEPLOYMENT.name(), resultJson.get("artifactGroupType"));
        assertEquals(RESOURCE_TYPE, resultJson.get("artifactType"));
        assertEquals(LABEL, resultJson.get("artifactLabel"));
        assertEquals(Base64Utils.encodeToString(PAYLOAD), resultJson.get("payloadData"));

        // when
        artifact = SdcRestClientUtils.generateCatalogDcaeToscaArtifact(NAME, PATH, PAYLOAD);
        resultJson = new JSONObject(SdcRestClientUtils.artifactToString(artifact));

        // then
        assertEquals(NAME, resultJson.get("artifactName"));
        assertEquals(PATH, resultJson.get("artifactURL"));
        assertEquals(PAYLOAD_STRING, resultJson.get("payloadData"));
    }

    private String getSdcCatalogPath() throws NoSuchFieldException, IllegalAccessException {
        Field field = SdcRestClientUtils.class.getDeclaredField("SDC_CATALOG_PATH");
        field.setAccessible(true);
        return field.get(null).toString();
    }
}

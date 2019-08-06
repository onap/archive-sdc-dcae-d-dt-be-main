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

package org.onap.sdc.dcae.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.enums.ArtifactGroupType;
import org.onap.sdc.dcae.enums.SdcConsumerInfo;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class SdcRestClientUtils {

    private static final String SDC_CATALOG_PATH = "/sdc/v1/catalog/";

    private SdcRestClientUtils() {
    }

    // TODO consider moving params elsewhere (user/password/instanceId can be constant)
    public static EnumMap<SdcConsumerInfo, String> extractConsumerInfoFromUri(URI configUri) {
        EnumMap<SdcConsumerInfo, String> userInfoMap = new EnumMap<>(SdcConsumerInfo.class);
        String userInfo = configUri.getUserInfo();
        if (userInfo != null) {
            userInfoMap.put(SdcConsumerInfo.AUTH, "Basic "+ Base64Utils.encodeToString(userInfo.getBytes()));
        }
        String fragment = configUri.getFragment();
        if (fragment == null)
            throw new IllegalArgumentException("The URI must contain a fragment specification, to be used as SDC instance id");
        userInfoMap.put(SdcConsumerInfo.INSTANCE_ID, fragment);
        try {
            userInfoMap.put(SdcConsumerInfo.CATALOG_URL, new URI(configUri.getScheme(), null, configUri.getHost(), configUri.getPort(), configUri.getPath()+SDC_CATALOG_PATH, null, null).toString());
        }
        catch (URISyntaxException se) {
            throw new IllegalArgumentException("Invalid uri", se);
        }
        return userInfoMap;
    }

    public static String buildResourceFilterQuery(String resourceType, String category, String subcategory) {
        List<String> filters = new ArrayList<>();
        if(!StringUtils.isEmpty(resourceType))
            filters.add("resourceType="+resourceType);
        if(!StringUtils.isEmpty(category))
            filters.add("category="+category);
        if(!StringUtils.isEmpty(subcategory))
            filters.add("subCategory="+subcategory);
        return "?"+ String.join("&", filters);
    }

    public static UserRemarks buildUserRemarksObject(String userRemarks) {
        return new UserRemarks(userRemarks);
    }

    private static class UserRemarks {
        private String userRemarks;

        private UserRemarks(String userRemarks) {
            this.userRemarks = userRemarks;
        }

        public String getUserRemarks() {
            return userRemarks;
        }
    }

    public static String artifactToString(Artifact artifact) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(artifact);
    }

    public static Artifact generateDeploymentArtifact(String description, String name, String type, String label, byte[] payload) {
        Artifact artifact = new Artifact();
        artifact.setDescription(description);
        artifact.setArtifactName(name);
        artifact.setArtifactGroupType(ArtifactGroupType.DEPLOYMENT.name());
        artifact.setArtifactType(type);
        artifact.setArtifactLabel(label);
        artifact.setPayloadData(Base64Utils.encodeToString(payload));
        return artifact;
    }

	public static Artifact generateCatalogDcaeToscaArtifact(String name, String path, byte[] payload) {
		Artifact artifact = new Artifact();
		artifact.setArtifactName(name);
		artifact.setArtifactURL(path);
		artifact.setPayloadData(new String(payload));
		return artifact;
	}
}

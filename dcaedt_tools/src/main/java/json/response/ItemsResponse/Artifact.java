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

package json.response.ItemsResponse;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Artifact {

    @SerializedName("artifactChecksum")
    private String mArtifactChecksum;
    @SerializedName("artifactDescription")
    private String mArtifactDescription;
    @SerializedName("artifactGroupType")
    private String mArtifactGroupType;
    @SerializedName("artifactLabel")
    private String mArtifactLabel;
    @SerializedName("artifactName")
    private String mArtifactName;
    @SerializedName("artifactType")
    private String mArtifactType;
    @SerializedName("artifactURL")
    private String mArtifactURL;
    @SerializedName("artifactUUID")
    private String mArtifactUUID;
    @SerializedName("artifactVersion")
    private String mArtifactVersion;

    public String getArtifactChecksum() {
        return mArtifactChecksum;
    }

    public void setArtifactChecksum(String artifactChecksum) {
        mArtifactChecksum = artifactChecksum;
    }

    public String getArtifactDescription() {
        return mArtifactDescription;
    }

    public void setArtifactDescription(String artifactDescription) {
        mArtifactDescription = artifactDescription;
    }

    public String getArtifactGroupType() {
        return mArtifactGroupType;
    }

    public void setArtifactGroupType(String artifactGroupType) {
        mArtifactGroupType = artifactGroupType;
    }

    public String getArtifactLabel() {
        return mArtifactLabel;
    }

    public void setArtifactLabel(String artifactLabel) {
        mArtifactLabel = artifactLabel;
    }

    public String getArtifactName() {
        return mArtifactName;
    }

    public void setArtifactName(String artifactName) {
        mArtifactName = artifactName;
    }

    public String getArtifactType() {
        return mArtifactType;
    }

    public void setArtifactType(String artifactType) {
        mArtifactType = artifactType;
    }

    public String getArtifactURL() {
        return mArtifactURL;
    }

    public void setArtifactURL(String artifactURL) {
        mArtifactURL = artifactURL;
    }

    public String getArtifactUUID() {
        return mArtifactUUID;
    }

    public void setArtifactUUID(String artifactUUID) {
        mArtifactUUID = artifactUUID;
    }

    public String getArtifactVersion() {
        return mArtifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        mArtifactVersion = artifactVersion;
    }

}

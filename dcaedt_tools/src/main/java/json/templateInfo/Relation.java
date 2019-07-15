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

package json.templateInfo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Relation {
    @SerializedName("fromComponent")
    private String fromComponent;
    @SerializedName("fromRequirement")
    private String fromRequirement;
    @SerializedName("toComponent")
    private String toComponent;
    @SerializedName("toCapability")
    private String toCapability;

    public String getFromComponent() {
        return fromComponent;
    }

    public void setFromComponent(String fromComponent) {
        this.fromComponent = fromComponent;
    }

    public String getFromRequirement() {
        return fromRequirement;
    }

    public void setFromRequirement(String fromRequirement) {
        this.fromRequirement = fromRequirement;
    }

    public String getToComponent() {
        return toComponent;
    }

    public void setToComponent(String toComponent) {
        this.toComponent = toComponent;
    }

    public String getToCapability() {
        return toCapability;
    }

    public void setToCapability(String toCapability) {
        this.toCapability = toCapability;
    }
}

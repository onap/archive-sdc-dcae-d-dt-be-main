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

package json.response.ModelResponse;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Capability {

    @SerializedName("dcae.capabilities.stream.subscribe")
    private DcaeCapabilitiesStreamSubscribe mDcaeCapabilitiesStreamSubscribe;
    @SerializedName("id")
    private String mId;
    @SerializedName("name")
    private String mName;
    @SerializedName("properties")
    private List<Property> mProperties;
    @SerializedName("type")
    private Type mType;

    public DcaeCapabilitiesStreamSubscribe getDcaeCapabilitiesStreamSubscribe() {
        return mDcaeCapabilitiesStreamSubscribe;
    }

    public void setDcaeCapabilitiesStreamSubscribe(DcaeCapabilitiesStreamSubscribe dcaeCapabilitiesStreamSubscribe) {
        mDcaeCapabilitiesStreamSubscribe = dcaeCapabilitiesStreamSubscribe;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public List<Property> getProperties() {
        return mProperties;
    }

    public void setProperties(List<Property> properties) {
        mProperties = properties;
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }

}

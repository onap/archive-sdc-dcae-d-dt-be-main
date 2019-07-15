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

package json;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Environment {

    @SerializedName("apiPath")
    private String ApiPath;
    @SerializedName("dcaeBeHost")
    private String DcaeBeHost;
    @SerializedName("dcaeBePort")
    private String DcaeBePort;
    @SerializedName("credential")
    private Credential credential;

    public String getApiPath() {
        return ApiPath;
    }

    public void setApiPath(String apiPath) {
        ApiPath = apiPath;
    }

    public String getDcaeBeHost() {
        return DcaeBeHost;
    }

    public void setDcaeBeHost(String dcaeBeHost) {
        DcaeBeHost = dcaeBeHost;
    }

    public String getDcaeBePort() {
        return DcaeBePort;
    }

    public void setDcaeBePort(String dcaeBePort) {
        DcaeBePort = dcaeBePort;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

}

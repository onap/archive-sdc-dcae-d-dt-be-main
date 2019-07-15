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

package utilities;

import json.Credential;
import json.Environment;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.canvas.DcaeComponentCatalog;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.LoggerDebug;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("dcaerestclient")
public class DcaeRestClient implements IDcaeRestClient {

    private static LoggerDebug debugLogger = LoggerDebug.getInstance();
    private static final String GET_RESOURCES_BY_CATEGORY = "/getResourcesByCategory";
    private static final String CREATE_VFCMT = "/createVFCMT";
    private static final String ELEMENTS = "/elements";
	private static final String CATALOG = "/catalog";


    private static final String ECOMP_INSTANCE_ID_HEADER = "X-ECOMP-InstanceID";
    private static final String USER_ID_HEADER = "USER_ID";


    private String uri;
    private RestTemplate client;
    private Credential credential;

    public DcaeRestClient(Credential credential) {
        this.credential = credential;
    }

    @Override
    public String getUserId() {
        return credential.getUsername();
    }

    @PostConstruct
    @Override
    public void init(Environment environment) {
        credential = environment.getCredential();
        debugLogger.log("Connecting to server host: " + environment.getDcaeBeHost() + ", port: " + environment.getDcaeBePort());
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultHeaders(defaultHeaders(credential)).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        client = new RestTemplate(requestFactory);
        uri = String.format("%s:%s%s", environment.getDcaeBeHost(), environment.getDcaeBePort(), environment.getApiPath());
    }

    private List<BasicHeader> defaultHeaders(Credential credential) {
        List<BasicHeader> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.add(new BasicHeader(ECOMP_INSTANCE_ID_HEADER, credential.getUsername()));
        return headers;
    }

    @Override
    public List<ResourceDetailed> getAllVfcmts() {
        String url = buildRequestPath(GET_RESOURCES_BY_CATEGORY);
        return Arrays.asList(client.getForObject(url, ResourceDetailed[].class));
    }

    @Override
    public List<ResourceDetailed> getAllBaseVfcmts() {
        String url = buildRequestPath("/getResourcesByMonitoringTemplateCategory");
        return Arrays.asList(client.getForObject(url, ResourceDetailed[].class));
    }

    @Override
    public ResourceDetailed createResource(CreateVFCMTRequest resource) {
        String url = buildRequestPath(CREATE_VFCMT);
        return client.postForObject(url, new HttpEntity<>(resource, postResourceHeaders(credential.getUsername())), ResourceDetailed.class);
    }

    @Override
    public ResourceDetailed checkoutVfcmt(String vfcmtUuid) {
        String url = buildRequestPath(String.format("/checkout/vfcmt/%s", vfcmtUuid));
        ResponseEntity<ResourceDetailed> resourceDetailedResponse = client.exchange(url, HttpMethod.PUT, new HttpEntity(postResourceHeaders(credential.getUsername())), ResourceDetailed.class);

        return resourceDetailedResponse.getBody();
    }

    @Override
    public ResourceDetailed checkinVfcmt(String vfcmtUuid) {
        String url = buildRequestPath(String.format("/checkin/vfcmt/%s", vfcmtUuid));
        ResponseEntity<ResourceDetailed> resourceDetailedResponse = client.exchange(url, HttpMethod.PUT, new HttpEntity(postResourceHeaders(credential.getUsername())), ResourceDetailed.class);

        return resourceDetailedResponse.getBody();
    }

	@Override
	public Map<String, List<Resource>> getDcaeCatalog() {
		String url = buildRequestPath(CATALOG);
		DcaeComponentCatalog catalog = client.getForObject(url, DcaeComponentCatalog.class);
		return catalog.getElements().stream().collect(Collectors.toMap(DcaeComponentCatalog.SubCategoryFolder::getName, DcaeComponentCatalog.SubCategoryFolder::getItems));
	}


    @Override
    public String getItemModel(String elementId) {
        String url = buildRequestPath("/"+ elementId +"/model");
        return client.getForObject(url, String.class);
    }
    @Override
    public String getItemType(String elementId, String type) {
        String url = buildRequestPath("/"+ elementId +"/type/"+ type +"/");
        return client.getForObject(url, String.class);
    }

    @Override
    public String saveComposition(String componentId, String body) {
        String url = buildRequestPath("/saveComposition/" + componentId);
        ResponseEntity<String> resourceDetailedResponse = client.exchange(url, HttpMethod.POST, new HttpEntity<>(body, postResourceHeaders(credential.getUsername())), String.class);

        return resourceDetailedResponse.getBody();
    }

    @Override
    public String certifyVfcmt(String vfcmtUuid) {
        String url = buildRequestPath(String.format("/certify/vfcmt/%s", vfcmtUuid));
        ResponseEntity<String> resourceDetailedResponse = client.exchange(url, HttpMethod.PUT, new HttpEntity(postResourceHeaders(credential.getUsername())), String.class);

        return resourceDetailedResponse.getBody();
    }

    private HttpHeaders postResourceHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add(USER_ID_HEADER, userId);
        return headers;
    }

    private String buildRequestPath(String... args){
        String url = uri + Stream.of(args).collect(Collectors.joining());
        debugLogger.log("Sending request: " + url);
        return url;
    }

    @Override
    public void updateResource(ResourceDetailed vfcmt) {
        // Do nothing
    }
}

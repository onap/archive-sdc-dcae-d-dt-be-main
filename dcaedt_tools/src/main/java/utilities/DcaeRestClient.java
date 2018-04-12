package utilities;

import json.Credential;
import json.Environment;
import json.response.ElementsResponse.Element;
import json.response.ElementsResponse.ElementsResponse;
import json.response.ItemsResponse.Item;
import json.response.ItemsResponse.ItemsResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("dcaerestclient")
public class DcaeRestClient implements IDcaeRestClient {

    private static final String GET_RESOURCES_BY_CATEGORY = "/getResourcesByCategory";
    private static final String CREATE_VFCMT = "/createVFCMT";
    private static final String ELEMENTS = "/elements";


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
    public List<Element> getElements() {
        String url = buildRequestPath(ELEMENTS);
        return client.getForObject(url, ElementsResponse.class).getData().getElements();
    }
    @Override
    public List<Item> getItem(String element) {
        String url = buildRequestPath("/"+ element + ELEMENTS);
        return client.getForObject(url, ItemsResponse.class).getData().getElement() == null ? null : client.getForObject(url, ItemsResponse.class).getData().getElement().getItems();
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
        return uri + Stream.of(args).collect(Collectors.joining());
    }

    @Override
    public void updateResource(ResourceDetailed vfcmt) {
        // Do nothing
    }
}

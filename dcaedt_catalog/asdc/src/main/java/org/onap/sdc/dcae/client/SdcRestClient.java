package org.onap.sdc.dcae.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.ReferenceUUID;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.enums.SdcConsumerInfo;
import org.onap.sdc.dcae.utils.Normalizers;
import org.onap.sdc.dcae.utils.SDCResponseErrorHandler;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("sdcrestclient")
public class SdcRestClient implements ISdcClient {

    @Autowired
    private SystemProperties systemProperties;

    private static final String SLASH = "/";
    private static final String ECOMP_INSTANCE_ID_HEADER = "X-ECOMP-InstanceID";
    private static final String ECOMP_REQUEST_ID_HEADER = "X-ECOMP-RequestID";
    private static final String USER_ID_HEADER = "USER_ID";
    private static final String ARTIFACTS_PATH = "artifacts";
    private static final String CONTENT_MD5_HEADER = "Content-MD5";
    private static final String RESOURCE_INSTANCES_PATH = "resourceInstances";
    private static final String LIFECYCLE_STATE_PATH = "lifecycleState/{lifecycleOperation}";
    private static final String METADATA_PATH = "metadata";
    private static final String VERSION_PATH = "version";
    private static final String CSAR_PATH = "toscaModel";
    private static final String MONITORING_REFERENCES_PATH = "externalReferences/monitoring";

    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private String uri;

    private RestTemplate client;

    @PostConstruct
    private void init() {
        URI configUri = URI.create(systemProperties.getProperties().getProperty(DcaeBeConstants.Config.URI));
        EnumMap<SdcConsumerInfo, String> userInfo = SdcRestClientUtils.extractConsumerInfoFromUri(configUri);
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultHeaders(defaultHeaders(userInfo)).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        client = new RestTemplate(requestFactory);
        client.setErrorHandler(new SDCResponseErrorHandler());
        uri = userInfo.get(SdcConsumerInfo.CATALOG_URL);
    }

    private List<BasicHeader> defaultHeaders(EnumMap<SdcConsumerInfo, String> userInfo) {
        List<BasicHeader> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.AUTHORIZATION, userInfo.get(SdcConsumerInfo.AUTH)));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.add(new BasicHeader(ECOMP_INSTANCE_ID_HEADER, userInfo.get(SdcConsumerInfo.INSTANCE_ID)));
        return headers;
    }

    public ServiceDetailed getAssetMetadata(String contextType, String uuid, String requestId) {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), uuid, METADATA_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get asset metadata from SDC. URL={}", url);
        return getObject(url, requestId, ServiceDetailed.class);
    }

    public ResourceDetailed getResource(String uuid, String requestId) {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), uuid, METADATA_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get resource from SDC. URL={}", url);
        return getObject(url, requestId, ResourceDetailed.class);
    }

	public byte[] getResourceToscaModel(String uuid, String requestId) {
		String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), uuid, CSAR_PATH);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get resource csar from SDC. URL={}", url);
		return getObject(url, requestId, byte[].class);
	}

    public ServiceDetailed getService(String uuid, String requestId) {
        return getAssetMetadata(AssetType.SERVICE.name(), uuid, requestId);
    }

    public List<Resource> getResources(String resourceType, String category, String subcategory, String requestId) {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), SdcRestClientUtils.buildResourceFilterQuery(resourceType, category, subcategory));
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get resources from SDC. URL={}", url);
        return Arrays.asList(getObject(url, requestId, Resource[].class));
    }

    public List<Service> getServices(String requestId) {
        String url = buildRequestPath(AssetType.SERVICE.getSdcContextPath());
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get services from SDC. URL={}", url);
        return Arrays.asList(getObject(url, requestId, Service[].class));
    }

    public String addExternalMonitoringReference(String userId, String contextType, String serviceUuid, String vfiName, ReferenceUUID vfcmtUuid, String requestId) {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), serviceUuid, RESOURCE_INSTANCES_PATH, Normalizers.normalizeComponentInstanceName(vfiName), MONITORING_REFERENCES_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Creating external monitoring reference from service id {} vfi name {} to vfcmt {} URL={}", serviceUuid, vfiName, vfcmtUuid.getReferenceUUID(), url);
        return client.postForObject(url, new HttpEntity<>(vfcmtUuid, postResourceHeaders(userId, requestId)), String.class);
    }

    public String addExternalMonitoringReference(String userId, CreateVFCMTRequest resource, ReferenceUUID vfcmtUuid, String requestId) {
        return addExternalMonitoringReference(userId, resource.getContextType(), resource.getServiceUuid(), resource.getVfiName(), vfcmtUuid, requestId);
    }

    public void updateExternalMonitoringReference(String userId, String contextType, String serviceUuid, String vfiName, String vfcmtUuid, ReferenceUUID updatedReference, String requestId) {
		String url = buildRequestPath(AssetType.getSdcContextPath(contextType), serviceUuid, RESOURCE_INSTANCES_PATH, Normalizers.normalizeComponentInstanceName(vfiName), MONITORING_REFERENCES_PATH, vfcmtUuid);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Updating external monitoring reference from service id {} vfi name {} to vfcmt {} URL={}", serviceUuid, vfiName, vfcmtUuid, url);
		client.put(url, new HttpEntity<>(updatedReference, postResourceHeaders(userId, requestId)));
	}

    public void deleteExternalMonitoringReference(String userId, String contextType, String serviceUuid, String normalizeVfiName, String vfcmtUuid, String requestId) {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), serviceUuid, RESOURCE_INSTANCES_PATH, normalizeVfiName, MONITORING_REFERENCES_PATH, vfcmtUuid);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Delete external monitoring reference from SDC asset. URL={}", url);
        client.exchange(url, HttpMethod.DELETE, new HttpEntity(postResourceHeaders(userId, requestId)), String.class);
    }

    public ResourceDetailed createResource(String userId, CreateVFCMTRequest resource, String requestId) {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath());
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Create SDC resource with name {} URL={}", resource.getName(), url);
        return client.postForObject(url, new HttpEntity<>(resource, postResourceHeaders(userId, requestId)), ResourceDetailed.class);
    }

    public ResourceDetailed changeResourceLifecycleState(String userId, String uuid, String lifecycleOperation, String userRemarks, String requestId) {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), uuid, LIFECYCLE_STATE_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Change SDC resource lifecycle state ({}). URL={}", lifecycleOperation, url);
        return client.postForObject(url, new HttpEntity<>(SdcRestClientUtils.buildUserRemarksObject(userRemarks), postResourceHeaders(userId, requestId)), ResourceDetailed.class, lifecycleOperation);
    }

    public String getResourceArtifact(String resourceUuid, String artifactUuid, String requestId) {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), resourceUuid, ARTIFACTS_PATH, artifactUuid);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get resource artifact from SDC. URL={}", url);
        return getObject(url, requestId, String.class);
    }

    public Artifact createResourceArtifact(String userId, String resourceUuid, Artifact artifact, String requestId) throws JsonProcessingException {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), resourceUuid, ARTIFACTS_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Create SDC resource artifact. URL={}", url);
        String artifactData = SdcRestClientUtils.artifactToString(artifact);
        return client.postForObject(url, new HttpEntity<>(artifactData, postArtifactHeaders(userId, artifactData, requestId)), Artifact.class);
    }

    public Artifact updateResourceArtifact(String userId, String resourceUuid, Artifact artifact, String requestId) throws JsonProcessingException {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), resourceUuid, ARTIFACTS_PATH, artifact.getArtifactUUID());
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Update SDC resource artifact. URL={}", url);
        String artifactData = SdcRestClientUtils.artifactToString(artifact);
        return client.postForObject(url, new HttpEntity<>(artifactData, postArtifactHeaders(userId, artifactData, requestId)), Artifact.class);
    }

    public void deleteResourceArtifact(String userId, String resourceUuid, String artifactId, String requestId) {
        String url = buildRequestPath(AssetType.RESOURCE.getSdcContextPath(), resourceUuid, ARTIFACTS_PATH, artifactId);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Delete SDC resource artifact. URL={}", url);
        client.exchange(url, HttpMethod.DELETE, new HttpEntity(postResourceHeaders(userId, requestId)), Artifact.class);
    }

    public  Artifact createInstanceArtifact(String userId, String contextType, String serviceUuid, String normalizedInstanceName, Artifact artifact, String requestId) throws JsonProcessingException {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), serviceUuid, RESOURCE_INSTANCES_PATH, normalizedInstanceName, ARTIFACTS_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Create SDC resource instance artifact. URL={}", url);
        String artifactData = SdcRestClientUtils.artifactToString(artifact);
        return client.postForObject(url, new HttpEntity<>(artifactData, postArtifactHeaders(userId, artifactData, requestId)), Artifact.class);
    }

    public Artifact updateInstanceArtifact(String userId, String contextType, String serviceUuid, String normalizedInstanceName, Artifact artifact, String requestId) throws JsonProcessingException {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), serviceUuid, RESOURCE_INSTANCES_PATH, normalizedInstanceName, ARTIFACTS_PATH, artifact.getArtifactUUID());
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Update SDC resource instance artifact. URL={}", url);
        String artifactData = SdcRestClientUtils.artifactToString(artifact);
        return client.postForObject(url, new HttpEntity<>(artifactData, postArtifactHeaders(userId, artifactData, requestId)), Artifact.class);
    }

    public ExternalReferencesMap getMonitoringReferences(String contextType, String uuid, String version, String requestId) {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), uuid, VERSION_PATH, version, MONITORING_REFERENCES_PATH);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Get SDC service monitoring references. URL={}", url);
        return getObject(url, requestId, ExternalReferencesMap.class);
    }

    public void deleteInstanceArtifact(String userId, String contextType, String serviceUuid, String normalizedVfiName, String artifactUuid, String requestId) {
        String url = buildRequestPath(AssetType.getSdcContextPath(contextType), serviceUuid, RESOURCE_INSTANCES_PATH, normalizedVfiName, ARTIFACTS_PATH, artifactUuid);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Delete SDC instance resource artifact. URL={}", url);
        client.exchange(url, HttpMethod.DELETE, new HttpEntity(postResourceHeaders(userId, requestId)), Artifact.class);
    }

    private HttpHeaders postResourceHeaders(String userId, String requestId) {
        HttpHeaders headers = requestHeader(requestId);
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add(USER_ID_HEADER, userId);
        return headers;
    }

    private HttpHeaders postArtifactHeaders(String userId, String artifact, String requestId) {
        HttpHeaders headers = postResourceHeaders(userId, requestId);
        String md5 = Base64Utils.encodeToString(DigestUtils.md5Hex(artifact).getBytes());
        headers.add(CONTENT_MD5_HEADER, md5);
        return headers;
    }

    private HttpHeaders requestHeader(String requestId){
        HttpHeaders headers = new HttpHeaders();
        headers.add(ECOMP_REQUEST_ID_HEADER, requestId);
        return headers;
    }

    private <T> T getObject(String url, String requestId, Class<T> clazz) {
        return client.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeader(requestId)), clazz).getBody();
    }

    private String buildRequestPath(String... args){
        return uri + Stream.of(args).collect(Collectors.joining(SLASH));
    }
}
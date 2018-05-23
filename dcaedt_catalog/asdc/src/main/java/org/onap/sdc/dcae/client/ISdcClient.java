package org.onap.sdc.dcae.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.ReferenceUUID;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;

import java.util.List;

public interface ISdcClient {

    ResourceDetailed getResource(String uuid, String requestId);

    ServiceDetailed getService(String uuid, String requestId);

    ServiceDetailed getAssetMetadata(String contextType, String uuid, String requestId);

    List<Resource> getResources(String resourceType, String category, String subcategory, String requestId);

    List<Service> getServices(String requestId);

    String addExternalMonitoringReference(String userId, String contextType, String serviceUuid, String vfiName, ReferenceUUID vfcmtUuid, String requestId);

    String addExternalMonitoringReference(String userId, CreateVFCMTRequest resource, ReferenceUUID vfcmtUuid, String requestId);

    void deleteExternalMonitoringReference(String userId, String contextType, String uuid, String vfiName, String vfcmtUuid, String requestId);

    ResourceDetailed createResource(String userId, CreateVFCMTRequest resource, String requestId);

    ResourceDetailed changeResourceLifecycleState(String userId, String uuid, String lifecycleOperation, String userRemarks, String requestId);

    String getResourceArtifact(String resourceUuid, String artifactUuid, String requestId);

    Artifact createResourceArtifact(String userId, String resourceUuid, Artifact artifact, String requestId) throws JsonProcessingException;

    Artifact updateResourceArtifact(String userId, String resourceUuid, Artifact artifact, String requestId) throws JsonProcessingException;

    void deleteResourceArtifact(String userId, String resourceUuid, String artifactId, String requestId);

    Artifact createInstanceArtifact(String userId, String contextType, String serviceUuid, String normalizedInstanceName, Artifact artifact, String requestId) throws JsonProcessingException;

    Artifact updateInstanceArtifact(String userId, String contextType, String serviceUuid, String normalizedInstanceName, Artifact artifact, String requestId) throws JsonProcessingException;

    ExternalReferencesMap getMonitoringReferences(String contextType, String uuid, String version, String requestId);

    void deleteInstanceArtifact(String userId, String contextType, String serviceUuid, String normalizedVfiName, String artifactUuid, String requestId);

}

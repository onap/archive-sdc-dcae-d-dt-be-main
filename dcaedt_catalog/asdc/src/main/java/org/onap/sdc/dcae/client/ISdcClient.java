package org.onap.sdc.dcae.client;

import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.restmodels.ReferenceUUID;
import org.onap.sdc.dcae.enums.AssetType;

import java.util.List;

public interface ISdcClient {

    ResourceDetailed getResource(String uuid, String requestId) throws Exception;

    ServiceDetailed getService(String uuid, String requestId) throws Exception;

    List<Resource> getResources(String resourceType, String category, String subcategory, String requestId) throws Exception;

    List<Service> getServices(String requestId) throws Exception;

    String addExternalMonitoringReference(String userId, CreateVFCMTRequest resource, ReferenceUUID vfiUuid, String requestId);

    void deleteExternalMonitoringReference(String userId, String context, String uuid, String vfiName, String vfcmtUuid, String requestId);

    ResourceDetailed createResource(String userId, CreateVFCMTRequest resource, String requestId) throws Exception;

    ResourceDetailed changeResourceLifecycleState(String userId, String uuid, String lifecycleOperation, String userRemarks, String requestId) throws Exception;

    ServiceDetailed changeServiceLifecycleState(String userId, String uuid, String lifecycleOperation, String userRemarks, String requestId) throws Exception;

    Asset changeAssetLifecycleState(String userId, String uuid, String lifecycleOperation, String userRemarks, AssetType assetType, String requestId) throws Exception;

    String getResourceArtifact(String resourceUuid, String artifactUuid, String requestId) throws Exception;

    Artifact createResourceArtifact(String userId, String resourceUuid, Artifact artifact, String requestId) throws Exception;

    Artifact updateResourceArtifact(String userId, String resourceUuid, Artifact artifact, String requestId) throws Exception;

    void deleteResourceArtifact(String userId, String resourceUuid, String artifactId, String requestId) throws Exception;

    Artifact createVfInstanceArtifact(String userId, String serviceUuid, String normalizedInstanceName, Artifact artifact, String requestId) throws Exception;

    Artifact updateVfInstanceArtifact(String userId, String serviceUuid, String normalizedInstanceName, Artifact artifact, String requestId) throws Exception;

    ExternalReferencesMap getMonitoringReferences(String context, String uuid, String version, String requestId);

    void deleteInstanceResourceArtifact(String userId, String context, String serviceUuid, String normalizedVfiName, String artifactUuid, String requestId);
}

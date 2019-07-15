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

package org.onap.sdc.dcae.composition.impl;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.AttachVFCMTServiceRequest;
import org.onap.sdc.dcae.composition.restmodels.DcaeMinimizedService;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.DcaeException;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceBusinessLogic extends BaseBusinessLogic {


    public ResponseEntity services(String userId, String vfcmtUuid, String requestId) {
        try {
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "GET ({}) Vfcmt form SDC: {}", vfcmtUuid, vfcmt);
            checkVfcmtType(vfcmt);
            checkUserIfResourceCheckedOut(userId, vfcmt);

            List<Service> services = getSdcRestClient().getServices(requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "GET services data from SDC: {}", services);
            List<Service> uuids = filterServicesByUser(services, userId);
            return new ResponseEntity<>(uuids, HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
            return ErrConfMgr.INSTANCE.handleException(e,  ErrConfMgr.ApiType.GET_SERVICE);
            }
    }

    public ResponseEntity service(String theServiceId, String requestId) {
        try {
            ServiceDetailed service = getSdcRestClient().getService(theServiceId, requestId);
            if (service != null) {
                if(service.getResources()!=null){
                    List<ResourceInstance> vfResourcesOnly = service.getResources().stream().filter(vfi -> vfi.getResoucreType().equals("VF")).collect(Collectors.toList());
                    service.setResources(vfResourcesOnly);
                }else{
                    errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Service {} doesn't have any resources (e.g VFi's)", theServiceId);
                }
            } else {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Couldn't fetch service with uuid {} from SDC", theServiceId);
            }
            return new ResponseEntity<>(service, HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
            return ErrConfMgr.INSTANCE.handleException(e,  ErrConfMgr.ApiType.GET_SERVICE);
        }
    }


     public ResponseEntity attachService( String vfcmtUuid, String userId, AttachVFCMTServiceRequest request, String requestId) {

        String serviceUuid = request.getServiceUuid();
        String vfiName = request.getInstanceName();
        String resourceUuid = vfcmtUuid;
        MessageResponse response = new MessageResponse();

        try {
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());

            checkVfcmtType(vfcmt);
            verifyVfiExists(serviceUuid, vfiName, requestId);

            boolean isUpdateMode = false;
            Artifact artifactObj = null;

            String reference = serviceUuid + "/resources/" + vfiName;
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "*****************************************");
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Reference between service and vfi {}", reference);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "*****************************************");

            if(!CollectionUtils.isEmpty(vfcmt.getArtifacts())){
                artifactObj = vfcmt.getArtifacts().stream().filter(a -> DcaeBeConstants.Composition.fileNames.SVC_REF.equals(a.getArtifactName())).findAny().orElse(null);
                isUpdateMode = null != artifactObj;
            }

            if (isNeedToCheckOut(vfcmt.getLifecycleState())) {
                vfcmt = getSdcRestClient().changeResourceLifecycleState(userId, vfcmtUuid, LifecycleOperationType.CHECKOUT.name(), null, requestId);
                if (vfcmt != null) {
                    resourceUuid = vfcmt.getUuid();
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New vfcmt uuid after checkoutVfcmt is: {}", resourceUuid);
                }
            }

            if(isUpdateMode){
                updateReferenceArtifact(userId, resourceUuid, artifactObj, reference, requestId);
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Artifact {} updated with content: {}", reference, DcaeBeConstants.Composition.fileNames.SVC_REF, reference);
            }else{
                Artifact artifact = SdcRestClientUtils.generateDeploymentArtifact("createReferenceArtifact", DcaeBeConstants.Composition.fileNames.SVC_REF, ArtifactType.DCAE_TOSCA.name(), "servicereference", reference.getBytes());
                getSdcRestClient().createResourceArtifact(userId, resourceUuid, artifact, requestId);
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Artifact {} created with content: {}", DcaeBeConstants.Composition.fileNames.SVC_REF, reference);
            }
            checkinVfcmt(userId, resourceUuid, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Attachment of reference={} in VFCMT {} has finished successfully", reference, resourceUuid);

            response.setSuccessResponse("Artifact updated");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
            return ErrConfMgr.INSTANCE.handleException(e,  ErrConfMgr.ApiType.ATTACH_TO_SERVICE);
        }
    }

    public ResponseEntity getAttachedService(String vfcmtUuid, String requestId) {

        MessageResponse response = new MessageResponse();

        try {
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
            checkVfcmtType(vfcmt);
            String artifact = "No Artifacts";

            if (!CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
                Artifact artifactObj = vfcmt.getArtifacts().stream().filter(a -> DcaeBeConstants.Composition.fileNames.SVC_REF.equals(a.getArtifactName())).findAny().orElse(null);
                if (null != artifactObj) {
                    artifact = getSdcRestClient().getResourceArtifact(vfcmtUuid, artifactObj.getArtifactUUID(), requestId);
                }
            }
            response.setSuccessResponse(artifact);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
            return ErrConfMgr.INSTANCE.handleException(e,  ErrConfMgr.ApiType.GET_VFCMT);
        }
    }

    /**** PRIVATE METHODS ****/

    private void updateReferenceArtifact(String userId, String VFCMTUuid, Artifact artifactObj, String reference, String requestId) throws Exception {
        artifactObj.setDescription("updateReferenceArtifact");
        artifactObj.setPayloadData(Base64Utils.encodeToString(reference.getBytes()));
        getSdcRestClient().updateResourceArtifact(userId, VFCMTUuid, artifactObj, requestId);
    }



    //TODO move method to ci tests
    public List<DcaeMinimizedService> parseAndFilterServicesByUser(String lastUpdaterUserId, List<LinkedHashMap<String, String>> services, String userId) {
        List<DcaeMinimizedService> uuids = null;
        if (services != null) {
            //services.stream().filter(predicate)
            uuids = services.stream()
                    .map(x -> new DcaeMinimizedService(x.get("uuid"), x.get("name"), x.get("lastUpdaterUserId"), x.get("lifecycleState"), x.get("version"), x.get("invariantUUID")))
                    .collect(Collectors.groupingBy(DcaeMinimizedService::getInvariantUUID)).values().stream()
                    .map(p -> p.stream()
                            .sorted(Comparator.comparing(DcaeMinimizedService::getVersionAsFloat).reversed())).map(p -> p.collect(Collectors.toList())).map(p -> p.get(0))
                    .filter(x -> (!(!x.getLastUpdaterUserId().equals(userId) && x.getLifeCycleState().equals(DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name()))))
                    .sorted(Comparator.comparing(DcaeMinimizedService::getName)).collect(Collectors.toList());
        }
        return uuids;
    }

    private List<Service> filterServicesByUser(List<Service> services, String userId) {
        return CollectionUtils.isEmpty(services) ? new ArrayList<>() : services.stream()
                .collect(Collectors.groupingBy(Service::getInvariantUUID)).values().stream()
                .map(p -> p.stream()
                        .sorted(Comparator.comparing(Service::versionAsFloat).reversed())).map(p -> p.collect(Collectors.toList())).map(p -> p.get(0))
                .filter(x -> (!(!x.getLastUpdaterUserId().equals(userId) && x.getLifecycleState().equals(DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name()))))
                .sorted(Comparator.comparing(Service::getName)).collect(Collectors.toList());
    }


    private void verifyVfiExists(String serviceUuid, String vfiName, String requestId) throws Exception {
        ServiceDetailed service = getSdcRestClient().getService(serviceUuid, requestId);
        boolean isServiceContainsVfi = null != service && !CollectionUtils.isEmpty(service.getResources()) && service.getResources().stream()
                .filter(vfi -> "VF".equals(vfi.getResoucreType()))
                .anyMatch(vfi -> vfiName.equals(vfi.getResourceInstanceName()));
        if (!isServiceContainsVfi) {
            ResponseFormat responseFormat = ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.VFI_FETCH_ERROR, null, serviceUuid, vfiName);
            throw new DcaeException(HttpStatus.NOT_FOUND, responseFormat.getRequestError());
        }
    }
}

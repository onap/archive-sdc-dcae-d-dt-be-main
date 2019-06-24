package org.onap.sdc.dcae.composition.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.StringUtils;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.ImportVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.ReferenceUUID;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ExternalReferencesMap;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT;
import static org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum.findState;

@Component
public class VfcmtBusinessLogic extends BaseBusinessLogic {

    private static final String TEMPLATE = "Template";
    private static final String MONITORING_TEMPLATE = "Monitoring Template";
    private static final String DEFAULTICON = "defaulticon";
    private static final String VENDOR_NAME = "vendorName";
    private static final String VENDOR_RELEASE = "vendorRelease";

    public ResponseEntity createMcFromTemplate(String userId, CreateVFCMTRequest request, String requestId) {
        if(!validateMCRequestFields(request)) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Missing information");
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_CONTENT);
        }
        return cloneMcAndAddServiceReference(userId, request, requestId, false);
    }

    //1806 US388513 collect existing VFCMT data - flowType from cdump artifact and external reference from svc_reference artifact. If cdump not found - return error
    public ResponseEntity getVfcmtReferenceData(String vfcmtUuid, String requestId) {
        ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
        Artifact artifactData = fetchCdump(vfcmt, requestId);
        if(null == artifactData) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"No composition found on vfcmt {}", vfcmtUuid);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", vfcmt.getName());
        }
        VfcmtData vfcmtData = new VfcmtData(vfcmt);
        //fetch cdump payload
        String payload = artifactData.getPayloadData();
        //extract and set flowType from cdump payload
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Looking for flowType definition in cdump");
        vfcmtData.setFlowType(StringUtils.substringBetween(payload,"\"flowType\":\"","\""));
        //find svc_reference
        artifactData = findArtifactDataByArtifactName(vfcmt, DcaeBeConstants.Composition.fileNames.SVC_REF);
        if(null != artifactData) {
            //fetch svc_reference payload
            payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, artifactData.getArtifactUUID(), requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Looking for attached service and vfi info in svc_reference");
            //extract and set serviceUuid from svc_reference payload
            vfcmtData.setServiceUuid(StringUtils.substringBefore(payload, "/"));
            //extract and set vfiName from svc_reference payload
            vfcmtData.setVfiName(StringUtils.substringAfterLast(payload, "/"));
        }
        return new ResponseEntity<>(vfcmtData, HttpStatus.OK);
    }

    //1806 US388525 import or clone VFCMT - always pass the flowType - update will only take place if missing from cdump
    public ResponseEntity importMC(String userId, ImportVFCMTRequest request, String requestId) {
        if(!validateMCRequestFields(request)) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Missing information");
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_CONTENT);
        }
        // option 1 - clone
        if(request.isCloneVFCMT()) {
            return cloneMcAndAddServiceReference(userId, request, requestId, request.isUpdateFlowType());
        }

        ResourceDetailed vfcmt = null;
        boolean undoCheckoutOnFailure = false;
        // fetch vfcmt and cdump
        try {
            vfcmt = sdcRestClient.getResource(request.getTemplateUuid(), requestId);
            Artifact cdumpArtifactData = fetchCdump(vfcmt, requestId);
            if (null == cdumpArtifactData) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "No cdump found for monitoring component {}", vfcmt.getUuid());
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", vfcmt.getName());
            }

            String cdumpPayload = cdumpArtifactData.getPayloadData();

            // option 2 - edit original cdump - requires check out
            if(request.isUpdateFlowType()) {
                if(DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT != DcaeBeConstants.LifecycleStateEnum.findState(vfcmt.getLifecycleState())) {
                    vfcmt = sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CHECKOUT.name(), "checking out VFCMT", requestId);
                    undoCheckoutOnFailure = true;
                }

                cdumpArtifactData.setDescription("updating flowType on cdump");
                cdumpPayload = cdumpArtifactData.getPayloadData().replaceFirst("\\{", "{\"flowType\":\"" + request.getFlowType() + "\",");
                cdumpArtifactData.setPayloadData(Base64Utils.encodeToString(cdumpPayload.getBytes()));
                sdcRestClient.updateResourceArtifact(userId, vfcmt.getUuid(), cdumpArtifactData, requestId);
            }
            // option 3 - update service reference only
            updateReferenceToService(userId, request, vfcmt.getUuid(), requestId);
            if(DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT == DcaeBeConstants.LifecycleStateEnum.findState(vfcmt.getLifecycleState())) {
                // this will not throw an exception
                checkinVfcmtAfterClone(userId, vfcmt, requestId);
            }
            return new ResponseEntity<>(buildVfcmtAndCdumpResponse(new VfcmtData(vfcmt, request.getVfiName(), extractFlowTypeFromCdump(cdumpPayload)), cdumpPayload), HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR,this.getClass().getName(),"Failed updating Monitoring Component:{}", e.getMessage());
            if(undoCheckoutOnFailure) {
                rollBack(userId, vfcmt, requestId);
            }
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.CREATE_NEW_VFCMT);
        }
    }

    private boolean validateMCRequestFields(CreateVFCMTRequest request) {
        return Stream.of(request.getFlowType(), request.getTemplateUuid(), request.getName(), request.getDescription(), request.getContextType(), request.getServiceUuid(), request.getVfiName())
                .allMatch(StringUtils::isNotBlank);
    }

    private void updateReferenceToService(String userId, CreateVFCMTRequest request, String newVfcmtUuid, String requestId) {
        String serviceUuid = request.getServiceUuid();
        String vfiName = request.getVfiName();

        debugLogger.log(LogLevel.INFO, this.getClass().getName(),"About to update service {}/{} to monitoring component {} ", serviceUuid, vfiName, request.getName());

        sdcRestClient.addExternalMonitoringReference(userId, request, new ReferenceUUID(newVfcmtUuid), requestId);

    }


    private ResponseEntity cloneMcAndAddServiceReference(String userId, CreateVFCMTRequest request, String requestId, boolean updateFlowType) {
        addSdcMandatoryFields(request, userId);
        ResourceDetailed newVfcmt = null;
        try {
            // Retrieve the Template VFCMT from SDC - use the template UUID provided from UI
            ResourceDetailed templateMC = sdcRestClient.getResource(request.getTemplateUuid(), requestId);
            // Download the CDUMP file from the template VFCMT
            Artifact cdumpArtifactData = updateFlowType ? fetchCdumpAndSetFlowType(templateMC, request.getFlowType(), requestId) : fetchCdump(templateMC, requestId);
            if (null == cdumpArtifactData) {
                errLogger.log(LogLevel.ERROR,this.getClass().getName(),"No cdump found for template {} while creating monitoring component", templateMC.getUuid());
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", templateMC.getName());
            }
            newVfcmt = sdcRestClient.createResource(userId, request, requestId);
            // The cdump has the original template id. we need to replace it with the new vfcmt id
            String newVfcmtUuid = newVfcmt.getUuid();
            String cdumpPayload = cdumpArtifactData.getPayloadData().replaceAll(templateMC.getUuid(), newVfcmtUuid);

            // Upload it to newly created VFCMT
            cloneArtifactToTarget(userId, newVfcmtUuid, cdumpPayload, cdumpArtifactData, requestId);
            cloneRuleArtifacts(userId, templateMC, newVfcmtUuid, requestId);
            createReferenceArtifact(userId, request, newVfcmtUuid, requestId);
            updateReferenceToService(userId, request, newVfcmtUuid, requestId);

            // this will not throw an exception
            checkinVfcmtAfterClone(userId, newVfcmt, requestId);
            return new ResponseEntity<>(buildVfcmtAndCdumpResponse(new VfcmtData(newVfcmt, request.getVfiName(), extractFlowTypeFromCdump(cdumpPayload)), cdumpPayload), HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR,this.getClass().getName(),"Failed creating Monitoring Component:{}", e.getMessage());
            rollBack(userId, newVfcmt, requestId);
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.CREATE_NEW_VFCMT);
        }
    }


    private void checkinVfcmtAfterClone(String userId, ResourceDetailed vfcmt, String requestId) {
        try {
            vfcmt = sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CHECKIN.getValue(), "check in after clone", requestId);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR,this.getClass().getName(),"Failed to check in Monitoring Component: {}. message: {}", vfcmt.getUuid(), e);
        }
    }

    private void cloneRuleArtifacts(String userId, ResourceDetailed templateMC, String newVfcmtUuid, String requestId) throws JsonProcessingException {
        // handle rule artifacts using java 7 for-loop - exception propagation to calling method
        for(Artifact artifact : templateMC.getArtifacts()) {
            if(artifact.getArtifactName().endsWith(DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX)) {
                cloneArtifactToTarget(userId, newVfcmtUuid, sdcRestClient.getResourceArtifact(templateMC.getUuid(), artifact.getArtifactUUID(), requestId), artifact, requestId);
            }
        }
    }

    // fetch the vfcmt cdump artifact payload and insert the flowType. Return the artifact with updated payload or null (artifact doesn't exist)
    private Artifact fetchCdumpAndSetFlowType(ResourceDetailed vfcmt, String flowType, String requestId) throws IOException {
        Artifact cdumpArtifactData = fetchCdump(vfcmt, requestId);
        if (null != cdumpArtifactData && null != cdumpArtifactData.getPayloadData() && !cdumpArtifactData.getPayloadData().contains("\"flowType\":\"")) {
            String cdumpPayload = cdumpArtifactData.getPayloadData().replaceFirst("\\{", "{\"flowType\":\"" + flowType + "\",");
            cdumpArtifactData.setPayloadData(cdumpPayload);
        }
        return cdumpArtifactData;
    }

    // backward compatibility (very backward)
    private void createReferenceArtifact(String userId, CreateVFCMTRequest request, String newVfcmtUuid, String requestId) throws JsonProcessingException {
        String referencePayload = request.getServiceUuid() + "/resources/" + request.getVfiName();
        Artifact refArtifact = SdcRestClientUtils.generateDeploymentArtifact("createReferenceArtifact", DcaeBeConstants.Composition.fileNames.SVC_REF, ArtifactType.DCAE_TOSCA.name(), "servicereference", referencePayload.getBytes());
        sdcRestClient.createResourceArtifact(userId, newVfcmtUuid, refArtifact, requestId);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Artifact {} created with content: {}", DcaeBeConstants.Composition.fileNames.SVC_REF, referencePayload);
    }

    public ResponseEntity getVfcmtsForMigration(String userId, String contextType, String uuid, String version,
                                                String requestId) {
        List<Resource> resources;
        ExternalReferencesMap connectedVfcmts;
        try {
            connectedVfcmts = getSdcRestClient().getMonitoringReferences(contextType, uuid, version, requestId);
            resources = getSdcRestClient().getResources(AssetType.VFCMT.name(), TEMPLATE, MONITORING_TEMPLATE, requestId);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR,this.getClass().getName(),"Exception getVfcmtsForMigration {}", e);
            debugLogger.log(LogLevel.DEBUG,this.getClass().getName(),"Exception getVfcmtsForMigration {}", e);
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_ALL_VFCMTS);
        }

        List<Resource> vfcmts = resources.stream()
                .filter(resource -> notCheckedOutOrMine(userId, resource))
                .filter(resource -> !connected(resource, connectedVfcmts))
                .collect(Collectors.toList());
        return new ResponseEntity<>(vfcmts, HttpStatus.OK);
    }

    private boolean connected(Resource resource, ExternalReferencesMap connectedVfcmts){
        return connectedVfcmts.values().stream().anyMatch(p -> p.contains(resource.getUuid()));
    }

    private boolean notCheckedOutOrMine(String userId, Resource resource) {
        // if the resource belongs to this user then it is kosher
        // or if it doesn't belong to the user check the lifecycle state is checked out

        return resource.getLastUpdaterUserId().equalsIgnoreCase(userId) ||
               NOT_CERTIFIED_CHECKOUT != findState(resource.getLifecycleState());
    }


    public void addSdcMandatoryFields(CreateVFCMTRequest createRequest, String user) {
        createRequest.setContactId(user);
        createRequest.setIcon(DEFAULTICON);
        createRequest.setResourceType(AssetType.VFCMT.name());
        createRequest.setVendorName(VENDOR_NAME);
        createRequest.setVendorRelease(VENDOR_RELEASE);
        if (StringUtils.isBlank(createRequest.getCategory())) {
            createRequest.setCategory(TEMPLATE);
        }
        if (StringUtils.isBlank(createRequest.getSubcategory())) {
            createRequest.setSubcategory(MONITORING_TEMPLATE);
        }
        createRequest.setTags(new String[]{createRequest.getName()});
    }

}

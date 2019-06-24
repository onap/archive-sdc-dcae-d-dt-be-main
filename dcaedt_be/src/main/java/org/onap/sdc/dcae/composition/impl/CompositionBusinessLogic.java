package org.onap.sdc.dcae.composition.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.CreateMcResponse;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceInstance;
import org.onap.sdc.dcae.composition.restmodels.sdc.ServiceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.utils.Normalizers;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class CompositionBusinessLogic extends BaseBusinessLogic {

    private static final String CREATE_DESC = "creating new artifact blueprint on the service vfi";
    private static final String UPDATE_DESC = "updating artifact blueprint on the service vfi";

	//canvas cdump as simple string
    public Artifact getComposition(String vfcmtUuid, String requestId) {
		ResourceDetailed vfcmt = sdcRestClient.getResource(vfcmtUuid, requestId);
		return fetchCdump(vfcmt, requestId);
	}

	//cdump and vfcmt for monitoring configuration
    public CreateMcResponse getDataAndComposition(String vfcmtUuid, String requestId) throws IOException {
        ResourceDetailed vfcmt = sdcRestClient.getResource(vfcmtUuid, requestId);
        Artifact composition = fetchCdump(vfcmt, requestId);
        return buildVfcmtAndCdumpResponse(new VfcmtData(vfcmt), composition.getPayloadData());
    }

    // 1806 US399018 update composition - assumes an artifact already exists (create mode flag for backward compatibility)
    public ResponseEntity saveComposition(String userId, String vfcmtUuid, String updatedPayload, String requestId, boolean allowCreateNew) {

        boolean undoCheckoutOnFailure = false;
        ResourceDetailed vfcmt = null;
        try {
            vfcmt = sdcRestClient.getResource(vfcmtUuid, requestId);
            Artifact artifactData = findCdumpArtifactData(vfcmt);
            if (null == artifactData && !allowCreateNew) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Composition not found on vfcmt {}", vfcmt.getUuid());
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", vfcmt.getName());
            }
            if (DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT != DcaeBeConstants.LifecycleStateEnum.findState(vfcmt.getLifecycleState())) {
                vfcmt = sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CHECKOUT.name(), null, requestId);
                undoCheckoutOnFailure = true;
            }
            if (null == artifactData) {
                artifactData = SdcRestClientUtils.generateDeploymentArtifact("creating composition file", DcaeBeConstants.Composition.fileNames.COMPOSITION_YML, ArtifactType.DCAE_TOSCA.name(), "composition", updatedPayload.getBytes());
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "creating new composition artifact for MC: {}", vfcmt.getUuid());
                sdcRestClient.createResourceArtifact(userId, vfcmt.getUuid(), artifactData, requestId);
            } else {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "updating composition artifact for MC: {}", vfcmt.getUuid());
                artifactData.setDescription("updating composition");
                artifactData.setPayloadData(Base64Utils.encodeToString(updatedPayload.getBytes()));
                sdcRestClient.updateResourceArtifact(userId, vfcmt.getUuid(), artifactData, requestId);
            }
            return new ResponseEntity<>(sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CHECKIN.name(), "auto checkin after save composition", requestId), HttpStatus.OK);
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Save composition failed: {}", e);
            if(undoCheckoutOnFailure) {
                rollBack(userId, vfcmt, requestId);
            }
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_CDUMP);
        }
    }

    // 1810 US436244 Update MC table version representations and actions
    public ResponseEntity overwriteRevertedMC(String userId, String vfcmtUuid, String revertedUuid, String updatedPayload, String requestId) {
    	try {
    		cloneArtifactsToRevertedMC(userId, vfcmtUuid, revertedUuid, requestId, false);
		}  catch (Exception e) {
		    errLogger.log(LogLevel.ERROR, this.getClass().getName(), "clone action failed: {}", e);
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_CDUMP);
		}
    	return saveComposition(userId, revertedUuid, updatedPayload, requestId, false);
	}

    Artifact submitComposition(String userId, String context, VfcmtData vfcmtData, String resultBlueprintCreation, String requestId) throws JsonProcessingException {

        // get service / find vfi
        ServiceDetailed service = sdcRestClient.getAssetMetadata(context, vfcmtData.getServiceUuid(), requestId);
        ResourceInstance vfi = findVfiOnService(service, vfcmtData.getVfiName());
        if(null == vfi){
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "VF instance {} not found on service {}", vfcmtData.getVfiName(), vfcmtData.getServiceUuid());
            return null;
        }
        // look for existing blueprint details
		//1802,1806 US412711 - Allow multiple BP from the same flow type on a single VFi - identify existing blueprint by name - for backward compatibility
		String artifactName = generateBlueprintFileName(vfcmtData.getFlowType(), vfcmtData.getName());
        Artifact blueprintArtifact = findExistingBlueprint(vfi, artifactName);

        // save blueprint as instance artifact
        // create mode
        if(null == blueprintArtifact) {
			String artifactLabel = "blueprint-".concat(vfcmtData.getFlowType()).concat("-").concat(Normalizers.normalizeComponentName(vfcmtData.getName()));
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Service {} does not consist {} ----> createMode", vfcmtData.getServiceUuid(), artifactLabel);
            blueprintArtifact = SdcRestClientUtils.generateDeploymentArtifact(CREATE_DESC, artifactName , ArtifactType.DCAE_INVENTORY_BLUEPRINT.name(), artifactLabel, resultBlueprintCreation.getBytes());
            blueprintArtifact = sdcRestClient.createInstanceArtifact(userId, context, vfcmtData.getServiceUuid(), Normalizers.normalizeComponentInstanceName(vfcmtData.getVfiName()), blueprintArtifact, requestId);
            // update mode
        } else {
            blueprintArtifact = updateBlueprint(userId, context, vfcmtData.getServiceUuid(), vfcmtData.getVfiName(), blueprintArtifact, resultBlueprintCreation, requestId);
            // TODO should this be safe?
            //1806 US390018 - remove reference to previous version
            deletePreviousReference(userId, context, service, vfcmtData, requestId);
        }
        //1806 US374593 - certify VFCMT after BP generation
        certifyVfcmt(userId, vfcmtData, requestId);
        return blueprintArtifact;
    }


    private Artifact updateBlueprint(String userId, String context, String serviceUuid, String vfiName, Artifact blueprintArtifact, String payload, String requestId) throws JsonProcessingException {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Found that service {} already consist of {} ----> updateMode", serviceUuid, blueprintArtifact.getArtifactLabel());
        blueprintArtifact.setDescription(UPDATE_DESC);
        blueprintArtifact.setPayloadData(Base64Utils.encodeToString(payload.getBytes()));
        return sdcRestClient.updateInstanceArtifact(userId, context, serviceUuid, Normalizers.normalizeComponentInstanceName(vfiName), blueprintArtifact, requestId);
    }

	private Artifact findExistingBlueprint(ResourceInstance vfi, String artifactName) {
		return CollectionUtils.isEmpty(vfi.getArtifacts()) ? null : vfi.getArtifacts().stream()
				.filter(p -> Normalizers.normalizeArtifactLabel(artifactName).equals(Normalizers.normalizeArtifactLabel(p.getArtifactName())))
				.findAny()
				.orElse(null);
	}

    //TODO should this be safe or throw?
    private void deletePreviousReference(String userId, String context, ServiceDetailed service, VfcmtData newReferencedMc, String requestId) {
    	String normalizedInstanceName = Normalizers.normalizeComponentInstanceName(newReferencedMc.getVfiName());
        List<String> vfiRefs = sdcRestClient.getMonitoringReferences(context, service.getUuid(), service.getVersion(), requestId).get(normalizedInstanceName);
        if (null != vfiRefs && 1 < vfiRefs.size()) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Found {} external monitoring references for vfi {} on service {}:{}", vfiRefs.size(), newReferencedMc.getVfiName(), service.getUuid(), vfiRefs);
            Collections.synchronizedList(vfiRefs).parallelStream()
                    .filter(p -> !newReferencedMc.getUuid().equals(p) && !p.endsWith(REVERTED_REF))
                    .filter(p -> newReferencedMc.getInvariantUUID().equals(sdcRestClient.getResource(p, requestId).getInvariantUUID()))
                    .forEach(id -> sdcRestClient.deleteExternalMonitoringReference(userId, context, service.getUuid(), normalizedInstanceName, id, requestId));
        } else {
			// this shouldn't happen - there should be at least two references registered to the vfi
            debugLogger.log(LogLevel.WARN, this.getClass().getName(), "Sum Ting Wong. References found: {}", vfiRefs);
        }
    }

    private void certifyVfcmt(String userId, VfcmtData vfcmt, String requestId) {
        String state = vfcmt.getLifecycleState();
        if(null == state) {
            debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "Couldn't read Vfcmt lifecycle state");
            return;
        }
        DcaeBeConstants.LifecycleStateEnum lifeCycleState = DcaeBeConstants.LifecycleStateEnum.findState(state);
        if(null == lifeCycleState) {
            debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "Undefined lifecycle state: {}", state);
            return;
        }
        try{
            switch (lifeCycleState){
            case NOT_CERTIFIED_CHECKOUT:
                sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CHECKIN.name(), "check in VFCMT after blueprint successful submission", requestId);
                sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CERTIFY.name(), "certify VFCMT after blueprint successful submission", requestId);
                break;
            case NOT_CERTIFIED_CHECKIN:
                sdcRestClient.changeResourceLifecycleState(userId, vfcmt.getUuid(), LifecycleOperationType.CERTIFY.name(), "certify VFCMT after blueprint successful submission", requestId);
                break;
            default:
            }
        }
        catch (Exception e){
            //informative only. no message to user (TBA)
            debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred during vfcmt lifecycle operation: {}", e);
        }
    }

}

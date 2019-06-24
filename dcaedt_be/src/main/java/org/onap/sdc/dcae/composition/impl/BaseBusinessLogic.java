package org.onap.sdc.dcae.composition.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.CreateMcResponse;
import org.onap.sdc.dcae.composition.restmodels.ReferenceUUID;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.DcaeException;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.utils.Normalizers;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@Component
public class BaseBusinessLogic {

	@Autowired
	protected SystemProperties systemProperties;

    @Autowired
    protected ISdcClient sdcRestClient;

    protected static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    protected static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	protected static final String REVERTED_REF = "_reverted";

    public ISdcClient getSdcRestClient() {
        return sdcRestClient;
    }

    public SystemProperties getSystemProperties() {
    	return systemProperties;
	}

    void setSdcRestClient(ISdcClient sdcRestClient) {
        this.sdcRestClient = sdcRestClient;
    }

    Artifact cloneArtifactToTarget(String userId, String targetId, String payload, Artifact artifactToClone, String requestId) throws JsonProcessingException {
        Artifact cloned = SdcRestClientUtils.generateDeploymentArtifact(artifactToClone.getArtifactDescription(), artifactToClone.getArtifactName(), artifactToClone.getArtifactType(), artifactToClone.getArtifactLabel(), payload.getBytes());
        return sdcRestClient.createResourceArtifact(userId, targetId, cloned, requestId);
    }

    public void undoRevert(String userId, String contextType, String serviceUuid, String vfiName, String revertedUuid, String requestId) {
		sdcRestClient.updateExternalMonitoringReference(userId, contextType, serviceUuid, vfiName, revertedUuid.concat(REVERTED_REF), new ReferenceUUID(revertedUuid), requestId);
	}

	// 1810 US436244 Update MC table version representations and actions
	void cloneArtifactsToRevertedMC(String userId, String vfcmtUuid, String revertedUuid, String requestId, boolean cloneComposition) throws JsonProcessingException {
		List<String> exclude = new ArrayList<>();
		exclude.add(DcaeBeConstants.Composition.fileNames.SVC_REF);
		if(!cloneComposition) {
			exclude.add(DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
		}
		ResourceDetailed sourceVfcmt = sdcRestClient.getResource(vfcmtUuid, requestId);
		ResourceDetailed targetVfcmt = sdcRestClient.getResource(revertedUuid, requestId);
		Map<String, Artifact> currentArtifacts = targetVfcmt.getArtifacts().stream().collect(Collectors.toMap(Artifact::getArtifactName, Function.identity()));
		debugLogger.log(LogLevel.DEBUG,this.getClass().getName(), "latest MC version artifact names: {}", currentArtifacts.keySet());
		Predicate<Artifact> predicate = p -> !exclude.contains(p.getArtifactName()) && (null == currentArtifacts.get(p.getArtifactName()) || !currentArtifacts.get(p.getArtifactName()).getArtifactChecksum().equals(p.getArtifactChecksum()));
		List<Artifact> artifactsToClone = sourceVfcmt.getArtifacts().stream().filter(predicate).collect(Collectors.toList());
		debugLogger.log(LogLevel.DEBUG,this.getClass().getName(), "submitted MC version artifacts to clone or overwrite on latest MC: {}", artifactsToClone.stream().map(Artifact::getArtifactName).collect(Collectors.toList()));
		// clone source artifacts to target
		if(!artifactsToClone.isEmpty()) {
			checkVfcmtType(targetVfcmt);
			checkUserIfResourceCheckedOut(userId, targetVfcmt);
			if (isNeedToCheckOut(targetVfcmt.getLifecycleState())) {
				targetVfcmt = sdcRestClient.changeResourceLifecycleState(userId, revertedUuid, LifecycleOperationType.CHECKOUT.name(), "checking out VFCMT before clone", requestId);
			}
			for (Artifact artifactToClone : artifactsToClone) {
				String payload = sdcRestClient.getResourceArtifact(vfcmtUuid, artifactToClone.getArtifactUUID(), requestId);
				cloneArtifactToTarget(userId, revertedUuid, payload, artifactToClone, currentArtifacts.get(artifactToClone.getArtifactName()), requestId);
			}
		}
        // delete any target artifacts that do not match source artifacts
		List<String> artifactsNames = sourceVfcmt.getArtifacts().stream().map(Artifact::getArtifactName).collect(Collectors.toList());
		targetVfcmt.getArtifacts().stream().filter(p -> !artifactsNames.contains(p.getArtifactName())).forEach(a ->
			sdcRestClient.deleteResourceArtifact(userId, revertedUuid, a.getArtifactUUID(), requestId)
		);
	}

    private void cloneArtifactToTarget(String userId, String targetId, String payload, Artifact artifactToClone, Artifact artifactToOverride, String requestId) throws JsonProcessingException {
        if (null != artifactToOverride) {
            artifactToOverride.setDescription(artifactToOverride.getArtifactDescription());
            artifactToOverride.setPayloadData(Base64Utils.encodeToString(payload.getBytes()));
            sdcRestClient.updateResourceArtifact(userId, targetId, artifactToOverride, requestId);
        } else {
            cloneArtifactToTarget(userId, targetId, payload, artifactToClone, requestId);
        }
    }

    Artifact findArtifactDataByArtifactName(ResourceDetailed vfcmt, String artifactName) {
        if (null == vfcmt || null == vfcmt.getArtifacts()) {
            return null;
        }
        return vfcmt.getArtifacts().stream()
                .filter(p -> artifactName.equals(p.getArtifactName())).findAny().orElse(null);
    }

    Artifact findCdumpArtifactData(ResourceDetailed vfcmt) {
        return findArtifactDataByArtifactName(vfcmt, DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
    }

    void rollBack(String userId, ResourceDetailed newVfcmt, String requestId) {
        if (null != newVfcmt) {
            try {
                sdcRestClient.changeResourceLifecycleState(userId, newVfcmt.getUuid(), LifecycleOperationType.UNDO_CHECKOUT.getValue(), "DCAE rollback", requestId);
            } catch (Exception e) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(),"Failed rolling back Monitoring Component. ID:{}", newVfcmt.getUuid());
                debugLogger.log(LogLevel.ERROR, this.getClass().getName(),"Failed rolling back Monitoring Component:{}", e);
            }
        }
    }

    CreateMcResponse buildVfcmtAndCdumpResponse(VfcmtData vfcmt, String cdumpPayload) throws IOException {
        return new CreateMcResponse(vfcmt, new ObjectMapper().readValue(cdumpPayload, Object.class));
    }

    Artifact fetchCdump(ResourceDetailed vfcmt, String requestId) {
        Artifact cdumpArtifactData = findCdumpArtifactData(vfcmt);
        if (null != cdumpArtifactData) {
            String cdumpPayload = sdcRestClient.getResourceArtifact(vfcmt.getUuid(), cdumpArtifactData.getArtifactUUID(), requestId);
            cdumpArtifactData.setPayloadData(cdumpPayload);
        }
        return cdumpArtifactData;
    }

    String generateBlueprintFileName(String monitoringFlowType, String vfcmtName) {
        return monitoringFlowType
                .concat(".")
                .concat(Normalizers.normalizeComponentName(vfcmtName))
                .concat(".")
                .concat(DcaeBeConstants.Composition.fileNames.EVENT_PROC_BP_YAML);
    }

    ResourceInstance findVfiOnService(ServiceDetailed service, String vfiName) {
        if (null == service || null == service.getResources()) {
            return null;
        }
        return service.getResources().stream()
                .filter(p -> Normalizers.normalizeComponentInstanceName(vfiName).equals(Normalizers.normalizeComponentInstanceName(p.getResourceInstanceName()))).findAny().orElse(null);
    }

    String extractFlowTypeFromCdump(String cdump) {
        return StringUtils.substringBetween(cdump,"\"flowType\":\"","\"");
    }

    public ResourceDetailed checkinVfcmt(String userId, String uuid, String requestId) {
        return getSdcRestClient().changeResourceLifecycleState(userId, uuid, LifecycleOperationType.CHECKIN.name(), "checking in vfcmt"  + uuid, requestId);
    }
    public ResourceDetailed checkoutVfcmt(String userId, String uuid, String requestId) {
        return getSdcRestClient().changeResourceLifecycleState(userId, uuid, LifecycleOperationType.CHECKOUT.name(), null, requestId);
    }


    void checkUserIfResourceCheckedOut(String userId, Asset asset) {
        if (DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT == DcaeBeConstants.LifecycleStateEnum.findState(asset.getLifecycleState())) {
            String lastUpdaterUserId = asset.getLastUpdaterUserId();
            if (lastUpdaterUserId != null && !lastUpdaterUserId.equals(userId)) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "User conflicts. Operation not allowed for user {} on resource checked out by {}", userId, lastUpdaterUserId);
                ResponseFormat responseFormat = ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.USER_CONFLICT, null, userId, asset.getName(), lastUpdaterUserId);
                throw new DcaeException(HttpStatus.FORBIDDEN, responseFormat.getRequestError());
            }
        }
    }

    boolean isNeedToCheckOut(String lifecycleState) {
        return DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT != DcaeBeConstants.LifecycleStateEnum.findState(lifecycleState);
    }

    void checkVfcmtType(ResourceDetailed vfcmt) {
        if (AssetType.VFCMT != getValidAssetTypeOrNull(vfcmt.getResourceType()) || !"Template".equals(vfcmt.getCategory())) {
            ResponseFormat responseFormat = ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.RESOURCE_NOT_VFCMT_ERROR, null, vfcmt.getUuid());
            throw new DcaeException(HttpStatus.BAD_REQUEST, responseFormat.getRequestError());
        }
    }

    public AssetType getValidAssetTypeOrNull(String type) {
        try {
            return AssetType.getAssetTypeByName(type);
        } catch (IllegalArgumentException e) {
            debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "invalid asset type: {}. Error: {}", type, e);
            return null;
        }
    }


	byte[] extractFile(ZipInputStream zis) throws IOException {
		byte[] buffer = new byte[1024];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()){
			int len;
			while ((len = zis.read(buffer)) > 0) {
				os.write(buffer, 0, len);
			}
			return os.toByteArray();
		}
	}
}

package org.onap.sdc.dcae.composition.impl;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

@Component
public class BaseBusinessLogic {
    @Autowired
    protected ISdcClient sdcRestClient;

    protected static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    protected static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	public ISdcClient getSdcRestClient() {
		return sdcRestClient;
	}

	void setSdcRestClient(ISdcClient sdcRestClient) {
		this.sdcRestClient = sdcRestClient;
	}

	Artifact cloneArtifactToTarget(String userId, String targetId, String payload, Artifact artifactToClone, String requestId) throws Exception {
		Artifact cloned = SdcRestClientUtils.generateDeploymentArtifact(artifactToClone.getArtifactDescription(), artifactToClone.getArtifactName(), artifactToClone.getArtifactType(), artifactToClone.getArtifactLabel(), payload.getBytes());
		return sdcRestClient.createResourceArtifact(userId, targetId, cloned, requestId);
	}

	public void cloneArtifactToTarget(String userId, String targetId, String payload, Artifact artifactToClone, Artifact artifactToOverride, String requestId) throws Exception{
		if (null != artifactToOverride) {
			artifactToOverride.setDescription(artifactToOverride.getArtifactDescription());
			artifactToOverride.setPayloadData(Base64Utils.encodeToString(payload.getBytes()));
			sdcRestClient.updateResourceArtifact(userId, targetId, artifactToOverride, requestId);
		} else {
			cloneArtifactToTarget(userId, targetId, payload, artifactToClone, requestId);
		}
	}

	Artifact findArtifactDataByArtifactName(ResourceDetailed vfcmt, String artifactName) {
		return null == vfcmt ? null : CollectionUtils.isEmpty(vfcmt.getArtifacts()) ? null : vfcmt.getArtifacts().stream()
				.filter(p -> artifactName.equals(p.getArtifactName())).findAny().orElse(null);
	}
}

package org.onap.sdc.dcae.composition.impl;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.MonitoringComponent;
import org.onap.sdc.dcae.composition.restmodels.sdc.ExternalReferencesMap;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceInstance;
import org.onap.sdc.dcae.composition.restmodels.sdc.ServiceDetailed;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.utils.Normalizers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class ReferenceBusinessLogic extends BaseBusinessLogic {

    public ResponseEntity deleteVfcmtReferenceBlueprint(String userId, String context, String monitoringComponentName, String serviceUuid, String vfiName, String vfcmtUuid, String requestId) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Going to delete blueprint, monitoringComponentName = {}, vfiName = {}", monitoringComponentName, vfiName);
        try {
            String normalizedVfiName = Normalizers.normalizeComponentInstanceName(vfiName);
            ServiceDetailed serviceDetailed = sdcRestClient.getService(serviceUuid, requestId);
            ResourceInstance resourceInstance = findVfiOnService(serviceDetailed, vfiName);
            if (resourceInstance != null && resourceInstance.getArtifacts() != null) {
                String artifactNameEndsWith = generateBlueprintFileName("", monitoringComponentName);
                resourceInstance.getArtifacts().stream()
                        .filter(item -> StringUtils.endsWithIgnoreCase(item.getArtifactName(), artifactNameEndsWith))
                        .findAny()
                        .ifPresent(artifact -> sdcRestClient.deleteInstanceArtifact(userId, context, serviceUuid, normalizedVfiName, artifact.getArtifactUUID(), requestId));
            }
        } catch (Exception e) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Failed to delete blueprint with serviceUuid {}, vfcmtUuid . message: {} ", serviceUuid, vfcmtUuid, e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.DELETE_BLUEPRINT_FAILED, e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    public void deleteVfcmtReference(String userId, String context, String serviceUuid, String vfiName, String vfcmtUuid, String requestId) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Going to delete vfcmt reference, vfiName = {}", vfiName);
        String normalizedVfiName = Normalizers.normalizeComponentInstanceName(vfiName);
        sdcRestClient.deleteExternalMonitoringReference(userId, context, serviceUuid, normalizedVfiName, vfcmtUuid, requestId);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished to delete vfcmt reference. serviceUuid {}, vfcmtUuid {}", serviceUuid, vfcmtUuid);
    }

    // 1806 US381853 Return a list of monitoring components by external reference id. Support partial success
    public Map<String, List<MonitoringComponent>> fetchMonitoringComponents(ExternalReferencesMap mcRefs, String requestId) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Going to fetch monitoring components metadata for vfis {}", mcRefs.keySet());
        Map<String, List<MonitoringComponent>> result = new LinkedHashMap<>();
        List<MonitoringComponent> monitoringComponents = Collections.synchronizedList(new ArrayList<>());
        List<MonitoringComponent> unavailable = Collections.synchronizedList(new ArrayList<>());
        mcRefs.entrySet().parallelStream().forEach(entry ->
                entry.getValue().parallelStream().forEach(id -> {
                    try{
                        monitoringComponents.add(new MonitoringComponent(getSdcRestClient().getResource(id, requestId), entry.getKey()));
                    } catch (Exception e) {
                        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Failed to fetch monitoring component with uuid {}. message: {} ", id, e);
                        unavailable.add(new MonitoringComponent(id, entry.getKey(), "unavailable"));
                    }

                })
        );
        result.put("monitoringComponents", monitoringComponents);
        if(!isEmpty(unavailable)) {
            result.put("unavailable", unavailable);
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished fetching monitoring components metadata for vfis {}", mcRefs.keySet());
        return result;
    }

}

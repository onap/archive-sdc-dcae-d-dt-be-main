package org.onap.sdc.dcae.composition.impl;

import org.apache.commons.lang.StringUtils;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.catalog.asdc.ASDC;
import org.onap.sdc.dcae.catalog.asdc.ASDCUtils;
import org.onap.sdc.dcae.catalog.asdc.Blueprinter;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.StringReader;
import java.net.URI;

@Component
public class BlueprintBusinessLogic extends CompositionBusinessLogic {

    @Autowired
    private Blueprinter blueprinter;
    @Autowired
    private ASDC asdc;


    @PostConstruct
    public void init() {
        URI sdcUri = URI.create(systemProperties.getProperties().getProperty(DcaeBeConstants.Config.URI));
        asdc.setUri(sdcUri);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "SDC uri: {}", sdcUri);
    }


    public ResponseEntity generateAndSaveBlueprint(String userId, String context, String vfcmtUuid, String serviceUuid, String vfiName, String flowType, String requestId) {
        try {
            // prepare - fetch vfcmt and cdump
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            Artifact cdumpArtifactData = fetchCdump(vfcmt, requestId);
            if (null == cdumpArtifactData) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Composition not found on vfcmt {}", vfcmtUuid);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", vfcmt.getName());
            }
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Found the cdump (composition.yml) on top of VFCMT {}", vfcmtUuid);
            String cdump = cdumpArtifactData.getPayloadData();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Going to use python procedure to create a blueprint....");
            String resultBlueprintCreation = generateBlueprintViaToscaLab(cdump);
            if (StringUtils.isEmpty(resultBlueprintCreation)) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred during blueprint generation");
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERATE_BLUEPRINT_ERROR, "", vfcmt.getName());
            }
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "BLUEPRINT:\n{}", resultBlueprintCreation);

            // 1806 US374595 flowType in cdump
            String flowTypeFromCdump = extractFlowTypeFromCdump(cdump);

            // support backward compatibility
            if(StringUtils.isBlank(flowTypeFromCdump)) {
                flowTypeFromCdump = flowType;
            }

            VfcmtData vfcmtData = new VfcmtData(vfcmt, vfiName, flowTypeFromCdump, serviceUuid);
            Artifact blueprintArtifactResult = submitComposition(userId, context, vfcmtData, resultBlueprintCreation, requestId);
            if (null == blueprintArtifactResult) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SUBMIT_BLUEPRINT_ERROR);
            }

            MessageResponse response = new MessageResponse();
            response.setSuccessResponse("Blueprint build complete \n. Blueprint=" + blueprintArtifactResult.getArtifactName());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SUBMIT_BLUEPRINT);
        }
    }

    private String generateBlueprintViaToscaLab(String cdump) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "---------------------------------------------------------------CDUMP: -----------------------------------------------------------------------------");
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), cdump);
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "---------------------------------------------------------------------------------------------------------------------------------------------------");
        ASDCUtils utils = new ASDCUtils(asdc, blueprinter);
        String resultBlueprintCreation = null;
        try{
            resultBlueprintCreation	= utils.buildBlueprintViaToscaLab(new StringReader(cdump)).waitForResult().waitForResult();
        }catch (Exception e){
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Generate blueprint via tosca lab error: {}", e);
        }
        return resultBlueprintCreation;
    }
}

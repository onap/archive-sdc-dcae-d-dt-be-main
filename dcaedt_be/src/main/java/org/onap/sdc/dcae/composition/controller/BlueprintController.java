package org.onap.sdc.dcae.composition.controller;

import org.apache.commons.lang.StringUtils;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.catalog.asdc.ASDC;
import org.onap.sdc.dcae.catalog.asdc.ASDCUtils;
import org.onap.sdc.dcae.catalog.asdc.Blueprinter;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.utils.Normalizers;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ErrConfMgr.ApiType;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.StringReader;
import java.net.URI;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class BlueprintController extends BaseController{

	@Autowired
	private Blueprinter blueprinter;

	@Autowired
	private ASDC asdc;

	private static final String CREATE_DESC = "creating new artifact blueprint on the service vfi";
	private static final String UPDATE_DESC = "updating artifact blueprint on the service vfi";



	@PostConstruct
	public void init(){
		URI sdcUri = URI.create(systemProperties.getProperties().getProperty(DcaeBeConstants.Config.URI));
		asdc.setUri(sdcUri);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "SDC uri: {}", sdcUri);
	}

	/***
	 * VFCMT - Resource, blueprint - as an artifact as an service. 
	 * @param userId
	 * @param vfcmtUuid
	 * @param serviceUuid
	 * @param serviceInstanceName
	 * @param monitoringFlowType
	 * @return ResponseEntity
	 */
		@RequestMapping(value = "/createBluePrint/{VFCMTUuid}/{serviceUuid}/{instanceName}/{monitoringFlowType}", method = RequestMethod.POST)
		public ResponseEntity createBluePrint(@RequestHeader("USER_ID") String userId,
				@PathVariable("VFCMTUuid") String vfcmtUuid,
				@PathVariable("serviceUuid") String serviceUuid,
				@PathVariable("instanceName") String serviceInstanceName,
				@PathVariable("monitoringFlowType") String monitoringFlowType,
				@ModelAttribute("requestId") String requestId) {
			try {
				
				ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(vfcmtUuid, requestId);
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
				checkVfcmtType(vfcmt);
				Artifact cdumpArtifactData = findCdumpArtifactData(vfcmt);
				if (null != cdumpArtifactData) {
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Found the cdump (composition.yml) on top of VFCMT {}", vfcmtUuid);
					String cdump = baseBusinessLogic.getSdcRestClient().getResourceArtifact(vfcmtUuid, cdumpArtifactData.getArtifactUUID(), requestId);
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "---------------------------------------------------------------CDUMP: -----------------------------------------------------------------------------");
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), cdump);
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "---------------------------------------------------------------------------------------------------------------------------------------------------");
					ASDCUtils utils = new ASDCUtils(asdc, blueprinter);

					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Going to use python procedure to create a blueprint....");
					String resultBlueprintCreation;
					try{
						resultBlueprintCreation	= utils.buildBlueprintViaToscaLab(new StringReader(cdump)).waitForResult().waitForResult();
					}catch (Exception e){
						return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERATE_BLUEPRINT_ERROR, e.getMessage(), vfcmt.getName());
					}
					if (StringUtils.isEmpty(resultBlueprintCreation)) {
						return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERATE_BLUEPRINT_ERROR, "", vfcmt.getName());
					}

					// 1806 US374595 flowType in cdump
					String flowTypeFromCdump = StringUtils.substringBetween(cdump,"\"flowType\":\"","\"");
					if(StringUtils.isNotBlank(flowTypeFromCdump)) {
						monitoringFlowType = flowTypeFromCdump;
					}
					// saving to serviceVfInstance
					Artifact savedBluePrint = saveBluePrint(userId, serviceUuid, serviceInstanceName, resultBlueprintCreation, monitoringFlowType, vfcmt.getName(), requestId);
					if(savedBluePrint!=null){
						MessageResponse response = new MessageResponse();
						response.setSuccessResponse("Blueprint build complete \n. Blueprint="+savedBluePrint.getArtifactName());
						//1806 US374593 - certify VFCMT after BP generation
						certifyVfcmt(vfcmt, requestId);
						return new ResponseEntity<>(response, HttpStatus.OK);
					}
					else{
						return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SUBMIT_BLUEPRINT_ERROR);
					}

				}else{
					return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", vfcmt.getName());
				}
			} catch (Exception e) {
				return handleException(e, ApiType.SUBMIT_BLUEPRINT);
			}
		}
		
		
	/********************* private function ********************/

	/**
	 * @param userId
	 * @param serviceUuid
	 * @param resourceInstanceName
	 * @param bluePrint
	 * @param monitoringFlowType
	 * @param vfcmtName
	 * @param requestId
	 * @return
	 * @throws Exception
	 */
	private Artifact saveBluePrint(String userId, String serviceUuid, String resourceInstanceName, String bluePrint, String monitoringFlowType, String vfcmtName, String requestId) throws Exception {
		
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "BLUEPRINT:\n{}", bluePrint);
		try {
			ServiceDetailed service = baseBusinessLogic.getSdcRestClient().getService(serviceUuid, requestId);
			//Validations
			checkUserIfResourceCheckedOut(userId, service);
			ResourceInstance vfi = findVfiOnService(service, resourceInstanceName);
			if(null == vfi){
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "VF instance {} not found on service {}", resourceInstanceName, serviceUuid);
				return null;
			}

			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), service.toString());

			String normalizedArtifactLabel = Normalizers.normalizeArtifactLabel("blueprint-" + monitoringFlowType);
			Artifact blueprintArtifact = CollectionUtils.isEmpty(vfi.getArtifacts()) ? null : vfi.getArtifacts().stream()
					.filter(p -> normalizedArtifactLabel.equals(Normalizers.normalizeArtifactLabel(p.getArtifactLabel())))
					.findAny()
					.orElse(null);

			boolean isNeed2Checkout = isNeedToCheckOut(service.getLifecycleState());
			if (isNeed2Checkout) {
				Asset result = checkout(userId, serviceUuid, AssetType.SERVICE, requestId);
				if (result != null) {
					serviceUuid = result.getUuid();
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New service after checkout is: {}", serviceUuid);
				}
			}
			//update mode
			if (null != blueprintArtifact) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Found that service {} already consist of {} ----> updateMode", serviceUuid, normalizedArtifactLabel);
				blueprintArtifact.setDescription(UPDATE_DESC);
				blueprintArtifact.setPayloadData(Base64Utils.encodeToString(bluePrint.getBytes()));
				blueprintArtifact = baseBusinessLogic.getSdcRestClient().updateVfInstanceArtifact(userId, serviceUuid, Normalizers.normalizeComponentInstanceName(resourceInstanceName), blueprintArtifact, requestId);
			//create mode
			} else {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Service {} does not consist {} ----> createMode", serviceUuid, normalizedArtifactLabel);
				blueprintArtifact = SdcRestClientUtils.generateDeploymentArtifact(CREATE_DESC, generateBlueprintFileName(monitoringFlowType, vfcmtName), ArtifactType.DCAE_INVENTORY_BLUEPRINT.name(), normalizedArtifactLabel, bluePrint.getBytes());
				blueprintArtifact = baseBusinessLogic.getSdcRestClient().createVfInstanceArtifact(userId, serviceUuid, Normalizers.normalizeComponentInstanceName(resourceInstanceName), blueprintArtifact, requestId);
			}

			//No need to check the service in in 1806
//			Asset blueprintAsJson = checkin(user_id, serviceUuid, AssetType.SERVICE);
//			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "service result after check-in: {}", blueprintAsJson.toString());

			return blueprintArtifact;

		} catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred while trying to save blueprint {}", e.toString());
			throw e;
		}
	}

	/**
	 * 
	 * @param monitoringFlowType
	 * @param vfcmtName
	 * @return
	 */
	private String generateBlueprintFileName(String monitoringFlowType, String vfcmtName) {
		StringBuffer sb = new StringBuffer();
		sb.append(monitoringFlowType);
		sb.append(".");
		sb.append(Normalizers.normalizeComponentName(vfcmtName));
		sb.append(".");
		sb.append(DcaeBeConstants.Composition.fileNames.EVENT_PROC_BP_YAML);
		return sb.toString();
	}

	private ResourceInstance findVfiOnService(ServiceDetailed service, String vfiName) {
		return null == service ? null : CollectionUtils.isEmpty(service.getResources()) ? null : service.getResources().stream().filter(p -> vfiName.equals(p.getResourceInstanceName())).findAny().orElse(null);
	}

	private Artifact findCdumpArtifactData(ResourceDetailed vfcmt) {
		return null == vfcmt ? null : CollectionUtils.isEmpty(vfcmt.getArtifacts()) ? null : vfcmt.getArtifacts().stream()
				.filter(p -> DcaeBeConstants.Composition.fileNames.COMPOSITION_YML.equals(p.getArtifactName())).findAny().orElse(null);
	}

	private void certifyVfcmt(ResourceDetailed vfcmt, String requestId){
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
				baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(vfcmt.getLastUpdaterUserId(), vfcmt.getUuid(), LifecycleOperationType.CHECKIN.name(), "check in VFCMT after blueprint successful submission", requestId);
			case NOT_CERTIFIED_CHECKIN:
				baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(vfcmt.getLastUpdaterUserId(), vfcmt.getUuid(), LifecycleOperationType.CERTIFY.name(), "certify VFCMT after blueprint successful submission", requestId);
			}
		}
		catch (Exception e){
			//informative only. no message to user (TBA)
			debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred during vfcmt lifecycle operation: {}", e.toString());
		}
	}

}

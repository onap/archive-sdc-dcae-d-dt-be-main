package org.onap.sdc.dcae.composition.controller;

import org.onap.sdc.dcae.composition.impl.BlueprintBusinessLogic;
import org.onap.sdc.dcae.enums.AssetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class BlueprintController extends BaseController {

	@Autowired
	private BlueprintBusinessLogic blueprintBusinessLogic;

	/***
	 * VFCMT - Resource, blueprint - as an artifact as an service.
	 * @param context
	 * @param userId
	 * @param vfcmtUuid
	 * @param serviceUuid
	 * @param instanceName
	 * @param requestId
	 * @return ResponseEntity
	 */
	@RequestMapping(value = "{context}/createBluePrint/{VFCMTUuid}/{serviceUuid}/{instanceName:.*}", method = RequestMethod.POST)
	public ResponseEntity createBlueprint(
			@RequestHeader("USER_ID") String userId,
			@PathVariable String context,
			@PathVariable("VFCMTUuid") String vfcmtUuid,
			@PathVariable("serviceUuid") String serviceUuid,
			@PathVariable("instanceName") String instanceName,
			@ModelAttribute("requestId") String requestId) {
		return blueprintBusinessLogic.generateAndSaveBlueprint(userId, context, vfcmtUuid, serviceUuid, instanceName, "", requestId);
	}

	@Deprecated
	@RequestMapping(value = "/createBluePrint/{VFCMTUuid}/{serviceUuid}/{instanceName}/{monitoringFlowType:.*}", method = RequestMethod.POST)
	public ResponseEntity createBluePrintWithFlowType(
			@RequestHeader("USER_ID") String userId,
			@PathVariable("VFCMTUuid") String vfcmtUuid,
			@PathVariable("serviceUuid") String serviceUuid,
			@PathVariable("instanceName") String instanceName,
			@PathVariable("monitoringFlowType") String monitoringFlowType,
			@ModelAttribute("requestId") String requestId) {
		return blueprintBusinessLogic.generateAndSaveBlueprint(userId, AssetType.SERVICE.name(), vfcmtUuid, serviceUuid, instanceName, monitoringFlowType, requestId);
	}
}

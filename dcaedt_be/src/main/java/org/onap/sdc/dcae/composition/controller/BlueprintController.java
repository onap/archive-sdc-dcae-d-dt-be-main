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
	@PostMapping(value = "{context}/createBluePrint/{VFCMTUuid}/{serviceUuid}/{instanceName:.*}")
	public ResponseEntity createBlueprint(
			@RequestHeader("USER_ID") String userId,
			@PathVariable String context,
			@PathVariable("VFCMTUuid") String vfcmtUuid,
			@PathVariable("serviceUuid") String serviceUuid,
			@PathVariable("instanceName") String instanceName,
			@ModelAttribute("requestId") String requestId) {
		return blueprintBusinessLogic.generateAndSaveBlueprint(userId, context, vfcmtUuid, serviceUuid, instanceName, "", requestId);
	}

	// 1810 US436244 MC table
	@PostMapping(value = "{context}/createBluePrint/{vfcmtUuid}/{revertedUuid}/{serviceUuid}/{instanceName:.*}")
	public ResponseEntity createBlueprint(
			@RequestHeader("USER_ID") String userId,
			@PathVariable String context,
			@PathVariable String vfcmtUuid,
			@PathVariable String revertedUuid,
			@PathVariable String serviceUuid,
			@PathVariable String instanceName,
			@ModelAttribute("requestId") String requestId) {
		return blueprintBusinessLogic.generateAndSaveBlueprint(userId, context, vfcmtUuid, serviceUuid, instanceName, "", requestId);
	}

	@Deprecated
	@PostMapping(value = "/createBluePrint/{VFCMTUuid}/{serviceUuid}/{instanceName}/{monitoringFlowType:.*}")
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

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

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.impl.ServiceBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.AttachVFCMTServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class ServicesController extends BaseController {

	@Autowired
	private ServiceBusinessLogic serviceBusinessLogic;
	/***
	 * GET services list by VFCMT
	 * @param userId
	 * @param vfcmtUuid
	 * @return ResponseEntity
	 */
	@RequestMapping(value = { "/services/{vfcmtUuid}" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity services(@RequestHeader("USER_ID") String userId, @PathVariable String vfcmtUuid, @ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting services");
		return serviceBusinessLogic.services(userId,vfcmtUuid,requestId);
	}

	/***
	 * GET a single service
	 * @param theServiceId
	 * @return ResponseEntity
	 */
	@RequestMapping(value = { "/service/{theServiceId}" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity service(@PathVariable String theServiceId, @ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting service");
		return serviceBusinessLogic.service(theServiceId,requestId);
	}

	
	/***
	 * Attach service and service instance to VFCMT 
	 * @param userId
	 * @param request
	 * @return ResponseEntity
	 */
	@Deprecated
	@RequestMapping(value = "/{vfcmtUuid}/attachment", method = RequestMethod.POST, produces = {"application/json" })
	public ResponseEntity attachService(
			@PathVariable("vfcmtUuid") String vfcmtUuid, 
			@RequestHeader("USER_ID") String userId,
			@RequestBody AttachVFCMTServiceRequest request,
			@ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting attachService");
		return serviceBusinessLogic.attachService(vfcmtUuid,userId,request,requestId);
	}

	@RequestMapping(value = { "/{vfcmtUuid}/attachment" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity getAttachedService(@PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getAttachedService");
		return serviceBusinessLogic.getAttachedService(vfcmtUuid,requestId);
	}

}

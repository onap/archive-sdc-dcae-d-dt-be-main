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
import org.onap.sdc.dcae.composition.impl.CompositionBusinessLogic;
import org.onap.sdc.dcae.composition.impl.CompositionCatalogBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.CreateMcResponse;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.ReferenceUUID;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrConfMgr.ApiType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class CompositionController extends BaseController {

	@Autowired
	private CompositionCatalogBusinessLogic compositionCatalogBusinessLogic;

	@Autowired
	private CompositionBusinessLogic compositionBusinessLogic;

	@RequestMapping(value = "/{theItemId}/model", method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity model(@ModelAttribute("requestId") String requestId, @PathVariable String theItemId) {
		return compositionCatalogBusinessLogic.getModelById(requestId, theItemId);
	}

	@RequestMapping(value = "/{theItemId}/type/{theTypeName:.*}", method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity typeInfo(@ModelAttribute("requestId") String requestId, @PathVariable String theItemId, @PathVariable String theTypeName) {
		return compositionCatalogBusinessLogic.getTypeInfo(theItemId, theTypeName, requestId);
	}

	@RequestMapping(value = "/catalog", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity getCatalog(@ModelAttribute("requestId") String requestId) {
		try {
			return new ResponseEntity<>(compositionCatalogBusinessLogic.getCatalog(requestId), HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ApiType.GET_MODEL);
		}
	}

	@RequestMapping(value = { "/getComposition/{vfcmtUuid}" }, method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity getComposition(@PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId) {
		MessageResponse response = new MessageResponse();
		try {
			Artifact compositionArtifact = compositionBusinessLogic.getComposition(vfcmtUuid, requestId);
			if (null == compositionArtifact) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Couldn't find {} in VFCMT artifacts", DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
				response.setErrorResponse("No Artifacts");
				return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
			}

			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "ARTIFACT: {}", compositionArtifact.getPayloadData());
			response.setSuccessResponse(compositionArtifact.getPayloadData());
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ApiType.GET_CDUMP);
		}
	}

	@RequestMapping(value = { "/getMC/{vfcmtUuid}" }, method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity getMC(@PathVariable String vfcmtUuid, @ModelAttribute String requestId) {
		try {
			return new ResponseEntity<>(compositionBusinessLogic.getDataAndComposition(vfcmtUuid, requestId), HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ApiType.GET_VFCMT);
		}
	}

	// 1810 US436244 MC table
	@RequestMapping(value = { "/getMC/{vfcmtUuid}/{revertedUuid}" }, method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity getSubmittedMcWithRevertedReference(@PathVariable String vfcmtUuid, @PathVariable String revertedUuid, @ModelAttribute String requestId) {
		try {
			CreateMcResponse res = compositionBusinessLogic.getDataAndComposition(vfcmtUuid, requestId);
			res.getVfcmt().setUuid(vfcmtUuid.concat("/").concat(revertedUuid));
			return new ResponseEntity<>(res, HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ApiType.GET_VFCMT);
		}
	}

	@RequestMapping(value = "/saveComposition/{vfcmtUuid}", method = RequestMethod.POST)
	public ResponseEntity saveComposition(@RequestHeader("USER_ID") String userId, @RequestBody String theCdump, @PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "ARTIFACT CDUMP: {}", theCdump);
		return compositionBusinessLogic.saveComposition(userId, vfcmtUuid, theCdump, requestId, true);
	}

	@RequestMapping(value = "/{contextType}/{serviceUuid}/{vfiName}/saveComposition/{vfcmtUuid}", method = RequestMethod.POST)
	public ResponseEntity updateComposition(@RequestHeader("USER_ID") String userId, @RequestBody String theCdump,
			@PathVariable String contextType, @PathVariable String serviceUuid, @PathVariable String vfiName, @PathVariable String vfcmtUuid, @ModelAttribute String requestId) {

		ResponseEntity res = compositionBusinessLogic.saveComposition(userId, vfcmtUuid, theCdump, requestId, false);
		if (HttpStatus.OK == res.getStatusCode()) {
			ResourceDetailed vfcmt = (ResourceDetailed) res.getBody();
			if (!vfcmtUuid.equals(vfcmt.getUuid())) {
				try {
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New vfcmt major version created with id {} , adding new reference.", vfcmt.getUuid());
					baseBusinessLogic.getSdcRestClient().addExternalMonitoringReference(userId, contextType, serviceUuid, vfiName, new ReferenceUUID(vfcmt.getUuid()), requestId);
				} catch (Exception e) {
					return handleException(e, ApiType.SAVE_CDUMP);
				}
			}
		}
		return res;
	}

	@RequestMapping(value = "/{contextType}/{serviceUuid}/{vfiName}/saveComposition/{vfcmtUuid}/{revertedUuid}", method = RequestMethod.POST)
	public ResponseEntity overwriteRevertedComposition(@RequestHeader("USER_ID") String userId, @RequestBody String theCdump,
			@PathVariable String contextType, @PathVariable String serviceUuid, @PathVariable String vfiName, @PathVariable String vfcmtUuid, @PathVariable String revertedUuid, @ModelAttribute String requestId) {
		try {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Reverted MC version {} is about to be overwritten with submitted MC version {}", revertedUuid, vfcmtUuid);
			ResponseEntity res = compositionBusinessLogic.overwriteRevertedMC(userId, vfcmtUuid, revertedUuid, theCdump, requestId);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Reverted MC version {} overwrite result status code: {}", revertedUuid, res.getStatusCodeValue());
			if(HttpStatus.OK == res.getStatusCode()) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "About to undo revert of external monitoring reference from service {} to MC {}", serviceUuid, revertedUuid);
				compositionBusinessLogic.undoRevert(userId, contextType, serviceUuid, vfiName, revertedUuid, requestId);
			}
			return res;
		} catch (Exception e) {
			return handleException(e, ApiType.SAVE_CDUMP);
		}
	}
}

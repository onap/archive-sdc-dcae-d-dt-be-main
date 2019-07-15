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
import org.onap.sdc.dcae.composition.impl.ReferenceBusinessLogic;
import org.onap.sdc.dcae.composition.impl.VfcmtBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.CreateVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.ImportVFCMTRequest;
import org.onap.sdc.dcae.composition.restmodels.sdc.ExternalReferencesMap;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.errormng.ErrConfMgr.ApiType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class VfcmtController extends BaseController{

    @Autowired
    private VfcmtBusinessLogic vfcmtBusinessLogic;
    @Autowired
    private ReferenceBusinessLogic referenceBusinessLogic;

    private static final String TEMPLATE = "Template";
    private static final String BASE_MONITORING_TEMPLATE = "Base Monitoring Template";
	private static final String MONITORING_TEMPLATE = "Monitoring Template";

    /***
     * Get one resource information
     * @param theResourceId retrieved resource id
     * @return ResponseEntity
     */
    @RequestMapping(value = { "/resource/{theResourceId}" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity resource(@PathVariable String theResourceId, @ModelAttribute("requestId") String requestId) {
        try {
            ResourceDetailed resource = baseBusinessLogic.getSdcRestClient().getResource(theResourceId, requestId);
            return new ResponseEntity<>(resource, HttpStatus.OK);
        }catch (Exception e) {
            return handleException(e, ApiType.GET_VFCMT);
        }
    }

    /***
     * Get All resources
     * @return ResponseEntity
     */
    @RequestMapping(value = { "/getResourcesByCategory" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity getResourcesByCategory(@ModelAttribute("requestId") String requestId) {
        try {
            List<Resource> resources = baseBusinessLogic.getSdcRestClient().getResources(AssetType.VFCMT.name(), TEMPLATE, MONITORING_TEMPLATE, requestId);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.GET_ALL_VFCMTS);
        }
    }

    /***
     * Get All resources by Service
     * @return ResponseEntity
     */

    @RequestMapping(value = { "/{contextType}/{uuid}/{version}/getVfcmtsForMigration" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity getVfcmtsForMigration(@RequestHeader("USER_ID") String userId,
                                                       @PathVariable String contextType,
                                                       @PathVariable String uuid,
                                                       @PathVariable String version,
                                                       @ModelAttribute("requestId") String requestId){

        return vfcmtBusinessLogic.getVfcmtsForMigration(userId, contextType, uuid, version, requestId);
    }

    /***
     * Get All resources by Monitoring Template Category
     * @return ResponseEntity
     */
    @RequestMapping(value = { "/getResourcesByMonitoringTemplateCategory" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity getResourcesByMonitoringTemplateCategory(@ModelAttribute("requestId") String requestId) {
        try {
            List<Resource> resources = baseBusinessLogic.getSdcRestClient().getResources(AssetType.VFCMT.name(), TEMPLATE, BASE_MONITORING_TEMPLATE, requestId);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.GET_ALL_VFCMTS);
        }
    }

    /***
     * Create new Vfcmt
     * @param userId retrieved user ID
     * @param request retrieved request
     * @return ResponseEntity
     */
    @RequestMapping(value = "/createVFCMT", method = RequestMethod.POST, produces = {"application/json" })
    public ResponseEntity createVFCMT(@RequestHeader("USER_ID") String userId, @RequestBody CreateVFCMTRequest request, @ModelAttribute("requestId") String requestId) {
        vfcmtBusinessLogic.addSdcMandatoryFields(request, userId);
        try {
            ResourceDetailed response = baseBusinessLogic.getSdcRestClient().createResource(userId, request, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "createVFCMT after post: {}", response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.CREATE_NEW_VFCMT);
        }
    }

    /***
     * Create new Vfcmt from general screen
     * @param userId retrieved user ID
     * @param request retrieved request
     * @return ResponseEntity
     */
    @RequestMapping(value = "/createMC", method = RequestMethod.POST, produces = {"application/json" })
    public ResponseEntity createMC(@RequestHeader("USER_ID") String userId, @RequestBody CreateVFCMTRequest request, @ModelAttribute("requestId") String requestId) {
        return vfcmtBusinessLogic.createMcFromTemplate(userId, request, requestId);
    }


    /***
     * Clone or import existing VFCMT and attach to selected service/resource
     * @param userId
     * @param request
     * @return ResponseEntity
     */
    @RequestMapping(value = "/importMC", method = RequestMethod.POST, produces = {"application/json" })
    public ResponseEntity importMC(@RequestHeader("USER_ID") String userId, @RequestBody ImportVFCMTRequest request, @ModelAttribute("requestId") String requestId) {
        return vfcmtBusinessLogic.importMC(userId, request, requestId);
    }

    /***
     * GET a list of Monitoring Components of a service by uuid and version
     * @param contextType the context type of this request
     * @param uuid the uuid of the type requested
     * @param version the version of the entity requested
     * @return ResponseEntity
     */
    @RequestMapping(value = { "/{contextType}/{uuid}/{version}/monitoringComponents" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity getMonitoringComponents(@PathVariable String contextType, @PathVariable String uuid, @PathVariable String version, @ModelAttribute("requestId") String requestId) {
        try {
            ExternalReferencesMap mcRefs = baseBusinessLogic.getSdcRestClient().getMonitoringReferences(contextType, uuid, version, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Got monitoring references map from SDC: {}", mcRefs.values());
            return new ResponseEntity<>(referenceBusinessLogic.fetchMonitoringComponents(mcRefs, requestId), HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.GET_SERVICE);
        }
    }

    @RequestMapping(value = { "/{contextType}/{serviceUuid}/{vfiName}/{vfcmtUuid}/deleteVfcmtReference" }, method = { RequestMethod.DELETE }, produces = {"application/json" })
    public ResponseEntity deleteVfcmtReference(@RequestHeader("USER_ID")  String userId, @PathVariable String contextType, @PathVariable String serviceUuid, @PathVariable String vfiName, @PathVariable String vfcmtUuid, @ModelAttribute String requestId) {
        try {
            referenceBusinessLogic.deleteVfcmtReference(userId, contextType, serviceUuid, vfiName, vfcmtUuid, requestId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.DELETE_VFCMT_REFERENCE);
        }
    }

    @RequestMapping(value = { "/{contextType}/{monitoringComponentName}/{serviceUuid}/{vfiName}/{vfcmtUuid}/deleteVfcmtReference" }, method = { RequestMethod.DELETE }, produces = {"application/json" })
    public ResponseEntity deleteVfcmtReferenceWithBlueprint(@RequestHeader("USER_ID")  String userId, @PathVariable String contextType, @PathVariable String monitoringComponentName, @PathVariable String serviceUuid, @PathVariable String vfiName, @PathVariable String vfcmtUuid, @ModelAttribute String requestId) {
        try {
            referenceBusinessLogic.deleteVfcmtReference(userId, contextType, serviceUuid, vfiName, vfcmtUuid, requestId);
        } catch (Exception e) {
            return handleException(e, ApiType.DELETE_VFCMT_REFERENCE);
        }
        return referenceBusinessLogic.deleteVfcmtReferenceBlueprint(userId, contextType, monitoringComponentName, serviceUuid, vfiName, vfcmtUuid, requestId);
    }

    // 1810 US436244 MC table functionality
	@RequestMapping(value = { "/{contextType}/{monitoringComponentName}/{serviceUuid}/{vfiName}/{vfcmtUuid}/{revertedUuid}/deleteVfcmtReference" }, method = { RequestMethod.DELETE }, produces = {"application/json" })
	public ResponseEntity deleteVfcmtReferenceWithBlueprint(@RequestHeader("USER_ID") String userId,
			                                                @PathVariable String contextType,
			                                                @PathVariable String monitoringComponentName,
			                                                @PathVariable String serviceUuid,
			                                                @PathVariable String vfiName,
			                                                @PathVariable String vfcmtUuid,
			                                                @PathVariable String revertedUuid,
			                                                @ModelAttribute String requestId) {
		try {
			referenceBusinessLogic.deleteVfcmtReference(userId, contextType, serviceUuid, vfiName, vfcmtUuid, revertedUuid, requestId);
		} catch (Exception e) {
			return handleException(e, ApiType.DELETE_VFCMT_REFERENCE);
		}
		return referenceBusinessLogic.deleteVfcmtReferenceBlueprint(userId, contextType, monitoringComponentName, serviceUuid, vfiName, vfcmtUuid, requestId);
	}

	// 1810 US436244 MC table functionality
	@RequestMapping(value = { "/{contextType}/{monitoringComponentName}/{serviceUuid}/{vfiName}/{vfcmtUuid}/deleteVfcmtReference/{submittedUuid}" }, method = { RequestMethod.DELETE }, produces = {"application/json" })
	public ResponseEntity deleteVfcmtReferencesWithBlueprint(@RequestHeader("USER_ID") String userId,
			@PathVariable String contextType,
			@PathVariable String monitoringComponentName,
			@PathVariable String serviceUuid,
			@PathVariable String vfiName,
			@PathVariable String vfcmtUuid,
			@PathVariable String submittedUuid,
			@ModelAttribute String requestId) {
		try {
			referenceBusinessLogic.deleteVfcmtReference(userId, contextType, serviceUuid, vfiName, vfcmtUuid, requestId);
			referenceBusinessLogic.deleteVfcmtReference(userId, contextType, serviceUuid, vfiName, submittedUuid, requestId);
		} catch (Exception e) {
			return handleException(e, ApiType.DELETE_VFCMT_REFERENCE);
		}
		return referenceBusinessLogic.deleteVfcmtReferenceBlueprint(userId, contextType, monitoringComponentName, serviceUuid, vfiName, submittedUuid, requestId);
	}

    @RequestMapping(value = { "/getVfcmtReferenceData/{vfcmtUuid}" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity getVfcmtReferenceData(@PathVariable String vfcmtUuid, @ModelAttribute String requestId) {
        try {
            return vfcmtBusinessLogic.getVfcmtReferenceData(vfcmtUuid, requestId);
        } catch (Exception e) {
            return handleException(e, ApiType.GET_VFCMT);
        }
    }

	@RequestMapping(value = { "/{contextType}/{serviceUuid}/{vfiName}/{vfcmtUuid}/getLatestMcUuid" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity getLatestMcUuid(@RequestHeader("USER_ID")  String userId, @PathVariable String contextType, @PathVariable String serviceUuid, @PathVariable String vfiName, @PathVariable String vfcmtUuid, @ModelAttribute String requestId) {
	    return referenceBusinessLogic.checkoutAndBindToServiceIfCertified(userId, contextType, serviceUuid, vfiName, vfcmtUuid, requestId);
	}

	// 1810 US436244 MC table functionality
	@RequestMapping(value = { "/{contextType}/{serviceUuid}/{vfiName}/{vfcmtUuid}/{revertedUuid}/getLatestMcUuid" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity getLatestMcUuid(@RequestHeader("USER_ID")  String userId, @PathVariable String contextType, @PathVariable String serviceUuid, @PathVariable String vfiName, @PathVariable String vfcmtUuid, @PathVariable String revertedUuid, @ModelAttribute String requestId) {
		return referenceBusinessLogic.checkoutAndUndoRevertMC(userId, contextType, serviceUuid, vfiName, vfcmtUuid, revertedUuid, requestId);
	}

	@RequestMapping(value = { "/{contextType}/{serviceUuid}/{vfiName}/{vfcmtUuid}/revert/{submittedUuid}" }, method = { RequestMethod.POST }, produces = {"application/json" })
	public ResponseEntity revertToSubmittedMC(@RequestHeader("USER_ID")  String userId,
			                                                @PathVariable String contextType,
			                                                @PathVariable String serviceUuid,
			                                                @PathVariable String vfiName,
			                                                @PathVariable String vfcmtUuid,
			                                                @PathVariable String submittedUuid,
			                                                @ModelAttribute String requestId) {
		try {
			return ResponseEntity.ok(referenceBusinessLogic.revertToSubmittedMC(userId, contextType, serviceUuid, vfiName, vfcmtUuid, submittedUuid, requestId));
		} catch (Exception e) {
			return handleException(e, ApiType.ATTACH_TO_SERVICE);
		}
	}
}

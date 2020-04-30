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

import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class LifecycleController extends BaseController {

    @PutMapping(value={"/checkin/{assetType}/{uuid}"}, produces={"application/json"})
    public ResponseEntity putCheckin(
            @PathVariable("assetType") String assetType,
            @PathVariable("uuid") UUID uuid,
            @RequestHeader("USER_ID") String userId,
            @ModelAttribute("requestId") String requestId)  {

        try {
        	if (AssetType.VFCMT == baseBusinessLogic.getValidAssetTypeOrNull(assetType)) {
				ResourceDetailed resCheckin = baseBusinessLogic.checkinVfcmt(userId, uuid.toString(), requestId);
				return new ResponseEntity<>(resCheckin, HttpStatus.OK);
            } else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
        } catch (Exception e) {
            return handleException(e, ErrConfMgr.ApiType.CHECK_IN_RESOURCE);
        }
    }

    @PutMapping(value={"/checkout/{assetType}/{uuid}"}, produces={"application/json"})
    public ResponseEntity putCheckout(
            @PathVariable("assetType") String assetType,
            @PathVariable("uuid") UUID uuid,
            @RequestHeader("USER_ID") String userId,
            @ModelAttribute("requestId") String requestId)  {

        try {
			if (AssetType.VFCMT == baseBusinessLogic.getValidAssetTypeOrNull(assetType)) {
				ResourceDetailed asset = baseBusinessLogic.checkoutVfcmt(userId, uuid.toString(), requestId);
				return new ResponseEntity<>(asset, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return handleException(e, ErrConfMgr.ApiType.CHECK_OUT_RESOURCE);
        }
    }

    @PutMapping(value={"/certify/{assetType}/{uuid}"}, produces={"application/json"})
    public ResponseEntity putCertify(
            @PathVariable("assetType") String assetType,
            @PathVariable("uuid") String uuid,
            @RequestHeader("USER_ID") String userId,
            @ModelAttribute("requestId") String requestId)  {

        try {
			if (AssetType.VFCMT == baseBusinessLogic.getValidAssetTypeOrNull(assetType)) {
				ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, uuid, LifecycleOperationType.CERTIFY.name(), "certifying VFCMT", requestId);
				return new ResponseEntity<>(vfcmt, HttpStatus.OK);

			} else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return handleException(e, ErrConfMgr.ApiType.CHECK_OUT_RESOURCE);
        }
    }

}

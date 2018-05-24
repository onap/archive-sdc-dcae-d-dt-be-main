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

    @RequestMapping(value={"/checkin/{assetType}/{uuid}"}, method={RequestMethod.PUT}, produces={"application/json"})
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

    @RequestMapping(value={"/checkout/{assetType}/{uuid}"}, method={RequestMethod.PUT}, produces={"application/json"})
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

    @RequestMapping(value={"/certify/{assetType}/{uuid}"}, method={RequestMethod.PUT}, produces={"application/json"})
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

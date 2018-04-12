package org.onap.sdc.dcae.composition.controller;

import org.onap.sdc.dcae.composition.restmodels.sdc.Asset;
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

    private static final String VFCMT = "vfcmt";

    @RequestMapping(value={"/checkin/{assetType}/{uuid}"}, method={RequestMethod.PUT}, produces={"application/json"})
    public ResponseEntity putCheckin(
            @PathVariable("assetType") String assetType,
            @PathVariable("uuid") UUID uuid,
            @RequestHeader("USER_ID") String user_id,
            @ModelAttribute("requestId") String requestId)  {

        try {
            switch (assetType) {
                case VFCMT:
                     Asset res_checkin = checkin(user_id, uuid.toString(), AssetType.RESOURCE, requestId);
                     return new ResponseEntity<>(res_checkin, HttpStatus.OK);

                default:
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
            @RequestHeader("USER_ID") String user_id,
            @ModelAttribute("requestId") String requestId)  {

        try {
            switch (assetType) {
                case VFCMT:
                     Asset asset = checkout(user_id, uuid.toString(), AssetType.RESOURCE, requestId);
                     return new ResponseEntity<>(asset, HttpStatus.OK);

                default:
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
            @RequestHeader("USER_ID") String user_id,
            @ModelAttribute("requestId") String requestId)  {

        try {
            switch (assetType) {
            case VFCMT:
                ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(user_id, uuid, LifecycleOperationType.CERTIFY.name(), "certifying VFCMT", requestId);
                return new ResponseEntity<>(vfcmt, HttpStatus.OK);

            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return handleException(e, ErrConfMgr.ApiType.CHECK_OUT_RESOURCE);
        }
    }
}

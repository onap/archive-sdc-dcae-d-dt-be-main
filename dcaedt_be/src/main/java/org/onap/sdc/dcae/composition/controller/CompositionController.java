package org.onap.sdc.dcae.composition.controller;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.impl.CompositionBusinessLogic;
import org.onap.sdc.dcae.composition.impl.CompositionCatalogBusinessLogic;
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

//	@Deprecated
//	@RequestMapping(value = { "/utils/clone/{assetType}/{sourceId}/{targetId}" }, method = { RequestMethod.GET }, produces = { "application/json" })
//	public ResponseEntity clone(@RequestHeader("USER_ID") String userId,
//			@PathVariable("assetType") String theAssetType, @PathVariable("sourceId") String theSourceId, @PathVariable("targetId") String theTargetId, @ModelAttribute("requestId") String requestId) {
//		MessageResponse response = new MessageResponse();
//
//		try {
//			// fetch the source and assert it is a vfcmt containing clone worthy artifacts (composition + rules)
//			ResourceDetailed sourceVfcmt = baseBusinessLogic.getSdcRestClient().getResource(theSourceId, requestId);
//			baseBusinessLogic.checkVfcmtType(sourceVfcmt);
//			List<Artifact> artifactsToClone = CollectionUtils.isEmpty(sourceVfcmt.getArtifacts()) ?
//					null :
//					sourceVfcmt.getArtifacts().stream().filter(p -> DcaeBeConstants.Composition.fileNames.COMPOSITION_YML.equals(p.getArtifactName()) || p.getArtifactName().endsWith(DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX))
//							.collect(Collectors.toList());
//			if (CollectionUtils.isEmpty(artifactsToClone)) {
//				response.setSuccessResponse("Nothing to clone");
//				return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
//			}
//
//			// fetch the target
//			ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(theTargetId, requestId);
//			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
//			baseBusinessLogic.checkVfcmtType(vfcmt);
//			baseBusinessLogic.checkUserIfResourceCheckedOut(userId, vfcmt);
//			boolean isTargetNeed2Checkout = baseBusinessLogic.isNeedToCheckOut(vfcmt.getLifecycleState());
//			if (isTargetNeed2Checkout) {
//				ResourceDetailed targetVfcmt = baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, theTargetId, LifecycleOperationType.CHECKOUT.name(), "checking out VFCMT before clone", requestId);
//				if (null == targetVfcmt) {
//					return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERAL_ERROR);
//				}
//				theTargetId = targetVfcmt.getUuid();
//				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New targetVfcmt (for artifact clone) after checkoutVfcmt is: {}", theTargetId);
//			}
//
//			Map<String, Artifact> currentArtifacts = CollectionUtils.isEmpty(vfcmt.getArtifacts()) ? new HashMap<>() : vfcmt.getArtifacts().stream().collect(Collectors.toMap(Artifact::getArtifactName, Function.identity()));
//
//			//TODO target VFCMT rule artifacts should be removed
//			for (Artifact artifactToClone : artifactsToClone) {
//				String payload = baseBusinessLogic.getSdcRestClient().getResourceArtifact(theSourceId, artifactToClone.getArtifactUUID(), requestId);
//				baseBusinessLogic.cloneArtifactToTarget(userId, theTargetId, payload, artifactToClone, currentArtifacts.get(artifactToClone.getArtifactName()), requestId);
//			}
//
//			baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, theTargetId, LifecycleOperationType.CHECKIN.name(), "check in VFCMT after clone", requestId);
//			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Cloning {} from {} has finished successfully", theSourceId, theTargetId);
//			response.setSuccessResponse("Clone VFCMT complete");
//			return new ResponseEntity<>(response, HttpStatus.OK);
//		} catch (Exception e) {
//			return handleException(e, ApiType.CLONE_VFCMT);
//		}
//	}

	@RequestMapping(value = "/{theItemId}/model", method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity model(@ModelAttribute("requestId") String requestId, @PathVariable String theItemId) {
		return compositionCatalogBusinessLogic.getModelById(requestId, theItemId);
	}

	@RequestMapping(value = "/{theItemId}/type/{theTypeName:.*}", method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity typeInfo(@ModelAttribute("requestId") String requestId, @PathVariable String theItemId, @PathVariable String theTypeName) {
		return compositionCatalogBusinessLogic.getTypeInfo(theItemId, theTypeName);
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
}
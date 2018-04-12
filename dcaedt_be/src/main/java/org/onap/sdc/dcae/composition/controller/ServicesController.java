package org.onap.sdc.dcae.composition.controller;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.AttachVFCMTServiceRequest;
import org.onap.sdc.dcae.composition.restmodels.DcaeMinimizedService;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.sdc.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.DcaeException;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class ServicesController extends BaseController {

	/***
	 * GET services list by VFCMT
	 * @param userId
	 * @param vfcmtUuid
	 * @return ResponseEntity
	 */
	@RequestMapping(value = { "/services/{vfcmtUuid}" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity services(@RequestHeader("USER_ID") String userId, @PathVariable String vfcmtUuid, @ModelAttribute("requestId") String requestId) {
		try {
			ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(vfcmtUuid, requestId);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "GET ({}) Vfcmt form SDC: {}", vfcmtUuid, vfcmt);
			checkVfcmtType(vfcmt);
			checkUserIfResourceCheckedOut(userId, vfcmt);

			List<Service> services = baseBusinessLogic.getSdcRestClient().getServices(requestId);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "GET services data from SDC: {}", services);
			List<Service> uuids = filterServicesByUser(services, userId);
			return new ResponseEntity<>(uuids, HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ErrConfMgr.ApiType.GET_SERVICE);
		}
	}

	/***
	 * GET a single service
	 * @param theServiceId
	 * @return ResponseEntity
	 */
	@RequestMapping(value = { "/service/{theServiceId}" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity service(@PathVariable String theServiceId, @ModelAttribute("requestId") String requestId) {
		try {
			ServiceDetailed service = baseBusinessLogic.getSdcRestClient().getService(theServiceId, requestId);
			if (service != null) {
				if(service.getResources()!=null){
					List<ResourceInstance> vfResourcesOnly = service.getResources().stream().filter(vfi -> vfi.getResoucreType().equals("VF")).collect(Collectors.toList());
					service.setResources(vfResourcesOnly);
				}else{
					errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Service {} doesn't have any resources (e.g VFi's)", theServiceId);
				}
			} else {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Couldn't fetch service with uuid {} from SDC", theServiceId);
			}
			return new ResponseEntity<>(service, HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ErrConfMgr.ApiType.GET_SERVICE);
		}
	}

	
	/***
	 * Attach service and service instance to VFCMT 
	 * @param userId
	 * @param request
	 * @return ResponseEntity
	 */
	@RequestMapping(value = "/{vfcmtUuid}/attachment", method = RequestMethod.POST, produces = {"application/json" })
	public ResponseEntity attachService(
			@PathVariable("vfcmtUuid") String vfcmtUuid, 
			@RequestHeader("USER_ID") String userId,
			@RequestBody AttachVFCMTServiceRequest request,
			@ModelAttribute("requestId") String requestId) {

		String serviceUuid = request.getServiceUuid();
		String vfiName = request.getInstanceName();
		String resourceUuid = vfcmtUuid;
		MessageResponse response = new MessageResponse();

		try {
			ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(vfcmtUuid, requestId);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
			
			checkVfcmtType(vfcmt);
			verifyVfiExists(serviceUuid, vfiName, requestId);
			
			boolean isUpdateMode = false;
			Artifact artifactObj = null;
			
			String reference = serviceUuid + "/resources/" + vfiName;
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "*****************************************");
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Reference between service and vfi {}", reference);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "*****************************************");
			
			if(!CollectionUtils.isEmpty(vfcmt.getArtifacts())){
				artifactObj = vfcmt.getArtifacts().stream().filter(a -> DcaeBeConstants.Composition.fileNames.SVC_REF.equals(a.getArtifactName())).findAny().orElse(null);
				isUpdateMode = null != artifactObj;
			}

			if (isNeedToCheckOut(vfcmt.getLifecycleState())) {
				vfcmt = baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, vfcmtUuid, LifecycleOperationType.CHECKOUT.name(), null, requestId);
				if (vfcmt != null) {
					resourceUuid = vfcmt.getUuid();
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New vfcmt uuid after checkout is: {}", resourceUuid);
				}
			}
			
			if(isUpdateMode){
				updateReferenceArtifact(userId, resourceUuid, artifactObj, reference, requestId);
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Artifact {} updated with content: {}", reference, DcaeBeConstants.Composition.fileNames.SVC_REF, reference);
			}else{
				Artifact artifact = SdcRestClientUtils.generateDeploymentArtifact("createReferenceArtifact", DcaeBeConstants.Composition.fileNames.SVC_REF, ArtifactType.DCAE_TOSCA.name(), "servicereference", reference.getBytes());
				baseBusinessLogic.getSdcRestClient().createResourceArtifact(userId, resourceUuid, artifact, requestId);
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Artifact {} created with content: {}", DcaeBeConstants.Composition.fileNames.SVC_REF, reference);
			}
			checkin(userId, resourceUuid, org.onap.sdc.dcae.enums.AssetType.RESOURCE, requestId);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Attachment of reference={} in VFCMT {} has finished successfully", reference, resourceUuid);
			
			response.setSuccessResponse("Artifact updated");
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ErrConfMgr.ApiType.ATTACH_TO_SERVICE);
		}
	}

	@RequestMapping(value = { "/{vfcmtUuid}/attachment" }, method = { RequestMethod.GET }, produces = {"application/json" })
	public ResponseEntity getAttachedService(@PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId) {
		
		MessageResponse response = new MessageResponse();
		
		try {
			ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(vfcmtUuid, requestId);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
			checkVfcmtType(vfcmt);
			String artifact = "No Artifacts";

			if (!CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
				Artifact artifactObj = vfcmt.getArtifacts().stream().filter(a -> DcaeBeConstants.Composition.fileNames.SVC_REF.equals(a.getArtifactName())).findAny().orElse(null);
				if (null != artifactObj)
					artifact = baseBusinessLogic.getSdcRestClient().getResourceArtifact(vfcmtUuid, artifactObj.getArtifactUUID(), requestId);
			}
			response.setSuccessResponse(artifact);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return handleException(e, ErrConfMgr.ApiType.GET_VFCMT);
		}
	}
	
	/**** PRIVATE METHODS ****/

	private void updateReferenceArtifact(String userId, String VFCMTUuid, Artifact artifactObj, String reference, String requestId) throws Exception {
		artifactObj.setDescription("updateReferenceArtifact");
		artifactObj.setPayloadData(Base64Utils.encodeToString(reference.getBytes()));
		baseBusinessLogic.getSdcRestClient().updateResourceArtifact(userId, VFCMTUuid, artifactObj, requestId);
	}

	
	/**
	 * 
	 * @param lastUpdaterUserId
	 * @param services
	 * @param userId
	 * @return
	 */

	//TODO move method to ci tests
    public List<DcaeMinimizedService> parseAndFilterServicesByUser(String lastUpdaterUserId, List<LinkedHashMap<String, String>> services, String userId) {
        List<DcaeMinimizedService> uuids = null;
        if (services != null) {
        	//services.stream().filter(predicate)
           uuids = services.stream()
                    .map(x -> new DcaeMinimizedService(x.get("uuid"), x.get("name"), x.get("lastUpdaterUserId"), x.get("lifecycleState"), x.get("version"), x.get("invariantUUID")))
                    .collect(Collectors.groupingBy(DcaeMinimizedService::getInvariantUUID)).values().stream()
                    .map(p -> p.stream()
                    .sorted(Comparator.comparing(DcaeMinimizedService::getVersionAsFloat).reversed())).map(p -> p.collect(Collectors.toList())).map(p -> p.get(0))
                    .filter(x -> (!(!x.getLastUpdaterUserId().equals(userId) && x.getLifeCycleState().equals(LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name()))))
                    .sorted(Comparator.comparing(DcaeMinimizedService::getName)).collect(Collectors.toList());
        }
        return uuids;
    }

	private List<Service> filterServicesByUser(List<Service> services, String userId) {
    	return CollectionUtils.isEmpty(services) ? new ArrayList<>() : services.stream()
					.collect(Collectors.groupingBy(Service::getInvariantUUID)).values().stream()
					.map(p -> p.stream()
							.sorted(Comparator.comparing(Service::versionAsFloat).reversed())).map(p -> p.collect(Collectors.toList())).map(p -> p.get(0))
					.filter(x -> (!(!x.getLastUpdaterUserId().equals(userId) && x.getLifecycleState().equals(LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name()))))
					.sorted(Comparator.comparing(Service::getName)).collect(Collectors.toList());
	}

    /**
	 * 
	 * @param serviceUuid
	 * @param vfiName
	 * @param requestId
	 * @throws Exception
	 */
	private void verifyVfiExists(String serviceUuid, String vfiName, String requestId) throws Exception {
		ServiceDetailed service = baseBusinessLogic.getSdcRestClient().getService(serviceUuid, requestId);
		boolean isServiceContainsVfi = null != service && !CollectionUtils.isEmpty(service.getResources()) && service.getResources().stream()
				.filter(vfi -> "VF".equals(vfi.getResoucreType()))
				.anyMatch(vfi -> vfiName.equals(vfi.getResourceInstanceName()));
		if (!isServiceContainsVfi) {
			ResponseFormat responseFormat = ErrConfMgr.INSTANCE.getResponseFormat(ActionStatus.VFI_FETCH_ERROR, null, serviceUuid, vfiName);
			throw new DcaeException(HttpStatus.NOT_FOUND, responseFormat.getRequestError());
		}
	}
}

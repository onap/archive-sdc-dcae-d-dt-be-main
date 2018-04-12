package org.onap.sdc.dcae.composition.controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.Asset;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.Catalog.*;
import org.onap.sdc.dcae.catalog.engine.*;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.enums.LifecycleOperationType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ErrConfMgr.ApiType;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@EnableAutoConfiguration
@CrossOrigin
public class CompositionController extends BaseController{

    @Autowired
    private CatalogController catalogController;

    @PostConstruct
    public void init() {
        catalogController.setDefaultCatalog(URI.create(systemProperties.getProperties().getProperty(DcaeBeConstants.Config.ASDC_CATALOG_URL)));
    }

    @RequestMapping(value = { "/utils/clone/{assetType}/{sourceId}/{targetId}" }, method = {RequestMethod.GET }, produces = { "application/json" })
    public ResponseEntity clone(@RequestHeader("USER_ID") String userId, @PathVariable("assetType") String theAssetType, @PathVariable("sourceId") String theSourceId, @PathVariable("targetId") String theTargetId,
            @ModelAttribute("requestId") String requestId) {
        MessageResponse response = new MessageResponse();

        try {
            // fetch the source and assert it is a vfcmt containing clone worthy artifacts (composition + rules)
            ResourceDetailed sourceVfcmt = baseBusinessLogic.getSdcRestClient().getResource(theSourceId, requestId);
            checkVfcmtType(sourceVfcmt);
            List<Artifact> artifactsToClone = CollectionUtils.isEmpty(sourceVfcmt.getArtifacts()) ? null : sourceVfcmt.getArtifacts().stream()
                    .filter(p -> DcaeBeConstants.Composition.fileNames.COMPOSITION_YML.equals(p.getArtifactName()) || p.getArtifactName().endsWith(DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX))
                    .collect(Collectors.toList());
            if(CollectionUtils.isEmpty(artifactsToClone)) {
                response.setSuccessResponse("Nothing to clone");
                return new ResponseEntity<>(response ,HttpStatus.NO_CONTENT);
            }

            // fetch the target
            ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(theTargetId, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
            checkVfcmtType(vfcmt);
            checkUserIfResourceCheckedOut(userId, vfcmt);
            boolean isTargetNeed2Checkout = isNeedToCheckOut(vfcmt.getLifecycleState());
            if (isTargetNeed2Checkout) {
                ResourceDetailed targetVfcmt = baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, theTargetId, LifecycleOperationType.CHECKOUT.name(), "checking out VFCMT before clone", requestId);
                if(null == targetVfcmt){
                    return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERAL_ERROR);
                }
                theTargetId = targetVfcmt.getUuid();
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New targetVfcmt (for artifact clone) after checkout is: {}", theTargetId);
            }

            Map<String, Artifact> currentArtifacts = CollectionUtils.isEmpty(vfcmt.getArtifacts()) ? new HashMap<>() : vfcmt.getArtifacts().stream()
                    .collect(Collectors.toMap(Artifact::getArtifactName, Function.identity()));

            //TODO target VFCMT rule artifacts should be removed
            for(Artifact artifactToClone : artifactsToClone) {
                String payload = baseBusinessLogic.getSdcRestClient().getResourceArtifact(theSourceId, artifactToClone.getArtifactUUID(), requestId);
                baseBusinessLogic.cloneArtifactToTarget(userId, theTargetId, payload, artifactToClone, currentArtifacts.get(artifactToClone.getArtifactName()), requestId);
            }

            baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, theTargetId, LifecycleOperationType.CHECKIN.name(), "check in VFCMT after clone", requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Cloning {} from {} has finished successfully", theSourceId, theTargetId);
            response.setSuccessResponse("Clone VFCMT complete");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.CLONE_VFCMT);
        }
    }

    @RequestMapping(value = "/elements", method = { RequestMethod.POST, RequestMethod.GET }, produces = "application/json")
    public DeferredResult<CatalogResponse> items(@RequestBody(required = false) ItemsRequest theRequest) {

        final ItemsRequest request = (theRequest == null) ? ItemsRequest.EMPTY_REQUEST : theRequest;

        Catalog catalog = catalogController.getCatalog(request.getCatalog());
        DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());

        catalog.rootsByLabel(request.getStartingLabel())
                .setHandler(catalogController.new CatalogHandler<Folders>(request, result) {
                    public CatalogResponse handleData(Folders theFolders) {
                        JSONArray ja = new JSONArray();
                        if (theFolders != null) {
                            for (Folder folder : theFolders) {
                                ja.put(catalogController.patchData(catalog, folder.data()));
                            }
                        }
                        CatalogResponse response = new CatalogResponse(this.request);
                        try {
                            response.data().put("elements", ja);
                        } catch (JSONException e) {
                            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "JSONException putting json elements to response {}", e);
                        }
                        return response;
                    }
                });
        return result;
    }

    @RequestMapping(value = "/{theItemId}/elements", method = { RequestMethod.POST, RequestMethod.GET }, produces = "application/json")
    public DeferredResult<CatalogResponse> items(@RequestBody(required = false) ItemsRequest theRequest, @PathVariable String theItemId) {

        final ItemsRequest request = (theRequest == null) ? ItemsRequest.EMPTY_REQUEST : theRequest;

        Catalog catalog = catalogController.getCatalog(request.getCatalog());
        DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());

        catalog
                // .fetchFolderByItemId(theItemId)
                .folder(theItemId).withParts().withPartAnnotations().withItems().withItemAnnotations().withItemModels()
                .execute().setHandler(catalogController.new CatalogHandler<Folder>(request, result) {
                    public CatalogResponse handleData(Folder theFolder) {
                        CatalogResponse response = new CatalogResponse(this.request);
                        if (theFolder == null) {
                            return response;
                        }

                        try {
                            Elements folders = theFolder.elements("parts", Folders.class);
                            if (folders != null) {
                                for (Object folder : folders) {
                                    catalogController.patchData(catalog, ((Element) folder).data());
                                    // lots of ephemere proxies created here ..
                                    Elements annotations = ((Element) folder).elements("annotations",
                                            Annotations.class);
                                    if (annotations != null) {
                                        for (Object a : annotations) {
                                            catalogController.patchData(catalog, ((Annotation) a).data());
                                        }
                                    }
                                }
                            }
                            Elements items = theFolder.elements("items", Items.class);
                            if (items != null) {
                                for (Object i : items) {
                                    catalogController.patchData(catalog, ((Element) i).data());
                                    // lots of ephemere proxies created here ..
                                    Elements annotations = ((Element) i).elements("annotations", Annotations.class);
                                    if (annotations != null) {
                                        for (Object a : annotations) {
                                            catalogController.patchData(catalog, ((Annotation) a).data());
                                        }
                                    }
                                }
                            }
                        } catch (Exception x) {
                            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Exception processing catalog {}", x);
                            return new CatalogError(this.request, "", x);
                        }

                        try {
                            response.data().put("element", theFolder.data());
                        } catch (JSONException e) {
                            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "JSONException putting element to response {}", e);
                        }
                        return response;
                    }
                });

        return result;
    }

    @RequestMapping(value = "/{theItemId}/model", method = { RequestMethod.POST,RequestMethod.GET }, produces = "application/json")
    public DeferredResult model(@RequestBody(required = false) ElementRequest theRequest,
            @PathVariable String theItemId) {
        final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;

        Catalog catalog = catalogController.getCatalog(request.getCatalog());
        DeferredResult<CatalogResponse> result = new DeferredResult<>(request.getTimeout());

        catalog
                .item(theItemId).withModels().execute()
                .setHandler(catalogController.new CatalogHandler<Item>(request, result) {
                    public CatalogResponse handleData(Item theItem) {
                        if (theItem == null) {
                            return new CatalogError(this.request, "No such item");
                        }
                        Templates models = null;
                        try {
                            models = (Templates) theItem.elements("models", Templates.class);
                            if (models == null || models.isEmpty()) {
                                return new CatalogError(this.request, "Item has no models");
                            }
                            if (models.size() > 1) {
                                return new CatalogError(this.request, "Item has more than one model !?");
                            }
                            catalog.template(models.get(0).id()).withInputs().withOutputs().withNodes()
                                    .withNodeProperties().withNodePropertiesAssignments().withNodeRequirements()
                                    .withNodeCapabilities().withNodeCapabilityProperties()
                                    .withNodeCapabilityPropertyAssignments().withPolicies().withPolicyProperties()
                                    .withPolicyPropertiesAssignments().execute().setHandler(
                                            catalogController.new CatalogHandler<Template>(this.request, this.result) {
                                                public CatalogResponse handleData(Template theTemplate) {
                                                    CatalogResponse response = new CatalogResponse(this.request);
                                                    if (theTemplate != null) {
                                                        try {
                                                            response.data().put("model", catalogController
                                                                    .patchData(catalog, theTemplate.data()));
                                                        } catch (JSONException e) {
                                                            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "JSONException putting model to response {}", e);
                                                        }
                                                    }
                                                    return response;
                                                }
                                            });
                        } catch (Exception e) {
                            handleException(e, ApiType.GET_MODEL, models.get(0).name());
                        }
                        return null;
                    }
                });

        return result;
    }

    @RequestMapping(value = "/{theItemId}/type/{theTypeName}", method = { RequestMethod.POST, RequestMethod.GET }, produces = "application/json")
    public DeferredResult<CatalogResponse> model(@RequestBody(required = false) ElementRequest theRequest, @PathVariable String theItemId, @PathVariable String theTypeName) {
        final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;

        Catalog catalog = catalogController.getCatalog(request.getCatalog());
        DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());

        catalog.type(theItemId, theTypeName).withHierarchy().withCapabilities().withRequirements().execute()
                .setHandler(catalogController.new CatalogHandler<Type>(request, result) {
                    public CatalogResponse handleData(Type theType) {
                        CatalogResponse response = new CatalogResponse(this.request);
                        if (theType != null) {
                            try {
                                response.data().put("type", catalogController.patchData(catalog, theType.data()));
                            } catch (JSONException e) {
                                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Exception processing catalog {}", e);
                            }
                        }
                        return response;
                    }
                });

        return result;
    }

    @RequestMapping(value = { "/getComposition/{vfcmtUuid}" }, method = { RequestMethod.GET }, produces = {"application/json" })
    public ResponseEntity getComposition(@PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId) {
        MessageResponse response = new MessageResponse();
        try {
            ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(vfcmtUuid, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), vfcmt.toString());
            checkVfcmtType(vfcmt);

            Artifact compositionArtifact = CollectionUtils.isEmpty(vfcmt.getArtifacts()) ? null : vfcmt.getArtifacts().stream().filter(a -> DcaeBeConstants.Composition.fileNames.COMPOSITION_YML.equals(a.getArtifactName())).findAny().orElse(null);

            if(null == compositionArtifact){
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Couldn't find {} in VFCMT artifacts", DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
                response.setErrorResponse("No Artifacts");
                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }

            String artifact = baseBusinessLogic.getSdcRestClient().getResourceArtifact(vfcmtUuid, compositionArtifact.getArtifactUUID(), requestId);

            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "ARTIFACT: {}", artifact);
            response.setSuccessResponse(artifact);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.GET_CDUMP);
        }
    }

    @RequestMapping(value = "/saveComposition/{vfcmtUuid}", method = RequestMethod.POST)
    public ResponseEntity saveComposition(@RequestHeader("USER_ID") String userId, @RequestBody String theCdump, @PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "ARTIFACT CDUMP: {}", theCdump);

        try {

            ResourceDetailed vfcmt = baseBusinessLogic.getSdcRestClient().getResource(vfcmtUuid, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "VFCMT: {}", vfcmt);

            checkVfcmtType(vfcmt);
            checkUserIfResourceCheckedOut(userId, vfcmt);
            boolean isNeed2Checkout = isNeedToCheckOut(vfcmt.getLifecycleState());
            Artifact compositionArtifact = CollectionUtils.isEmpty(vfcmt.getArtifacts()) ? null : vfcmt.getArtifacts().stream().filter(a -> DcaeBeConstants.Composition.fileNames.COMPOSITION_YML.equals(a.getArtifactName())).findAny().orElse(null);
            String resourceUuid = vfcmtUuid; // by default the resource is the original vfcmtId unless a checkout will be done
            if (isNeed2Checkout) {
                vfcmt = baseBusinessLogic.getSdcRestClient().changeResourceLifecycleState(userId, resourceUuid, LifecycleOperationType.CHECKOUT.name(), null, requestId);
                if (vfcmt != null) {
                    resourceUuid = vfcmt.getUuid();
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New resource after checkout is: {}", resourceUuid);
                }
            }
            boolean isUpdateMode = null != compositionArtifact;
            if (isUpdateMode) {
                compositionArtifact.setDescription("updating composition file");
                compositionArtifact.setPayloadData(Base64Utils.encodeToString(theCdump.getBytes()));
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "VFCMT {} does consist {} ----> updateMode", resourceUuid, DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
                baseBusinessLogic.getSdcRestClient().updateResourceArtifact(userId, resourceUuid, compositionArtifact, requestId);

            } else {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "VFCMT {} does not consist {} ----> createMode", resourceUuid, DcaeBeConstants.Composition.fileNames.COMPOSITION_YML);
                compositionArtifact = SdcRestClientUtils.generateDeploymentArtifact("creating composition file", DcaeBeConstants.Composition.fileNames.COMPOSITION_YML, ArtifactType.DCAE_TOSCA.name(), "composition", theCdump.getBytes());
                baseBusinessLogic.getSdcRestClient().createResourceArtifact(userId, resourceUuid, compositionArtifact, requestId);
            }
            Asset result = checkin(userId, resourceUuid, org.onap.sdc.dcae.enums.AssetType.RESOURCE, requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "vfcmt check-in result: {}", result);

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return handleException(e, ApiType.SAVE_CDUMP);
        }
    }
}

package org.onap.sdc.dcae.composition.impl;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.engine.CatalogController;
import org.onap.sdc.dcae.catalog.engine.CatalogResponse;
import org.onap.sdc.dcae.catalog.engine.ElementRequest;
import org.onap.sdc.dcae.composition.restmodels.canvas.DcaeComponentCatalog;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.AssetType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class CompositionCatalogBusinessLogic extends BaseBusinessLogic {

	@Autowired
	private CatalogController catalogController;

	protected OnapLoggerError errLogger = OnapLoggerError.getInstance();

	public ResponseEntity getModelById(String requestId, String theItemId) {

		try {
			ResourceDetailed resourceDetailed = catalogController.getCatalog().hasCachedItem(theItemId) ? fetchCachedArtifactsMetadata(theItemId, requestId) : fetchAndExtractTemplateAndSchema(theItemId, requestId);
			Future<Catalog.Template> modelFuture = catalogController.getCatalog().template(resourceDetailed).withInputs().withOutputs().withNodes().withNodeProperties().withNodePropertiesAssignments().withNodeRequirements().withNodeCapabilities().withNodeCapabilityProperties()
					.withNodeCapabilityPropertyAssignments().withPolicies().withPolicyProperties().withPolicyPropertiesAssignments().execute();
			if(modelFuture.succeeded()) {
				CatalogResponse response = new CatalogResponse(ElementRequest.EMPTY_REQUEST);
				response.data().put("model", modelFuture.result().data());
				return ResponseEntity.ok().body(response);
			}
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERAL_ERROR, modelFuture.cause().getMessage());

		} catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error fetching catalog model with id {}. Message: {}", theItemId, e);
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_MODEL, theItemId);
		}
	}


	public ResponseEntity getTypeInfo(String theItemId, String theTypeName, String requestId) {

		try {
			// temporary patch - precede with caching verification //
			if(!catalogController.getCatalog().hasCachedItem(theItemId)) {
				ResourceDetailed resourceDetailed = fetchAndExtractTemplateAndSchema(theItemId, requestId);
				catalogController.getCatalog().template(resourceDetailed).execute();
			}
			//         //         //         //         //         //
			Future<Catalog.Type> theTypeInfoFuture = catalogController.getCatalog().type(theItemId, theTypeName).withHierarchy().withCapabilities().withRequirements().execute();
			if(theTypeInfoFuture.succeeded()) {
				CatalogResponse response = new CatalogResponse(ElementRequest.EMPTY_REQUEST);
				response.data().put("type", theTypeInfoFuture.result().data());
				return ResponseEntity.ok().body(response);
			}
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERAL_ERROR, theTypeInfoFuture.cause().getMessage());

		} catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Exception processing catalog {}", e);
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_MODEL, theItemId);
		}
	}

	public DcaeComponentCatalog getCatalog(String requestId) {
		List<DcaeComponentCatalog.SubCategoryFolder> folders = sdcRestClient.getResources(AssetType.VF.name(), "DCAE Component", null, requestId).stream()
				.filter(r -> DcaeBeConstants.LifecycleStateEnum.CERTIFIED == DcaeBeConstants.LifecycleStateEnum.findState(r.getLifecycleState()))
				.collect(Collectors.groupingBy(Resource::getSubCategory)).entrySet().stream()
				.map(e -> new DcaeComponentCatalog.SubCategoryFolder(e.getKey(), e.getValue())).collect(Collectors.toList());
		DcaeComponentCatalog catalog = new DcaeComponentCatalog();
		catalog.setElements(folders);
	    return catalog;
	}


	private ResourceDetailed fetchAndExtractTemplateAndSchema(String uuid, String requestId) throws IOException {
		String toscaModelPath = toscaModelPath(uuid);
		ResourceDetailed resourceDetailed = new ResourceDetailed();
		resourceDetailed.setUuid(uuid);
		resourceDetailed.setToscaModelURL(toscaModelPath);
		resourceDetailed.setArtifacts(extractToscaArtifactsFromCsar(sdcRestClient.getResourceToscaModel(uuid, requestId), toscaModelPath));
		return resourceDetailed;
	}

	private ResourceDetailed fetchCachedArtifactsMetadata(String uuid, String requestId) throws IOException {
		String toscaModelPath = toscaModelPath(uuid);
		ResourceDetailed cachedVf = sdcRestClient.getResource(uuid, requestId);
		cachedVf.getArtifacts().forEach(a -> a.setArtifactURL(toscaModelPath.concat(a.getArtifactName())));
		return cachedVf;
	}

	private List<Artifact> extractToscaArtifactsFromCsar(byte[] csar, String toscaModelPath) throws IOException {
		//we are only interested in unzipping files under Artifacts/Deployment/DCAE_TOSCA/
		String dcaeToscaDir = "Artifacts/Deployment/DCAE_TOSCA/";
		List<Artifact> extracted = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(csar))) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				if(ze.getName().startsWith(dcaeToscaDir)) {
					String artifactName = ze.getName().replace(dcaeToscaDir,"");
					extracted.add(SdcRestClientUtils.generateCatalogDcaeToscaArtifact(artifactName, toscaModelPath.concat(artifactName), extractFile(zis)));
				}
				ze = zis.getNextEntry();
			}
			return extracted;
		}
	}

	private String toscaModelPath(String uuid) {
		return "/sdc/v1/catalog/resources/".concat(uuid).concat("/toscaModel/");
	}

}


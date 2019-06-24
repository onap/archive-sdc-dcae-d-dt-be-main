package org.onap.sdc.dcae.composition.impl;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.catalog.commons.Recycler;
import org.onap.sdc.dcae.composition.restmodels.MessageResponse;
import org.onap.sdc.dcae.composition.restmodels.VfcmtData;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@ConfigurationProperties(prefix="blueprinter")
public class BlueprintBusinessLogic extends CompositionBusinessLogic {

	private String uri;

	public void setUri(String uri) {
		this.uri = uri;
	}

    public ResponseEntity generateAndSaveBlueprint(String userId, String context, String vfcmtUuid, String serviceUuid, String vfiName, String flowType, String requestId) {
        try {
            // prepare - fetch vfcmt and cdump
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            Artifact cdumpArtifactData = fetchCdump(vfcmt, requestId);
            if (null == cdumpArtifactData) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Composition not found on vfcmt {}", vfcmtUuid);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.MISSING_TOSCA_FILE, "", vfcmt.getName());
            }
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Found the cdump (composition.yml) on top of VFCMT {}", vfcmtUuid);
            String cdump = cdumpArtifactData.getPayloadData();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Going to use python procedure to create a blueprint....");

            String input = prepareInput(cdump, requestId);
            if (null == input) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SUBMIT_BLUEPRINT_ERROR);
			}
			String resultBlueprintCreation = new RestTemplate().postForObject(uri, new HttpEntity<>(input), String.class);

            if (StringUtils.isEmpty(resultBlueprintCreation)) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred during blueprint generation");
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.GENERATE_BLUEPRINT_ERROR, "", vfcmt.getName());
            }
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "BLUEPRINT:\n{}", resultBlueprintCreation);

            // 1806 US374595 flowType in cdump
            String flowTypeFromCdump = extractFlowTypeFromCdump(cdump);

            // support backward compatibility
            if(StringUtils.isBlank(flowTypeFromCdump)) {
                flowTypeFromCdump = flowType;
            }

            VfcmtData vfcmtData = new VfcmtData(vfcmt, vfiName, flowTypeFromCdump, serviceUuid);
            Artifact blueprintArtifactResult = submitComposition(userId, context, vfcmtData, resultBlueprintCreation, requestId);
            if (null == blueprintArtifactResult) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SUBMIT_BLUEPRINT_ERROR);
            }

            MessageResponse response = new MessageResponse();
            response.setSuccessResponse("Blueprint build complete \n. Blueprint=" + blueprintArtifactResult.getArtifactName());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), e.getMessage());
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SUBMIT_BLUEPRINT);
        }
    }

	private String prepareInput(String cdump, String requestId) throws IOException {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "fetched cdump payload: {}", cdump);
		Map	cdumpToTosca = new Recycler().recycle(new StringReader(cdump));
		Set<String> dcaeComponentsUuids = extractComponentUuids(cdumpToTosca);
		List<Map> extractedModels = Collections.synchronizedSet(dcaeComponentsUuids).parallelStream().map(id -> fetchAndExtractModel(id, requestId)).filter(Objects::nonNull).collect(Collectors.toList());
		// aggregation of parallel stream fetch results - exceptions are swallowed and we check the final size to verify no errors occurred
		if(dcaeComponentsUuids.size() != extractedModels.size()) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "error: {} distinct DCAE components were mapped to {} tosca lab input models.", dcaeComponentsUuids.size(), extractedModels.size());
			return null;
		}
		return new Gson().toJson(new ToscaLabInput(Base64Utils.encodeToString(new Yaml().dump(cdumpToTosca).getBytes()), extractedModels));
	}

	private Set<String> extractComponentUuids(Map cdump) {
		//the node description contains the UUID of the resource declaring it
		//if the description is the URI the resource uuid is the 5th path element (backward compatibility)
		// TODO there has to be a better way
		Map<String, Map<String, Object>> nodes = (Map<String, Map<String, Object>>)((Map<String, Object>)cdump.get("topology_template")).get("node_templates");
		return nodes.values().stream()
				.map(n -> (String)n.get("description"))
				.filter(StringUtils::isNotBlank)
				.map(d -> StringUtils.substringBetween(d, "resources/", "/"))
						.collect(Collectors.toSet());
	}


	private class ToscaLabInput {
    	private String template;
    	private List<Map> models;

    	ToscaLabInput(String template, List<Map> models){
    		this.template = template;
    		this.models = models;
		}
	}

	private Map<String, String> fetchAndExtractModel(String uuid, String requestId) {
    	try {
			return extractModelFromCsar(sdcRestClient.getResourceToscaModel(uuid, requestId));
		} catch (Exception e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "model extraction error: {}", e);
			return null;
		}
	}

	private Map<String, String> extractModelFromCsar(byte[] csar) throws IOException {
		//we are only interested in unzipping the 3 files under Artifacts/Deployment/DCAE_TOSCA/
		String dcaeToscaDir = "Artifacts/Deployment/DCAE_TOSCA/";
		Map<String, String> extracted = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(csar))) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null && 3 != extracted.size()) {
				if(ze.getName().startsWith(dcaeToscaDir)) {
					extracted.put(ze.getName().replace(dcaeToscaDir,"").split("\\.")[0], Base64Utils.encodeToString(extractFile(zis)));
				}
				ze = zis.getNextEntry();
			}
			return extracted;
		}
	}
}

package org.onap.sdc.dcae.composition.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonParseException;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.CompositionConfig;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.*;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.enums.ArtifactType;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ServiceException;
import org.onap.sdc.dcae.rule.editor.impl.RulesBusinessLogic;
import org.onap.sdc.dcae.rule.editor.utils.RulesPayloadUtils;
import org.onap.sdc.dcae.utils.Normalizers;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.onap.sdc.dcae.ves.VesDataItemsDefinition;
import org.onap.sdc.dcae.ves.VesDataTypeDefinition;
import org.onap.sdc.dcae.ves.VesSimpleTypesEnum;
import org.onap.sdc.dcae.ves.VesStructureLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RuleEditorBusinessLogic extends BaseBusinessLogic {

    @Autowired
    private RulesBusinessLogic rulesBusinessLogic;
    @Autowired
    private CompositionConfig compositionConfig;

    private static final String EXCEPTION = "Exception {}";

    public ResponseEntity saveRule(String json, String requestId, String userId, String vfcmtUuid, String dcaeCompLabel, String nid, String configParam) {

        try {
            Rule rule = RulesPayloadUtils.parsePayloadToRule(json);
            if (null == rule) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT);
            }

            List<ServiceException> errors = rulesBusinessLogic.validateRule(rule);
            if (!errors.isEmpty()) {
                return ErrConfMgr.INSTANCE.buildErrorArrayResponse(errors);
            }

            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            checkVfcmtType(vfcmt);

            if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SAVE_RULE_FAILED);
            }

            String artifactLabel = Normalizers.normalizeArtifactLabel(dcaeCompLabel + nid + configParam);

            // check for MappingRules artifact in existing artifacts
            Artifact artifactFound = vfcmt.getArtifacts().stream()
                    .filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel())))
                    .findAny().orElse(null);

            // exception thrown if vfcmt is checked out and current user is not its owner
            // performs checkoutVfcmt if required
            String vfcmtId = assertOwnershipOfVfcmtId(userId, vfcmt, requestId);
            // new mappingRules artifact, validate nid exists in composition before creating new artifact
            if (null == artifactFound) {
                if (cdumpContainsNid(vfcmt, nid, requestId)) {
                    return saveNewRulesArtifact(rule, vfcmtId, generateMappingRulesFileName(dcaeCompLabel, nid, configParam), artifactLabel, userId, requestId);
                }
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.NODE_NOT_FOUND, "", dcaeCompLabel);
            }

            //update artifact flow - append new rule or edit existing rule
            return addOrEditRuleInArtifact(rule, vfcmtId, userId, artifactFound, requestId);

        } catch (JsonParseException je) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Rule format is invalid: {}", je);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", je.getMessage());
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
        }

    }

    public ResponseEntity getRules(String vfcmtUuid, String dcaeCompLabel, String nid, String configParam, String requestId) {

        try {
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
                return new ResponseEntity<>("{}", HttpStatus.OK);
            }
            String artifactLabel = Normalizers.normalizeArtifactLabel(dcaeCompLabel + nid + configParam);

            // check for MappingRules artifact in existing artifacts
            Artifact artifactListed = vfcmt.getArtifacts().stream().filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel()))).findAny().orElse(null);
            if (null == artifactListed) {
                return new ResponseEntity<>("{}", HttpStatus.OK);
            }
            String ruleFile = getSdcRestClient().getResourceArtifact(vfcmtUuid, artifactListed.getArtifactUUID(), requestId);

            // To avoid opening the file for reading we search for the eventType and SchemaVer from the artifact metadata's description
            SchemaInfo schemainfo = RulesPayloadUtils.extractInfoFromDescription(artifactListed);
            List<EventTypeDefinitionUI> schema = null == schemainfo ? new ArrayList<>() : getEventTypeDefinitionUIs(schemainfo.getVersion(), schemainfo.getEventType());
            return new ResponseEntity<>(RulesPayloadUtils.buildSchemaAndRulesResponse(ruleFile, schema), HttpStatus.OK);
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_RULE_ARTIFACT);
        }

    }

    public ResponseEntity deleteRule(String userId, String vfcmtUuid, String dcaeCompLabel, String nid, String configParam, String ruleUid, String requestId) {

        try {
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            if (null == vfcmt.getArtifacts()) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "VFCMT {} doesn't have artifacts", vfcmtUuid);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.DELETE_RULE_FAILED);
            }
            String artifactLabel = Normalizers.normalizeArtifactLabel(dcaeCompLabel + nid + configParam);

            // check for MappingRules artifact in existing artifacts
            Artifact mappingRuleFile = vfcmt.getArtifacts().stream()
                    .filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel())))
                    .findAny().orElse(null);

            if (null == mappingRuleFile) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "{} doesn't exist for VFCMT {}", artifactLabel, vfcmtUuid);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.DELETE_RULE_FAILED);
            }

            String vfcmtId = assertOwnershipOfVfcmtId(userId, vfcmt, requestId);
            String payload = getSdcRestClient().getResourceArtifact(vfcmtId, mappingRuleFile.getArtifactUUID(), requestId);
            MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
            Rule removedRule = rulesBusinessLogic.deleteRule(rules, ruleUid);
            if (null == removedRule) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Rule {} not found.", ruleUid);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.DELETE_RULE_FAILED);
            }
            if (rules.isEmpty()) { // if file doesn't contain any rules after last deletion -> let's delete the file
                getSdcRestClient().deleteResourceArtifact(userId, vfcmtId, mappingRuleFile.getArtifactUUID(), requestId);
            } else {
                updateRulesArtifact(vfcmtId, userId, mappingRuleFile, rules, requestId);
            }
            return checkInAndReturnSaveArtifactResult(removedRule, vfcmtId, userId, requestId);
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
        }

    }

    public ResponseEntity translateRules(String vfcmtUuid, String requestId, String dcaeCompLabel, String nid, String configParam, String flowType) {

        try {

            if (StringUtils.isBlank(flowType) || MapUtils.isEmpty(compositionConfig.getFlowTypesMap()) || null == compositionConfig.getFlowTypesMap().get(flowType)) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.TRANSLATE_FAILED, "", "Flow type " + flowType + " not found");
            }

            // extract entry phase name and last phase name from configuration:
            String entryPointPhaseName = compositionConfig.getFlowTypesMap().get(flowType).getEntryPointPhaseName();
            String lastPhaseName = compositionConfig.getFlowTypesMap().get(flowType).getLastPhaseName();

            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            checkVfcmtType(vfcmt);

            if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.TRANSLATE_FAILED, "", "No rules found on VFCMT " + vfcmtUuid);
            }
            String artifactLabel = Normalizers.normalizeArtifactLabel(dcaeCompLabel + nid + configParam);

            // check for MappingRules artifact in existing artifacts
            Artifact rulesArtifact = vfcmt.getArtifacts().stream().filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel()))).findAny().orElse(null);

            if (rulesArtifact == null) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.TRANSLATE_FAILED, "", artifactLabel + " doesn't exist on VFCMT " + vfcmtUuid);
            }

            String payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, rulesArtifact.getArtifactUUID(), requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Retrieved mapping rules artifact {}, start parsing rules...", artifactLabel);
            MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished parsing rules, calling validator...");
            List<ServiceException> errors = rulesBusinessLogic.validateRules(rules);
            if (!errors.isEmpty()) {
                return ErrConfMgr.INSTANCE.buildErrorArrayResponse(errors);
            }

            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Validation completed successfully, calling translator...");
            String translateJson = rulesBusinessLogic.translateRules(rules, entryPointPhaseName, lastPhaseName, vfcmt.getName());
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translation completed successfully");
            return new ResponseEntity<>(translateJson, HttpStatus.OK);
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
        }
    }

    public ResponseEntity getExistingRuleTargets(String vfcmtUuid, String requestId, String dcaeCompLabel, String nid) {

        try {
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "getExistingRuleTargets - no rules found! returning zero response.");
                return new ResponseEntity<>("{}", HttpStatus.OK);
            }
            String artifactNameStartWith = dcaeCompLabel + "_" + nid + "_";

            List<String> artifactWithRules = vfcmt.getArtifacts().stream().filter(artifact ->
                    artifact.getArtifactName().startsWith(artifactNameStartWith))
                    .map(artifact -> StringUtils.substringBetween(artifact.getArtifactName(), artifactNameStartWith, DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX)).collect(Collectors.toList());
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "getExistingRuleTargets completed successfully");
            return new ResponseEntity<>(artifactWithRules, HttpStatus.OK);
        } catch (Exception e) {
            debugLogger.log(LogLevel.ERROR, this.getClass().getName(), "getExistingRuleTargets had exception", e);
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_RULE_ARTIFACT);
        }
    }

    public ResponseEntity getDefinition(String version, String eventType) {

        try {
            List<EventTypeDefinitionUI> result = getEventTypeDefinitionUIs(version, eventType);

            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), EXCEPTION, e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.VES_SCHEMA_NOT_FOUND);
        }
    }

    public ResponseEntity getEventsByVersion() {
        try {

            Map<String, Set<String>> eventsByVersions = VesStructureLoader.getAvailableVersionsAndEventTypes();

            List<EventTypesByVersionUI> resBody = eventsByVersions.entrySet().stream().map(entry -> {
                Set<String> events = entry.getValue().stream().filter(event -> !EventTypesByVersionUI.DEFAULT_EVENTS.contains(event)).collect(Collectors.toSet());
                return new EventTypesByVersionUI(entry.getKey(), events);
            }).collect(Collectors.toList());

            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Got a request to return all ves event types by versions {}", eventsByVersions);
            return new ResponseEntity<>(resBody, HttpStatus.OK);

        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), EXCEPTION, e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.VES_SCHEMA_NOT_FOUND);
        }
    }

    ///////////////////PRIVATE METHODS////////////////////////////////////////////////////////////////////////

    private String assertOwnershipOfVfcmtId(String userId, ResourceDetailed vfcmt, String requestId) {
        checkUserIfResourceCheckedOut(userId, vfcmt);
        String newVfcmtId = vfcmt.getUuid(); // may change after checking out a certified vfcmt
        if (isNeedToCheckOut(vfcmt.getLifecycleState())) {
            ResourceDetailed result = checkoutVfcmt(userId, newVfcmtId, requestId);
            if (result != null) {
                newVfcmtId = result.getUuid();
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "New resource after checkoutVfcmt is: {}", newVfcmtId);
            }
        }
        return newVfcmtId;
    }

    // called after validating vfcmt.getArtifacts() is not null
    private boolean cdumpContainsNid(ResourceDetailed vfcmt, String nid, String requestId) {
        Artifact cdump = vfcmt.getArtifacts().stream()
                .filter(a -> DcaeBeConstants.Composition.fileNames.COMPOSITION_YML.equalsIgnoreCase(a.getArtifactName()))
                .findAny().orElse(null);
        if (null == cdump || null == cdump.getArtifactUUID()) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "No {} found on vfcmt {}", DcaeBeConstants.Composition.fileNames.COMPOSITION_YML, vfcmt.getUuid());
            return false;
        }
        try {
            String artifact = getSdcRestClient().getResourceArtifact(vfcmt.getUuid(), cdump.getArtifactUUID(), requestId);
            if (!artifact.contains("\"nid\":\"" + nid)) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "{} doesn't contain nid {}. Cannot save mapping rule file", DcaeBeConstants.Composition.fileNames.COMPOSITION_YML, nid);
                return false;
            }
        } catch (Exception e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), EXCEPTION, e);
            return false;
        }
        return true;
    }

    private ResponseEntity<String> saveNewRulesArtifact(Rule rule, String vfcmtUuid, String artifactFileName, String artifactLabel, String userId, String requestId) throws JsonProcessingException {
        MappingRules body = new MappingRules(rule);
        Artifact artifact = SdcRestClientUtils.generateDeploymentArtifact(body.describe(), artifactFileName, ArtifactType.OTHER.name(), artifactLabel, body.convertToPayload());
        getSdcRestClient().createResourceArtifact(userId, vfcmtUuid, artifact, requestId);
        return checkInAndReturnSaveArtifactResult(rule, vfcmtUuid, userId, requestId);
    }

    private ResponseEntity addOrEditRuleInArtifact(Rule rule, String vfcmtUuid, String userId, Artifact rulesArtifact, String requestId) throws JsonProcessingException {
        String payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, rulesArtifact.getArtifactUUID(), requestId);
        MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);

        // in case the rule id is passed but the rule doesn't exist on the mapping rule file:
        if (!rulesBusinessLogic.addOrEditRule(rules, rule)) {
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SAVE_RULE_FAILED);
        }
        updateRulesArtifact(vfcmtUuid, userId, rulesArtifact, rules, requestId);
        return checkInAndReturnSaveArtifactResult(rule, vfcmtUuid, userId, requestId);
    }

    // regardless of check in result, return save artifact success
    private ResponseEntity<String> checkInAndReturnSaveArtifactResult(Rule rule, String vfcmtUuid, String userId, String requestId) {
        try {
            checkinVfcmt(userId, vfcmtUuid, requestId);
        } catch (Exception e) {
            // swallowing the exception intentionally since it is on the check in action
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred while performing check in on VFCMT {}:{}", vfcmtUuid, e);
        }
        return new ResponseEntity<>(rule.toJson(), HttpStatus.OK);
    }

    private void updateRulesArtifact(String vfcmtUuid, String userId, Artifact artifactInfo, MappingRules rules, String requestId) throws JsonProcessingException {
        artifactInfo.setPayloadData(Base64Utils.encodeToString(rules.convertToPayload()));
        // POST must contain 'description' while GET returns 'artifactDescription'
        artifactInfo.setDescription(artifactInfo.getArtifactDescription());
        getSdcRestClient().updateResourceArtifact(userId, vfcmtUuid, artifactInfo, requestId);
    }


    /**
     * @param eventMapStream
     * @param parent
     * @param path
     * @return
     */
    private List<EventTypeDefinitionUI> convertToEventTypeDefinition(Stream<Map.Entry<String, VesDataTypeDefinition>> eventMapStream, VesDataTypeDefinition parent, String path) {

        return eventMapStream.map(entry -> {
            Map<String, VesDataTypeDefinition> properties = entry.getValue().getProperties();
            VesDataItemsDefinition items = entry.getValue().getItems();
            String newPath = path + "." + entry.getKey();
            List<EventTypeDefinitionUI> children = (properties == null) ? null : convertToEventTypeDefinition(properties.entrySet().stream(), entry.getValue(), newPath);
            if (VesSimpleTypesEnum.ARRAY.getType().equals(entry.getValue().getType())) {
                newPath += "[]";
                if (innerTypeIsComplex(items)) {
                    children = convertComplexArrayType(items, newPath);
                } else if (innerTypeIsArray(items)) {
                    newPath += "[]";
                }
            }

            boolean isRequired = (parent != null) ? parent.getRequired().contains(entry.getKey()) : false;
            return new EventTypeDefinitionUI(entry.getKey(), children, isRequired, newPath);
        }).collect(Collectors.toList());
    }

    private boolean innerTypeIsComplex(VesDataItemsDefinition items) {
        return items != null && items.stream().anyMatch(p -> p.getProperties() != null);
    }

    private boolean innerTypeIsArray(VesDataItemsDefinition items) {
        return items != null && items.stream().anyMatch(p -> p.getItems() != null);
    }

    private List<EventTypeDefinitionUI> convertComplexArrayType(VesDataItemsDefinition items, String path) {
        return items.stream().map(item -> item.getProperties() != null ? convertToEventTypeDefinition(item.getProperties().entrySet().stream(), item, path) : new ArrayList<EventTypeDefinitionUI>())
                .flatMap(List::stream).collect(Collectors.toList());
    }


    private String generateMappingRulesFileName(String dcaeCompLabel, String nid, String configParam) {
        return dcaeCompLabel + "_" + nid + "_" + configParam + DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX;
    }

    private List<EventTypeDefinitionUI> getEventTypeDefinitionUIs(String version, String eventType) {
        List<String> eventNamesToReturn = ListUtils.union(EventTypesByVersionUI.DEFAULT_EVENTS, Arrays.asList(eventType));
        Map<String, VesDataTypeDefinition> eventDefs = VesStructureLoader.getEventListenerDefinitionByVersion(version);
        Stream<Map.Entry<String, VesDataTypeDefinition>> filteredEvents = eventDefs.entrySet().stream().filter(entry -> eventNamesToReturn.contains(entry.getKey()));

        return convertToEventTypeDefinition(filteredEvents, null, "event");
    }
}

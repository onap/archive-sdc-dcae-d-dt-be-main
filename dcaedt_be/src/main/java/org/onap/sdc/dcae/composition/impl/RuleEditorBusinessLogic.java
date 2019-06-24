package org.onap.sdc.dcae.composition.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonParseException;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.sdc.common.onaplog.enums.LogLevel;
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
import org.onap.sdc.dcae.rule.editor.utils.ValidationUtils;
import org.onap.sdc.dcae.rule.editor.validators.MappingRulesValidator;
import org.onap.sdc.dcae.utils.Normalizers;
import org.onap.sdc.dcae.utils.SdcRestClientUtils;
import org.onap.sdc.dcae.ves.VesDataItemsDefinition;
import org.onap.sdc.dcae.ves.VesDataTypeDefinition;
import org.onap.sdc.dcae.ves.VesSimpleTypesEnum;
import org.onap.sdc.dcae.ves.VesStructureLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RuleEditorBusinessLogic extends BaseBusinessLogic {

    @Autowired
    private RulesBusinessLogic rulesBusinessLogic;

	private MappingRulesValidator mappingRulesValidator = MappingRulesValidator.getInstance();

    private static final String EXCEPTION = "Exception {}";

    public ResponseEntity saveRule(String json, String requestId, String userId, String vfcmtUuid, String dcaeCompLabel, String nid, String configParam) {

        try {
            Rule rule = RulesPayloadUtils.parsePayloadToRule(json);
            if (null == rule) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", "");
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

            // new mappingRules artifact, validate nid exists in composition before creating new artifact
            if (null == artifactFound) {
                if (cdumpContainsNid(vfcmt, nid, requestId)) {
					MappingRules body = new MappingRules(rule);
					saveNewRulesArtifact(body, vfcmtUuid, generateMappingRulesFileName(dcaeCompLabel, nid, configParam), artifactLabel, userId, requestId);
					return checkInAndReturnSaveArtifactResult(rule.toJson(), vfcmtUuid, userId, requestId);
                }
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.NODE_NOT_FOUND, "", dcaeCompLabel);
            }

            //update artifact flow - append new rule or edit existing rule
            return addOrEditRuleInArtifact(rule, vfcmtUuid, userId, artifactFound, requestId);

        } catch (JsonParseException je) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Rule format is invalid: {}", je);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", je.getMessage());
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
        }
    }

	public ResponseEntity applyFilter(String json, String requestId, String userId) {

		try {
			ApplyFilterRequest request = RulesPayloadUtils.convertFromPayload(json, ApplyFilterRequest.class);
			if (null == request) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_CONTENT);
			}
			if(!validateMandatoryRequestFields(request)) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Invalid apply filter request. request: {}", request);
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.RULE_OPERATION_FAILED_MISSING_PARAMS);
			}
			List<ServiceException> errors = rulesBusinessLogic.validateFilter(request.getFilter());
			if (!errors.isEmpty()) {
				// this will return the first violation found by the validator to the UI view as a regular error and all violations to the console view
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, errors.stream().map(ServiceException::getFormattedErrorMessage).collect(Collectors.joining(", ")), errors.get(0).getFormattedErrorMessage());
			}

			ResourceDetailed vfcmt = getSdcRestClient().getResource(request.getVfcmtUuid(), requestId);
			checkVfcmtType(vfcmt);

			if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SAVE_RULE_FAILED);
			}

			String artifactLabel = Normalizers.normalizeArtifactLabel(request.getDcaeCompLabel() + request.getNid() + request.getConfigParam());

			// check for MappingRules artifact in existing artifacts
			Artifact artifactFound = vfcmt.getArtifacts().stream()
					.filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel())))
					.findAny().orElse(null);

			// new mappingRules artifact, validate nid exists in composition before creating new artifact
			if (null == artifactFound) {
				if (cdumpContainsNid(vfcmt, request.getNid(), requestId)) {
					MappingRules body = new MappingRules(request);
					saveNewRulesArtifact(body, vfcmt.getUuid(), generateMappingRulesFileName(request.getDcaeCompLabel(),request.getNid(), request.getConfigParam()), artifactLabel, userId, requestId);
					return checkInAndReturnSaveArtifactResult(request.getFilter(), vfcmt.getUuid(), userId, requestId);
				}
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.NODE_NOT_FOUND, "", request.getDcaeCompLabel());
			}

			//update artifact flow - apply filter to existing artifact
			return applyFilterToExistingArtifact(request, userId, artifactFound, requestId);

		} catch (JsonParseException je) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: request format is invalid: {}", je);
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", je.getMessage());
		} catch (Exception e) {
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
		}
	}

    public ResponseEntity getRulesAndSchema(String vfcmtUuid, String dcaeCompLabel, String nid, String configParam, String requestId) {

        try {
			ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
        	Artifact rulesArtifact = fetchRulesArtifact(vfcmt, dcaeCompLabel, nid, configParam, requestId);
        	if(null == rulesArtifact) {
				return new ResponseEntity<>("{}", HttpStatus.OK);
			}
            // To avoid opening the file for reading we search for the eventType and SchemaVer from the artifact metadata's description
            SchemaInfo schemainfo = RulesPayloadUtils.extractInfoFromDescription(rulesArtifact);
            List<EventTypeDefinitionUI> schema = null == schemainfo ? new ArrayList<>() : getEventTypeDefinitionUIs(schemainfo.getVersion(), schemainfo.getEventType());
            return new ResponseEntity<>(RulesPayloadUtils.buildSchemaAndRulesResponse(rulesArtifact.getPayloadData(), schema), HttpStatus.OK);
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_RULE_ARTIFACT);
        }
    }

    //1810 US423581 export rules
    public ResponseEntity downloadRules(String vfcmtUuid, String dcaeCompLabel, String nid, String configParam, String requestId) {

    	try {
			ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
			Artifact rulesArtifact = fetchRulesArtifact(vfcmt, dcaeCompLabel, nid, configParam, requestId);
			if(null == rulesArtifact) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"requested rules artifact not found");
				return new ResponseEntity(HttpStatus.NOT_FOUND);
			}
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, generateMappingRulesFileNameHeader(vfcmt.getName(), dcaeCompLabel, configParam))
					.body(rulesArtifact.getPayloadData());
		} catch (Exception e) {
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.GET_RULE_ARTIFACT);
		}
	}

	private Artifact fetchRulesArtifact(ResourceDetailed vfcmt, String dcaeCompLabel, String nid, String configParam, String requestId) {

		if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
			return null;
		}
		String artifactLabel = Normalizers.normalizeArtifactLabel(dcaeCompLabel + nid + configParam);

		// check for MappingRules artifact in existing artifacts
		Artifact artifactListed = vfcmt.getArtifacts().stream().filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel()))).findAny().orElse(null);
		if (null == artifactListed) {
			return null;
		}
		artifactListed.setPayloadData(getSdcRestClient().getResourceArtifact(vfcmt.getUuid(), artifactListed.getArtifactUUID(), requestId));
		return artifactListed;
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

            String payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, mappingRuleFile.getArtifactUUID(), requestId);
            MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
            Rule removedRule = rulesBusinessLogic.deleteRule(rules, ruleUid);
            if (null == removedRule) {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Rule {} not found.", ruleUid);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.DELETE_RULE_FAILED);
            }
            if (rules.isEmpty()) { // if file doesn't contain any rules after last deletion -> let's delete the file
                getSdcRestClient().deleteResourceArtifact(userId, vfcmtUuid, mappingRuleFile.getArtifactUUID(), requestId);
            } else {
                updateRulesArtifact(vfcmtUuid, userId, mappingRuleFile, rules, requestId);
            }
            return checkInAndReturnSaveArtifactResult(removedRule.toJson(), vfcmtUuid, userId, requestId);
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
        }
    }

	public ResponseEntity deleteFilter(String json, String requestId, String userId) {

		try {
			RuleEditorRequest request = RulesPayloadUtils.convertFromPayload(json, RuleEditorRequest.class);
			if (null == request) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_CONTENT);
			}
			if(!validateMandatoryRequestFields(request)) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Invalid delete filter request. request: {}", request);
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.RULE_OPERATION_FAILED_MISSING_PARAMS);
			}
			ResourceDetailed vfcmt = getSdcRestClient().getResource(request.getVfcmtUuid(), requestId);
			if (null == vfcmt.getArtifacts()) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "VFCMT {} doesn't have artifacts", vfcmt.getUuid());
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.FILTER_NOT_FOUND);
			}
			String artifactLabel = Normalizers.normalizeArtifactLabel(request.getDcaeCompLabel() + request.getNid() + request.getConfigParam());

			// check for MappingRules artifact in existing artifacts
			Artifact mappingRuleFile = vfcmt.getArtifacts().stream()
					.filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel())))
					.findAny().orElse(null);

			if (null == mappingRuleFile) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "{} doesn't exist for VFCMT {}", artifactLabel, vfcmt.getUuid());
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.FILTER_NOT_FOUND);
			}

			String payload = getSdcRestClient().getResourceArtifact(vfcmt.getUuid(), mappingRuleFile.getArtifactUUID(), requestId);
			MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
			BaseCondition deletedFilter = rules.getFilter();
			if (null == deletedFilter) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Filter not found.");
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.FILTER_NOT_FOUND);
			}
			if (rules.isEmpty()) { // if the file doesn't contain any rules -> let's delete the file
				getSdcRestClient().deleteResourceArtifact(userId, vfcmt.getUuid(), mappingRuleFile.getArtifactUUID(), requestId);
			} else {
				rules.setFilter(null);
				updateRulesArtifact(vfcmt.getUuid(), userId, mappingRuleFile, rules, requestId);
			}
			return checkInAndReturnSaveArtifactResult(deletedFilter, vfcmt.getUuid(), userId, requestId);
		} catch (Exception e) {
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
		}
	}

	public ResponseEntity deleteGroupOfRules(String userId, String vfcmtUuid, String dcaeCompLabel, String nid, String configParam, String groupId, String requestId) {

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

			String payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, mappingRuleFile.getArtifactUUID(), requestId);
			MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
			List<Rule> removedRules = rulesBusinessLogic.deleteGroupOfRules(rules, groupId);
			if (removedRules.isEmpty()) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Group {} not found.", groupId);
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.DELETE_RULE_FAILED);
			}
			if (rules.isEmpty()) { // if file doesn't contain any rules after last deletion -> let's delete the file
				getSdcRestClient().deleteResourceArtifact(userId, vfcmtUuid, mappingRuleFile.getArtifactUUID(), requestId);
			} else {
				updateRulesArtifact(vfcmtUuid, userId, mappingRuleFile, rules, requestId);
			}
			return checkInAndReturnSaveArtifactResult(removedRules, vfcmtUuid, userId, requestId);
		} catch (Exception e) {
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
		}

	}

    public ResponseEntity translateRules(TranslateRequest request, String requestId) {

		// 1810 US436244 MC table
    	String vfcmtUuid = request.getVfcmtUuid().split("/")[0];
        try {
        	if(!validateTranslateRequestFields(request)) {
        		errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Invalid translate request. request: {}", request);
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.TRANSLATE_FAILED, "", "please enter valid request parameters");
            }
            ResourceDetailed vfcmt = getSdcRestClient().getResource(vfcmtUuid, requestId);
            checkVfcmtType(vfcmt);

            if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.TRANSLATE_FAILED, "", "No rules found on VFCMT " + vfcmtUuid);
            }
            String artifactLabel = Normalizers.normalizeArtifactLabel(request.getDcaeCompLabel() + request.getNid() + request.getConfigParam());

            // check for MappingRules artifact in existing artifacts
            Artifact rulesArtifact = vfcmt.getArtifacts().stream().filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel()))).findAny().orElse(null);

            if (rulesArtifact == null) {
                return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.TRANSLATE_FAILED, "", artifactLabel + " doesn't exist on VFCMT " + vfcmtUuid);
            }

            String payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, rulesArtifact.getArtifactUUID(), requestId);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Retrieved mapping rules artifact {}, start parsing rules...", artifactLabel);
            MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
            rulesBusinessLogic.updateGlobalTranslationFields(rules, request, vfcmt.getName());
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Finished parsing rules, calling validator...");
            List<ServiceException> errors = rulesBusinessLogic.validateRulesBeforeTranslate(rules);
            if (!errors.isEmpty()) {
                return ErrConfMgr.INSTANCE.buildErrorArrayResponse(errors);
            }

            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Validation completed successfully, calling translator...");
            String translateJson = rulesBusinessLogic.translateRules(rules);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Translation completed successfully");
            return new ResponseEntity<>(translateJson, HttpStatus.OK);
        } catch (Exception e) {
            return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
        }
    }

    public ResponseEntity importRules(MappingRulesResponse mappingRules, String requestId, String userId, String vfcmtUuid, String dcaeCompLabel, String nid, String configParam) {
    	try {
    		if(!mappingRulesValidator.validateVersionAndType(mappingRules)) {
    			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Invalid or missing VES version definition");
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "Invalid or missing VES version definitions", "The imported rules artifact is unsupported");
			}
			List<ServiceException> errors = rulesBusinessLogic.validateImportedRules(mappingRules);
			if (!errors.isEmpty()) {
				// this will return the first violation found by the validator to the UI view as a regular error and all violations to the console view
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, errors.stream().map(ServiceException::getFormattedErrorMessage).collect(Collectors.joining(", ")), errors.get(0).getFormattedErrorMessage());
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

			// new mappingRules artifact, validate nid exists in composition before creating new artifact
			if (null == artifactFound) {
				if (!cdumpContainsNid(vfcmt, nid, requestId)) {
					return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.NODE_NOT_FOUND, "", dcaeCompLabel);
				}
				saveNewRulesArtifact(mappingRules, vfcmtUuid, generateMappingRulesFileName(dcaeCompLabel, nid, configParam), artifactLabel, userId, requestId);
			} else {
				updateRulesArtifact(vfcmtUuid, userId, artifactFound, mappingRules, requestId);
			}
			mappingRules.setSchema(getEventTypeDefinitionUIs(mappingRules.getVersion(), mappingRules.getEventType()));
			return checkInAndReturnSaveArtifactResult(mappingRules, vfcmtUuid, userId, requestId);

		} catch (JsonParseException je) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Rule format is invalid: {}", je);
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", je.getMessage());
		} catch (Exception e) {
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
		}
	}

	public ResponseEntity importPhase(String json, String requestId, String userId) {

		MappingRulesResponse allRules;
		try {
			ImportPhaseRequest request = RulesPayloadUtils.convertFromPayload(json, ImportPhaseRequest.class);
			if (null == request) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_CONTENT);
			}
			if(!validateImportPhaseRequestFields(request)) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Invalid import phase request. request: {}", request);
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.RULE_OPERATION_FAILED_MISSING_PARAMS);
			}
			if(!validateVesVersionAndEventDomainMatch(request)) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Invalid ves definitions, {} != {} or {} != {}", request.getEventType(), request.getPayload().getEventType(), request.getVersion(), request.getPayload().getVersion());
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", "The Imported rules VES version is not compatible with the current rules VES version");
			}
			MappingRulesResponse inputRules = request.getPayload();
			if(!validateEditorVersion(inputRules, false)) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", "The imported rules artifact version is not compatible with the current rule engine");
			}

			// set the groupId and phase to be saved
			inputRules.getRules().forEach((k,v) -> {
				v.setGroupId(request.getGroupId());
				v.setPhase(request.getPhase());
			});

			List<ServiceException> errors = rulesBusinessLogic.validateImportedRules(inputRules);
			if (!errors.isEmpty()) {
				// this will return the first violation found by the validator to the UI view as a regular error and all violations to the console view
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, errors.stream().map(ServiceException::getFormattedErrorMessage).collect(Collectors.joining(", ")), errors.get(0).getFormattedErrorMessage());
			}

			ResourceDetailed vfcmt = getSdcRestClient().getResource(request.getVfcmtUuid(), requestId);
			checkVfcmtType(vfcmt);

			if (CollectionUtils.isEmpty(vfcmt.getArtifacts())) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SAVE_RULE_FAILED);
			}

			String artifactLabel = Normalizers.normalizeArtifactLabel(request.getDcaeCompLabel() + request.getNid() + request.getConfigParam());

			// check for MappingRules artifact in existing artifacts
			Artifact artifactFound = vfcmt.getArtifacts().stream()
					.filter(a -> artifactLabel.equals(Normalizers.normalizeArtifactLabel(a.getArtifactLabel())))
					.findAny().orElse(null);

			// new mappingRules artifact, validate nid exists in composition before creating new artifact
			if (null == artifactFound) {
				if (!cdumpContainsNid(vfcmt, request.getNid(), requestId)) {
					return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.NODE_NOT_FOUND, "", request.getDcaeCompLabel());
				}
				saveNewRulesArtifact(inputRules, vfcmt.getUuid(), generateMappingRulesFileName(request.getDcaeCompLabel(), request.getNid(), request.getConfigParam()), artifactLabel, userId, requestId);
				allRules = inputRules;
			} else {
				allRules = RulesPayloadUtils.parsePayloadToMappingRules(getSdcRestClient().getResourceArtifact(vfcmt.getUuid(), artifactFound.getArtifactUUID(), requestId));
				allRules.getRules().putAll(inputRules.getRules());
				updateRulesArtifact(vfcmt.getUuid(), userId, artifactFound, allRules, requestId);
			}
			allRules.setSchema(getEventTypeDefinitionUIs(allRules.getVersion(), allRules.getEventType()));
			return checkInAndReturnSaveArtifactResult(allRules, vfcmt.getUuid(), userId, requestId);

		} catch (JsonParseException je) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Rule format is invalid: {}", je);
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", je.getMessage());
		} catch (Exception e) {
			return ErrConfMgr.INSTANCE.handleException(e, ErrConfMgr.ApiType.SAVE_RULE_ARTIFACT);
		}
	}

	// when importing a rules file all rules must match the declared format (supportGroups)
	public boolean validateEditorVersion(MappingRules rules, boolean supportGroups) {
		return supportGroups ? mappingRulesValidator.validateGroupDefinitions(rules) : rules.getRules().values().stream().noneMatch(r -> ValidationUtils.validateNotEmpty(r.getGroupId()));
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


	private void saveNewRulesArtifact(MappingRules mappingRules, String vfcmtUuid, String artifactFileName, String artifactLabel, String userId, String requestId) throws JsonProcessingException {
		Artifact artifact = SdcRestClientUtils.generateDeploymentArtifact(mappingRules.describe(), artifactFileName, ArtifactType.OTHER.name(), artifactLabel, mappingRules.convertToPayload());
		getSdcRestClient().createResourceArtifact(userId, vfcmtUuid, artifact, requestId);
	}

    private ResponseEntity addOrEditRuleInArtifact(Rule rule, String vfcmtUuid, String userId, Artifact rulesArtifact, String requestId) throws JsonProcessingException {
        String payload = getSdcRestClient().getResourceArtifact(vfcmtUuid, rulesArtifact.getArtifactUUID(), requestId);
        MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);

		// 1810 US427299 support user defined phase names
		boolean supportGroups = ValidationUtils.validateNotEmpty(rule.getGroupId());
		if(!rulesBusinessLogic.validateGroupDefinitions(rules, supportGroups)) {
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", "invalid group definitions");
		}
        // in case the rule id is passed but the rule doesn't exist on the mapping rule file or if there's a mismatch in group definitions:
        if (!rulesBusinessLogic.addOrEditRule(rules, rule, supportGroups)) {
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.SAVE_RULE_FAILED);
        }
        updateRulesArtifact(vfcmtUuid, userId, rulesArtifact, rules, requestId);
        return checkInAndReturnSaveArtifactResult(rule.toJson(), vfcmtUuid, userId, requestId);
    }

	private ResponseEntity applyFilterToExistingArtifact(ApplyFilterRequest request, String userId, Artifact rulesArtifact, String requestId) throws JsonProcessingException {
		String payload = getSdcRestClient().getResourceArtifact(request.getVfcmtUuid(), rulesArtifact.getArtifactUUID(), requestId);
		MappingRules rules = RulesPayloadUtils.parseMappingRulesArtifactPayload(payload);
		rules.setEntryPhase(request.getEntryPhase());
		rules.setPublishPhase(request.getPublishPhase());
		rules.setFilter(request.getFilter());
		updateRulesArtifact(request.getVfcmtUuid(), userId, rulesArtifact, rules, requestId);
		return checkInAndReturnSaveArtifactResult(request.getFilter(), request.getVfcmtUuid(), userId, requestId);
	}

    // regardless of check in result, return save artifact success
    private ResponseEntity checkInAndReturnSaveArtifactResult(Object response, String vfcmtUuid, String userId, String requestId) {
        try {
            checkinVfcmt(userId, vfcmtUuid, requestId);
        } catch (Exception e) {
            // swallowing the exception intentionally since it is on the check in action
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error occurred while performing check in on VFCMT {}:{}", vfcmtUuid, e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void updateRulesArtifact(String vfcmtUuid, String userId, Artifact artifactInfo, MappingRules rules, String requestId) throws JsonProcessingException {
        artifactInfo.setPayloadData(Base64Utils.encodeToString(rules.convertToPayload()));
        // POST must contain 'description' while GET returns 'artifactDescription'
        artifactInfo.setDescription(rules.describe());
        getSdcRestClient().updateResourceArtifact(userId, vfcmtUuid, artifactInfo, requestId);
    }

	private boolean validateTranslateRequestFields(TranslateRequest request) {
		return validateMandatoryRequestFields(request) && Stream.of(request.getPublishPhase(), request.getEntryPhase()).allMatch(ValidationUtils::validateNotEmpty)
				&& !request.getEntryPhase().equalsIgnoreCase(request.getPublishPhase());
	}

	private boolean validateImportPhaseRequestFields(ImportPhaseRequest request) {
		return validateMandatoryRequestFields(request) && Stream.of(request.getGroupId(), request.getPhase(), request.getVersion(), request.getEventType()).allMatch(ValidationUtils::validateNotEmpty) && null != request.getPayload();
	}

	private boolean validateMandatoryRequestFields(RuleEditorRequest request) {
		return Stream.of(request.getVfcmtUuid(), request.getDcaeCompLabel(), request.getNid(), request.getConfigParam()).allMatch(ValidationUtils::validateNotEmpty);
	}

	private boolean validateVesVersionAndEventDomainMatch(ImportPhaseRequest request) {
		return request.getPayload().getVersion().equalsIgnoreCase(request.getVersion()) && request.getPayload().getEventType().equalsIgnoreCase(request.getEventType());
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

	private String generateMappingRulesFileNameHeader(String vfcmtName, String dcaeCompLabel, String configParam) {
		return "attachment; filename=\""
				.concat(vfcmtName)
				.concat("_")
				.concat(dcaeCompLabel)
				.concat("_")
				.concat(configParam)
				.concat(DcaeBeConstants.Composition.fileNames.MAPPING_RULE_POSTFIX)
				.concat("\"");
	}

    private List<EventTypeDefinitionUI> getEventTypeDefinitionUIs(String version, String eventType) {
        List<String> eventNamesToReturn = ListUtils.union(EventTypesByVersionUI.DEFAULT_EVENTS, Arrays.asList(eventType));
        Map<String, VesDataTypeDefinition> eventDefs = VesStructureLoader.getEventListenerDefinitionByVersion(version);
        Stream<Map.Entry<String, VesDataTypeDefinition>> filteredEvents = eventDefs.entrySet().stream().filter(entry -> eventNamesToReturn.contains(entry.getKey()));

        return convertToEventTypeDefinition(filteredEvents, null, "event");
    }


}

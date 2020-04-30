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

import com.google.gson.JsonParseException;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.composition.impl.RuleEditorBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.MappingRulesResponse;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.TranslateRequest;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.rule.editor.utils.RulesPayloadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@EnableAutoConfiguration
@CrossOrigin
@RequestMapping("/rule-editor")
public class RuleEditorController extends BaseController {

    @Autowired
    private RuleEditorBusinessLogic ruleEditorBusinessLogic;

    @GetMapping(value = "/list-events-by-versions")
    public ResponseEntity getEventsByVersion() {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getEventsByVersion");
        return ruleEditorBusinessLogic.getEventsByVersion();
    }

    @GetMapping(value = {"/definition/{version:.*}/{eventType}"}, produces = {"application/json"})
    public ResponseEntity getDefinition(@PathVariable("version") String version,
                                        @PathVariable("eventType") String eventType) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getDefinition", version);
        return ruleEditorBusinessLogic.getDefinition(version, eventType);
    }

    /**
     * This endpoint functions as a 'create/update' service for the rule editor UI
     *
     * @param json          - json representing the saved rule
     * @param vfcmtUuid     - VFCMT that the rule editor ui is saved in
     * @param dcaeCompLabel - the name of the DCAE Component which the rule is applied to
     * @param nid           - A unique id of the DCAE Component which the rule is applied to - exists also in the cdump
     * @param configParam   - the name of the DCAE Component configuration property the rule is linked to
     * @return json representing the rule editor UI
     * Validations:
     * 1. That the user is able to edit the VFCMT
     * 2. That the cdump holds a dcae component with such nid (to avoid orphan rules)
     * 3. Check that the fetched VFCMT is actually a VFCMT and not a regular VF
     */
    @PostMapping(value = "/rule/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", produces = "application/json")
    public ResponseEntity saveRule(@RequestBody String json, @ModelAttribute("requestId") String requestId,
                                   @RequestHeader("USER_ID") String userId,
                                   @PathVariable("vfcmtUuid") String vfcmtUuid,
                                   @PathVariable("dcaeCompLabel") String dcaeCompLabel,
                                   @PathVariable("nid") String nid,
                                   @PathVariable("configParam") String configParam) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting saveRule", json);
        return ruleEditorBusinessLogic.saveRule(json, requestId, userId, vfcmtUuid, dcaeCompLabel, nid, configParam);
    }


	@PostMapping(value = "/applyFilter", produces = "application/json")
	public ResponseEntity applyFilter(@RequestBody String json, @ModelAttribute("requestId") String requestId, @RequestHeader("USER_ID") String userId) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting applyFilter", json);
		return ruleEditorBusinessLogic.applyFilter(json, requestId, userId);
	}

	@PostMapping(value = "/deleteFilter", produces = "application/json")
	public ResponseEntity deleteFilter(@RequestBody String json, @ModelAttribute("requestId") String requestId, @RequestHeader("USER_ID") String userId) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting deleteFilter", json);
		return ruleEditorBusinessLogic.deleteFilter(json, requestId, userId);
	}


    /**
     * This endpoint functions as a 'fetch' service for the rule editor UI
     *
     * @param vfcmtUuid     - VFCMT that the rule editor ui is saved in
     * @param dcaeCompLabel - the name of the DCAE Component which the rule is applied to
     * @param nid           - A unique id of the DCAE Component which the rule is applied to - exists also in the cdump
     * @param configParam   - the name of the DCAE Component configuration property the rule is linked to
     * @return json representing the rule editor UI
     */
    @GetMapping(value = "/rule/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", produces = "application/json")
    public ResponseEntity getRules(
            @PathVariable("vfcmtUuid") String vfcmtUuid,
            @PathVariable("dcaeCompLabel") String dcaeCompLabel,
            @PathVariable("nid") String nid,
            @PathVariable("configParam") String configParam,
            @ModelAttribute("requestId") String requestId) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getRulesAndSchema", vfcmtUuid);
        return ruleEditorBusinessLogic.getRulesAndSchema(vfcmtUuid, dcaeCompLabel, nid, configParam, requestId);
    }

	// 1810 US436244 MC table
	@GetMapping(value = "/rule/{vfcmtUuid}/{revertedUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", produces = "application/json")
	public ResponseEntity getRules(
			@PathVariable String vfcmtUuid,
			@PathVariable String revertedUuid,
			@PathVariable String dcaeCompLabel,
			@PathVariable String nid,
			@PathVariable String configParam,
			@ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getRulesAndSchema", vfcmtUuid);
		return ruleEditorBusinessLogic.getRulesAndSchema(vfcmtUuid, dcaeCompLabel, nid, configParam, requestId);
	}

	@GetMapping(value = "/export/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity downloadRules(
			@PathVariable("vfcmtUuid") String vfcmtUuid,
			@PathVariable("dcaeCompLabel") String dcaeCompLabel,
			@PathVariable("nid") String nid,
			@PathVariable("configParam") String configParam,
			@ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting exportRules", vfcmtUuid);
		return ruleEditorBusinessLogic.downloadRules(vfcmtUuid, dcaeCompLabel, nid, configParam, requestId);
	}

	// 1810 US436244 MC table
	@GetMapping(value = "/export/{vfcmtUuid}/{revertedUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity downloadRules(
			@PathVariable String vfcmtUuid,
			@PathVariable String revertedUuid,
			@PathVariable String dcaeCompLabel,
			@PathVariable String nid,
			@PathVariable String configParam,
			@ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting exportRules", vfcmtUuid);
		return ruleEditorBusinessLogic.downloadRules(vfcmtUuid, dcaeCompLabel, nid, configParam, requestId);
	}

	@PostMapping(value = "/import/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam}/{supportGroups}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity importRules(
			@RequestBody String json, @ModelAttribute("requestId") String requestId,
			@RequestHeader("USER_ID") String userId,
			@PathVariable("vfcmtUuid") String vfcmtUuid,
			@PathVariable("dcaeCompLabel") String dcaeCompLabel,
			@PathVariable("nid") String nid,
			@PathVariable("configParam") String configParam,
			@PathVariable boolean supportGroups) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting importRules", json);
		MappingRulesResponse mappingRules;
		try {
			mappingRules = RulesPayloadUtils.parsePayloadToMappingRules(json);
			if(!ruleEditorBusinessLogic.validateEditorVersion(mappingRules, supportGroups)) {
				return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", "The imported rules artifact version is not compatible with the current rule engine");
			}
		} catch (JsonParseException je) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Rule format is invalid: {}", je);
			return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.INVALID_RULE_FORMAT, "", je.getMessage());
		}
		return ruleEditorBusinessLogic.importRules(mappingRules, requestId, userId, vfcmtUuid, dcaeCompLabel, nid, configParam);
	}

	@PostMapping(value = "/importPhase", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity importPhase(@RequestBody String json, @ModelAttribute("requestId") String requestId, @RequestHeader("USER_ID") String userId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting importPhase", json);
		return ruleEditorBusinessLogic.importPhase(json, requestId, userId);
	}

    /**
     * This endpoint functions as a 'delete' service for the rule editor UI
     *
     * @param vfcmtUuid     - VFCMT that the rule editor ui is saved in
     * @param dcaeCompLabel - the name of the DCAE Component which the rule is applied to
     * @param nid           - A unique id of the DCAE Component which the rule is applied to - exists also in the cdump
     * @param configParam   - the name of the DCAE Component configuration property the rule is linked to
     * @param ruleUid       - the unique id of the rule to delete
     * @return operation result
     */
    @DeleteMapping(value = "/rule/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam}/{ruleUid}", produces = "application/json")
    public ResponseEntity deleteRule(
            @RequestHeader("USER_ID") String userId,
            @PathVariable("vfcmtUuid") String vfcmtUuid,
            @PathVariable("dcaeCompLabel") String dcaeCompLabel,
            @PathVariable("nid") String nid,
            @PathVariable("configParam") String configParam,
            @PathVariable("ruleUid") String ruleUid,
            @ModelAttribute("requestId") String requestId) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting deleteRule", vfcmtUuid);
        return ruleEditorBusinessLogic.deleteRule(userId, vfcmtUuid, dcaeCompLabel, nid, configParam, ruleUid, requestId);
    }


	/**
	 * This endpoint functions as a 'delete' service for the rule editor UI
	 *
	 * @param vfcmtUuid     - VFCMT that the rule editor ui is saved in
	 * @param dcaeCompLabel - the name of the DCAE Component which the group is applied to
	 * @param nid           - A unique id of the DCAE Component which the group is applied to - exists also in the cdump
	 * @param configParam   - the name of the DCAE Component configuration property the rules are linked to
	 * @param groupId       - the unique id of the group to delete
	 * @return operation result
	 */
	@DeleteMapping(value = "/group/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam}/{groupId}", produces = "application/json")
	public ResponseEntity deleteGroup(
			@RequestHeader("USER_ID") String userId,
			@PathVariable("vfcmtUuid") String vfcmtUuid,
			@PathVariable("dcaeCompLabel") String dcaeCompLabel,
			@PathVariable("nid") String nid,
			@PathVariable("configParam") String configParam,
			@PathVariable("groupId") String groupId,
			@ModelAttribute("requestId") String requestId) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting deleteRule", vfcmtUuid);
		return ruleEditorBusinessLogic.deleteGroupOfRules(userId, vfcmtUuid, dcaeCompLabel, nid, configParam, groupId, requestId);
	}

    /**
     * This endpoint functions as a 'translate' service for the rule editor UI
     *
     * @param body (vfcmtUuid)     - VFCMT that the rule editor ui is saved in
     * @param body (dcaeCompLabel) - the name of the DCAE Component which the rule is applied to
     * @param body (nid)           - A unique id of the DCAE Component which the rule is applied to - exists also in the cdump
     * @param body (configParam)   - the name of the DCAE Component configuration property the rule is linked to
     * @param body (entryPhase)    - the global entry phase name
	 * @param body (publishPhase)  - the global publish phase name
     * @return translateJson representing the translated Rules
     * Validations:
     * 1. That the user is able to edit the VFCMT
     * 2. That the cdump holds a dcae component with such nid (to avoid orphan rules)
     * 3. Check that the fetched VFCMT is actually a VFCMT and not a regular VF
     */

	@PostMapping(value = "/rule/translate", produces = "application/json")
	public ResponseEntity translateRules(@RequestBody String body, @ModelAttribute("requestId") String requestId)
	{
		TranslateRequest request = gson.fromJson(body, TranslateRequest.class);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting translateRules");
		return ruleEditorBusinessLogic.translateRules(request, requestId);
	}


    @GetMapping(value = "/getExistingRuleTargets/{vfcmtUuid}/{dcaeCompLabel}/{nid:.*}", produces = "application/json")
    public ResponseEntity getExistingRuleTargets(@PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId,
                                                 @PathVariable("dcaeCompLabel") String dcaeCompLabel,
                                                 @PathVariable("nid") String nid) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getExistingRuleTargets ", vfcmtUuid);
        return ruleEditorBusinessLogic.getExistingRuleTargets(vfcmtUuid, requestId, dcaeCompLabel, nid);
    }

	// 1810 US436244 MC table
	@GetMapping(value = "/getExistingRuleTargets/{vfcmtUuid}/{revertedUuid}/{dcaeCompLabel}/{nid:.*}", produces = "application/json")
	public ResponseEntity getExistingRuleTargets(@PathVariable String vfcmtUuid, @PathVariable String revertedUuid, @PathVariable String dcaeCompLabel, @PathVariable String nid, @ModelAttribute("requestId") String requestId) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getExistingRuleTargets ", vfcmtUuid);
		return ruleEditorBusinessLogic.getExistingRuleTargets(vfcmtUuid, requestId, dcaeCompLabel, nid);
	}
}

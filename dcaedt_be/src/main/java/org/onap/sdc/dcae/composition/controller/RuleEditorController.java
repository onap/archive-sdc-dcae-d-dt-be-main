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

    @RequestMapping(value = "/list-events-by-versions", method = RequestMethod.GET)
    public ResponseEntity getEventsByVersion() {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getEventsByVersion");
        return ruleEditorBusinessLogic.getEventsByVersion();
    }

    @RequestMapping(value = {"/definition/{version:.*}/{eventType}"}, method = {RequestMethod.GET}, produces = {"application/json"})
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
    @RequestMapping(value = "/rule/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", method = {RequestMethod.POST}, produces = "application/json")
    public ResponseEntity saveRule(@RequestBody String json, @ModelAttribute("requestId") String requestId,
                                   @RequestHeader("USER_ID") String userId,
                                   @PathVariable("vfcmtUuid") String vfcmtUuid,
                                   @PathVariable("dcaeCompLabel") String dcaeCompLabel,
                                   @PathVariable("nid") String nid,
                                   @PathVariable("configParam") String configParam) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting saveRule", json);
        return ruleEditorBusinessLogic.saveRule(json, requestId, userId, vfcmtUuid, dcaeCompLabel, nid, configParam);
    }


	@RequestMapping(value = "/applyFilter", method = {RequestMethod.POST}, produces = "application/json")
	public ResponseEntity applyFilter(@RequestBody String json, @ModelAttribute("requestId") String requestId, @RequestHeader("USER_ID") String userId) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting applyFilter", json);
		return ruleEditorBusinessLogic.applyFilter(json, requestId, userId);
	}

	@RequestMapping(value = "/deleteFilter", method = {RequestMethod.POST}, produces = "application/json")
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
    @RequestMapping(value = "/rule/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", method = {RequestMethod.GET}, produces = "application/json")
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
	@RequestMapping(value = "/rule/{vfcmtUuid}/{revertedUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", method = {RequestMethod.GET}, produces = "application/json")
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

	@RequestMapping(value = "/export/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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
	@RequestMapping(value = "/export/{vfcmtUuid}/{revertedUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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

	@RequestMapping(value = "/import/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam}/{supportGroups}", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
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

	@RequestMapping(value = "/importPhase", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/rule/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam}/{ruleUid}", method = {RequestMethod.DELETE}, produces = "application/json")
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
	@RequestMapping(value = "/group/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam}/{groupId}", method = {RequestMethod.DELETE}, produces = "application/json")
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

	@RequestMapping(value = "/rule/translate", method = {RequestMethod.POST}, produces = "application/json")
	public ResponseEntity translateRules(@RequestBody String body, @ModelAttribute("requestId") String requestId)
	{
		TranslateRequest request = gson.fromJson(body, TranslateRequest.class);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting translateRules");
		return ruleEditorBusinessLogic.translateRules(request, requestId);
	}


    @RequestMapping(value = "/getExistingRuleTargets/{vfcmtUuid}/{dcaeCompLabel}/{nid:.*}", method = {RequestMethod.GET}, produces = "application/json")
    public ResponseEntity getExistingRuleTargets(@PathVariable("vfcmtUuid") String vfcmtUuid, @ModelAttribute("requestId") String requestId,
                                                 @PathVariable("dcaeCompLabel") String dcaeCompLabel,
                                                 @PathVariable("nid") String nid) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getExistingRuleTargets ", vfcmtUuid);
        return ruleEditorBusinessLogic.getExistingRuleTargets(vfcmtUuid, requestId, dcaeCompLabel, nid);
    }

	// 1810 US436244 MC table
	@RequestMapping(value = "/getExistingRuleTargets/{vfcmtUuid}/{revertedUuid}/{dcaeCompLabel}/{nid:.*}", method = {RequestMethod.GET}, produces = "application/json")
	public ResponseEntity getExistingRuleTargets(@PathVariable String vfcmtUuid, @PathVariable String revertedUuid, @PathVariable String dcaeCompLabel, @PathVariable String nid, @ModelAttribute("requestId") String requestId) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting getExistingRuleTargets ", vfcmtUuid);
		return ruleEditorBusinessLogic.getExistingRuleTargets(vfcmtUuid, requestId, dcaeCompLabel, nid);
	}
}

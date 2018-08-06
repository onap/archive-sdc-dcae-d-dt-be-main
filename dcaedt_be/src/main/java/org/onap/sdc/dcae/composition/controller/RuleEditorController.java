package org.onap.sdc.dcae.composition.controller;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.impl.RuleEditorBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.TranslateRequest;
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

	@RequestMapping(value = "/import/{vfcmtUuid}/{dcaeCompLabel}/{nid}/{configParam:.*}", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity importRules(
			@RequestBody String json, @ModelAttribute("requestId") String requestId,
			@RequestHeader("USER_ID") String userId,
			@PathVariable("vfcmtUuid") String vfcmtUuid,
			@PathVariable("dcaeCompLabel") String dcaeCompLabel,
			@PathVariable("nid") String nid,
			@PathVariable("configParam") String configParam) {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting importRules", json);
		return ruleEditorBusinessLogic.importRules(json, requestId, userId, vfcmtUuid, dcaeCompLabel, nid, configParam);
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
}

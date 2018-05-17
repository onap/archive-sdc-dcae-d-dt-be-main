package org.onap.sdc.dcae.composition.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.composition.CompositionConfig;
import org.onap.sdc.dcae.errormng.ActionStatus;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.ves.VesStructureLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@EnableAutoConfiguration
@CrossOrigin
@RequestMapping("/conf")
public class ConfigurationController extends BaseController{

    @Autowired
    private CompositionConfig compositionConfig;

    @ApiOperation(value = "Get a list of available flow types", response = CompositionConfig.class)
    @ApiResponses(value = {
                @ApiResponse(code = 200, message = "Successfully retrieved available flow types list"),
                @ApiResponse(code = 500, message = "Flow types couldn't be fetched due to internal error")})
    @RequestMapping(value = "/composition", method = RequestMethod.GET)
    public ResponseEntity getCompositionConfig() {
        try {
            return new ResponseEntity<>(compositionConfig, HttpStatus.OK);
        }catch (Exception e) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Exception:{}",e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.FLOW_TYPES_CONFIGURATION_ERROR);
        }
    }

    @RequestMapping(value = "/ves/schemaversions", method = RequestMethod.GET)
    public ResponseEntity getCommonEventFormatVersion() {
        try {
            Set<String> availableVersionsSet = VesStructureLoader.getAvailableVersionsList();
            List<String> availableVersionsList = new ArrayList<>(availableVersionsSet.size());
            availableVersionsList.addAll(availableVersionsSet);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Got a request to return available ves schema versions: {}", availableVersionsSet);
            return new ResponseEntity<>(availableVersionsList, HttpStatus.OK);
        }catch (Exception e) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(),"Exception:{}",e);
            return ErrConfMgr.INSTANCE.buildErrorResponse(ActionStatus.VES_SCHEMA_NOT_FOUND);
        }
    }


}
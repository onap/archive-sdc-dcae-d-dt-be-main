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

package tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.JsonObject;
import json.Environment;

import json.templateInfo.DeployTemplateConfig;
import json.templateInfo.TemplateInfo;

import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import utilities.IDcaeRestClient;
import utilities.IReport;
import utilities.Report;
import utilities.DcaeRestClient;

import java.io.*;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {
    private static final String CONFIG_FILE = "DcaeDtDeployConfigFile";
    private static final String ENVIRONMENT_CONFIG = "environment.resource";

    private static LoggerError errLogger = LoggerError.getInstance();
    private static LoggerDebug debugLogger = LoggerDebug.getInstance();

    private Main() {
        throw new IllegalAccessError("Utility class");
    }

    public static void main(String[] args) {
        System.setProperty("logback.configurationFile", "conf/logback.xml");
        debugLogger.log("Starting VFCMT template deployment");
        if (args.length != 2) {
            errLogger.log("Got " + args.length + ", but expecting exactly 2 arguments ONLY!");
            System.exit(2);
        }
        debugLogger.log("Arguments:");
        Arrays.stream(args).forEach(arg -> debugLogger.log(arg));

        initConfiguration(args);
        IReport report = new Report();
        try {
            ObjectMapper mapper = new ObjectMapper();
            DeployTemplateConfig deployTemplateConfig = mapper.readValue(new File(System.getProperty(CONFIG_FILE, "conf/config.json")), DeployTemplateConfig.class);
            Environment environment = mapper.readValue(new File(System.getProperty(ENVIRONMENT_CONFIG, "conf/environment.json")), Environment.class);

            IDcaeRestClient dcaeRestClient = new DcaeRestClient(environment.getCredential());
            dcaeRestClient.init(environment);

            Map<String, List<Resource>> elementsByFolderNames = dcaeRestClient.getDcaeCatalog();

            TemplateContainer templateContainer = new TemplateContainer(report, dcaeRestClient, deployTemplateConfig.getTemplateInfo(), elementsByFolderNames);
            Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap = templateContainer.getCdumps();

            DeployTemplate deployTemplate = new DeployTemplate(report, dcaeRestClient);
            deployTemplate.deploy(templateInfoToJsonObjectMap);

            debugLogger.log( "VFCMT template deployment completed");

        } catch (RuntimeException e) {
            errLogger.log("ERROR - Template deployment failed with error " + e, e);
            report.setStatusCode(2);
        } catch (ConnectException e) {
            errLogger.log( "ERROR - Failed connection to server, are you on AT&T network? {}" + e, e);
			report.setStatusCode(2);
        } catch (IOException e) {
            errLogger.log( "ERROR - Fatal Error! " + e, e);
			report.setStatusCode(2);
        } finally {
            debugLogger.log(report.toString());
            report.reportAndExit();
        }
    }

    private static void initConfiguration(String[] args) {
        System.setProperty(ENVIRONMENT_CONFIG, args[0]);
        System.setProperty(CONFIG_FILE, args[1]);
    }


}

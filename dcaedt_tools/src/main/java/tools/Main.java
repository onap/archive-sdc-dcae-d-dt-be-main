package tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.JsonObject;
import json.Environment;
import json.response.ItemsResponse.Item;
import json.templateInfo.DeployTemplateConfig;
import json.templateInfo.TemplateInfo;

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
            return;
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

            EntitiesRetriever entitiesRetriever = new EntitiesRetriever(dcaeRestClient);
            Map<String, List<Item>> elementsByFolderNames = entitiesRetriever.getElementsByFolder();

            TemplateContainer templateContainer = new TemplateContainer(report, dcaeRestClient, deployTemplateConfig.getTemplateInfo(), elementsByFolderNames);
            Map<TemplateInfo, JsonObject> templateInfoToJsonObjectMap = templateContainer.getCdumps();

            DeployTemplate deployTemplate = new DeployTemplate(report, dcaeRestClient);
            deployTemplate.deploy(templateInfoToJsonObjectMap);

            debugLogger.log( "VFCMT template deployment completed successfully");
        } catch (RuntimeException e) {
            errLogger.log("ERROR - Template deployment failed with error " + e);
        } catch (ConnectException e) {
            errLogger.log( "ERROR - Failed connection to server, are you on AT&T network? {}" + e);
        } catch (IOException e) {
            errLogger.log( "ERROR - Fatal Error! " + e);
        } finally {
            debugLogger.log(report.toString());
        }
    }

    private static void initConfiguration(String[] args) {
        System.setProperty(ENVIRONMENT_CONFIG, args[0]);
        System.setProperty(CONFIG_FILE, args[1]);
    }


}

package org.onap.sdc.dcae.ves;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("vesstructureloader")
public class VesStructureLoader {



    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private static Map<String, EventListenerDefinition> eventListeners = new HashMap<>();
    private static final Type type = new TypeToken<VesDataItemsDefinition>(){}.getType();
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(type, new VesJsonDeserializer()).create();
    private static final String SCHEMA_NAME_PREFIX = "CommonEventFormat_v";
    private static final String SCHEMA_NAME_SUFFIX = ".json";

    private VesStructureLoader() {
    }

    @PostConstruct public void init() {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "VesStructureLoader: Trying to load json schemas");
        String jettyBase = System.getProperty("jetty.base");
        if (jettyBase == null) {
            String msg = "Couldn't resolve jetty.base environmental variable";
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
            throw new IllegalArgumentException(msg + ". Failed to load VES schema files... aborting");
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "jetty.base={}", jettyBase);

        File dir = new File(jettyBase + "/config/dcae-be/ves-schema");
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(SCHEMA_NAME_PREFIX) && name.endsWith(SCHEMA_NAME_SUFFIX));

        if (ArrayUtils.isEmpty(files)) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error – Failed to find VES Schema definitions.");
        } else {

            for (File f : files) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Loading VES schema file: {}", f.getName());
                parseJsonFileAndSaveToMap(f);
            }
        }

    }

    private void parseJsonFileAndSaveToMap(File file) {

        try {
            EventListenerDefinition eventListener = gson.fromJson(new FileReader(file), EventListenerDefinition.class);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), gson.toJson(eventListener));
            String validationError = getValidatorMessage(eventListener);
            if (StringUtils.isEmpty(validationError)) {
                eventListeners.put(getVersionFromFileName(file.getName()), eventListener);
            } else {
                errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Failed to parse VES schema file {}. [{}]", file.getName(), validationError);
            }
        } catch (FileNotFoundException | JsonIOException | JsonSyntaxException e) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error: Failed to parse VES schema file {}. [{}]", file.getName(), e);
        }
    }

    public static Map<String, VesDataTypeDefinition> getEventListenerDefinitionByVersion(String version) {
        return eventListeners.get(version).getProperties().get(EventListenerDefinition.EVENT_ROOT).getProperties();
    }

    public static Set<String> getAvailableVersionsList() {
        return eventListeners.keySet();
    }

    public static Map<String, Set<String>> getAvailableVersionsAndEventTypes() {
        return eventListeners.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> getEventListenerDefinitionByVersion(e.getKey()).keySet()));
    }

    public static Set<String> getEventTypeListByVersion(String version) {
        return getEventListenerDefinitionByVersion(version).keySet();
    }

    private String getValidatorMessage(EventListenerDefinition eventListenerDefinition) {
        String validationError = eventListenerDefinition.validate();
        if (StringUtils.isBlank(validationError)) {
            validationError = eventListenerDefinition.resolveRefTypes();
        }
        return validationError;
    }

    private String getVersionFromFileName(String fileName) {
        return fileName.replace(SCHEMA_NAME_PREFIX, "").replace(SCHEMA_NAME_SUFFIX, "");
    }

    @PreDestroy
    public void preDestroy() {
        // why is this method empty?
    }
}

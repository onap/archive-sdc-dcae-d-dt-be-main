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

package org.onap.sdc.dcae;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.onap.sdc.dcae.ves.EventListenerDefinition;
import org.onap.sdc.dcae.ves.VesDataItemsDefinition;
import org.onap.sdc.dcae.ves.VesDataTypeDefinition;
import org.onap.sdc.dcae.ves.VesJsonDeserializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class VesStructureLoaderMock {

	private Map<String, EventListenerDefinition> eventListeners = new HashMap<>();
	private Type type = new TypeToken<VesDataItemsDefinition>() {
	}.getType();
	private Gson gson = new GsonBuilder().registerTypeAdapter(type, new VesJsonDeserializer()).create();
	private final String schemaNamePrefix = "CommonEventFormat_v";
	private final String schemaNameSuffix = ".json";
	private List<String> initErrors;

	public VesStructureLoaderMock() {
		this(true);
	}

	public VesStructureLoaderMock(boolean validateAndResolve) {
		this(validateAndResolve, System.getProperty("user.dir") + "/src/test/resources/ves-schema");
	}

	public VesStructureLoaderMock(boolean validateAndResolve, String path) {
		initErrors = init(validateAndResolve, path);
	}

	public List<String> init(boolean validateAndResolve, String pathToSchemaDir) {

		List<String> parseErrors = new ArrayList<>();
		File dir = new File(pathToSchemaDir);
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				return name.startsWith(schemaNamePrefix) && name.endsWith(schemaNameSuffix);
			}
		});
		if (ArrayUtils.isEmpty(files)) {
			parseErrors.add("No VES schema files found");
		} else {
			for (File f : files) {
				String error = parseJsonFileAndSaveToMap(f, validateAndResolve);
				if (StringUtils.isNotBlank(error)) {
					parseErrors.add("Error: parsing VES schema file " + f.getName() + " failed due to " + error);
				}
			}
		}
		return parseErrors;

	}

	public Map<String, VesDataTypeDefinition> getEventListenerDefinitionByVersion(String version) {
		return eventListeners.get(version).getProperties().get(EventListenerDefinition.EVENT_ROOT).getProperties();
	}

	public Set<String> getAvailableVersionsList() {
		return eventListeners.keySet();
	}

	public Map<String, Set<String>> getAvailableVersionsAndEventTypes() {
		return eventListeners.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> getEventListenerDefinitionByVersion(e.getKey()).keySet()));
	}

	public Set<String> getEventTypeListByVersion(String version) {
		return getEventListenerDefinitionByVersion(version).keySet();
	}

	public String getVersionFromFileName(String fileName) {
		return fileName.replace(schemaNamePrefix, "").replace(schemaNameSuffix, "");
	}

	private String parseJsonFileAndSaveToMap(File file, boolean validateAndResolve) {
		String validationError = null;
		try {
			EventListenerDefinition eventListener = gson.fromJson(new FileReader(file), EventListenerDefinition.class);
			if (validateAndResolve)
				validationError = getValidatorMessage(eventListener);
			if (StringUtils.isEmpty(validationError))
				eventListeners.put(getVersionFromFileName(file.getName()), eventListener);
		} catch (FileNotFoundException | JsonIOException | JsonSyntaxException e) {
			validationError = e.getMessage();
		}
		return validationError;
	}

	public Map<String, EventListenerDefinition> getEventListeners() {
		return eventListeners;
	}

	public List<String> getInitErrors() {
		return initErrors;
	}

	private String getValidatorMessage(EventListenerDefinition eventListenerDefinition) {
		String validationError = eventListenerDefinition.validate();
		if (StringUtils.isBlank(validationError))
			validationError = eventListenerDefinition.resolveRefTypes();
		return validationError;
	}
}

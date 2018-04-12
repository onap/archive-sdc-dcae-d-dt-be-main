package org.onap.sdc.dcae.ves;

import org.junit.Test;
import org.onap.sdc.dcae.VesStructureLoaderMock;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VesStructureLoaderTest {

	// the file names of test schema files
	private final String UNRESOLVABLE_REFERENCES = "CommonEventFormat_vUnresolvable.json";
	private final String VALID_VERSION_4_1 = "CommonEventFormat_v4.1.json";
	private final String VALID_VERSION_5_3 = "CommonEventFormat_v5.3.json";
	private final String INVALID_JSON = "CommonEventFormat_vInvalidJson.json";
	private final String UNSUPPORTED_FILENAME = "unsupportedFilename.json";
	private final String INVALID_SCHEMA_STRUCTURE = "CommonEventFormat_vInvalidSchemaStructure.json";
	private final String INVALID_TYPE = "CommonEventFormat_vInvalidType.json";
	private final String INVALID_REQUIRED_ENTRY = "CommonEventFormat_vInvalidRequiredEntry.json";
	private final String NO_EVENT_PROPERTY = "CommonEventFormat_vNoEventProperty.json";
	private final String NO_COMMON_EVENT_HEADER = "CommonEventFormat_v4.1WithoutCommonEventHeader.json";

	// schema directory test paths
	private final String EMPTY_SCHEMA_DIR = System.getProperty("user.dir") + "/src/test/resources/ves-schema/empty";
	private final String NONE_EXISTING_DIR = EMPTY_SCHEMA_DIR + "/null";

	private final String ERROR_TEXT = "Error: parsing VES schema file ";

	// files loaded from default path, only valid files are kept, errors logged for invalid files (initError);
	@Test
	public void defaultInit() {
		VesStructureLoaderMock loader = new VesStructureLoaderMock();
		Set<String> expectedAvailableVersions = new HashSet<>();
		expectedAvailableVersions.add(loader.getVersionFromFileName(VALID_VERSION_4_1));
		expectedAvailableVersions.add(loader.getVersionFromFileName(VALID_VERSION_5_3));
		expectedAvailableVersions.add(loader.getVersionFromFileName(NO_COMMON_EVENT_HEADER));
		assertEquals(expectedAvailableVersions, loader.getAvailableVersionsList());
		List<String> expectedLoggedErrors = Arrays
				.asList(getExpectedInvalidJsonError(), getExpectedInvalidRequiredEntryError(), getExpectedInvalidStructureError(), getExpectedInvalidTypeError(), getExpectedNoEventDefinitionError(), getExpectedUnresolvableError());
		assertTrue(loader.getInitErrors().containsAll(expectedLoggedErrors));
		assertEquals(expectedLoggedErrors.size(), loader.getInitErrors().size());
	}

	@Test
	public void initWithEmptyDir() {
		VesStructureLoaderMock loader = new VesStructureLoaderMock(true, EMPTY_SCHEMA_DIR);
		assertTrue(loader.getAvailableVersionsList().isEmpty());
		assertEquals("No VES schema files found", loader.getInitErrors().get(0));
	}

	@Test
	public void initWithNoneExistingDir() {
		VesStructureLoaderMock loader = new VesStructureLoaderMock(true, NONE_EXISTING_DIR);
		assertTrue(loader.getAvailableVersionsList().isEmpty());
		assertEquals("No VES schema files found", loader.getInitErrors().get(0));
	}

	@Test
	public void complexDataTypeLoaderOutputTest() {
		VesStructureLoaderMock loader = new VesStructureLoaderMock();
		VesDataTypeDefinition loaded = loader.getEventListenerDefinitionByVersion("5.3").get("stateChangeFields");
		assertEquals(buildStateChangeFieldsDefinition(), loaded);
	}

	private String getExpectedInvalidJsonError() {
		return ERROR_TEXT + INVALID_JSON + " failed due to java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at path $";
	}

	private String getExpectedUnresolvableError() {
		return ERROR_TEXT + UNRESOLVABLE_REFERENCES + " failed due to the following definitions containing unresolvable references: [\"otherFields\",\"stateChangeFields\",\"syslogFields\",\"thresholdCrossingAlertFields\"]";
	}

	private String getExpectedInvalidStructureError() {
		return ERROR_TEXT + INVALID_SCHEMA_STRUCTURE + " failed due to java.lang.IllegalStateException: Expected BEGIN_ARRAY but was BEGIN_OBJECT at line 8 column 20 path $.definitions..properties[0]";
	}

	private String getExpectedInvalidTypeError() {
		return ERROR_TEXT + INVALID_TYPE + " failed due to invalid type declaration: invalid";
	}

	private String getExpectedInvalidRequiredEntryError() {
		return ERROR_TEXT + INVALID_REQUIRED_ENTRY + " failed due to invalid required entry: codecIdentifier(invalid)";
	}

	private String getExpectedNoEventDefinitionError() {
		return ERROR_TEXT + NO_EVENT_PROPERTY + " failed due to schema not containing property: event";
	}

	private VesDataTypeDefinition buildFieldDefinition() {
		Map<String, VesDataTypeDefinition> propsMap = new HashMap<>();
		VesDataTypeDefinition prop = buildVesDataType(null, VesSimpleTypesEnum.STRING.getType(), new ArrayList<>(), null, null);
		propsMap.put("name", prop);
		propsMap.put("value", prop);
		return buildVesDataType("name value pair", VesSimpleTypesEnum.OBJECT.getType(), Arrays.asList("name", "value"), propsMap, null);
	}

	private VesDataTypeDefinition buildStateChangeFieldsDefinition() {

		VesDataItemsDefinition items = new VesDataItemsDefinition();
		items.add(buildFieldDefinition());
		VesDataTypeDefinition prop = buildVesDataType("additional stateChange fields if needed", VesSimpleTypesEnum.ARRAY.getType(), new ArrayList<>(), null, null);
		prop.setItems(items);
		Map<String, VesDataTypeDefinition> propsMap = new HashMap<>();
		propsMap.put("additionalFields", prop);
		prop = buildVesDataType("new state of the entity", VesSimpleTypesEnum.STRING.getType(), new ArrayList<>(), null, Arrays.asList("inService", "maintenance", "outOfService"));
		propsMap.put("newState", prop);
		prop = buildVesDataType("previous state of the entity", VesSimpleTypesEnum.STRING.getType(), new ArrayList<>(), null, Arrays.asList("inService", "maintenance", "outOfService"));
		propsMap.put("oldState", prop);
		prop = buildVesDataType("version of the stateChangeFields block", VesSimpleTypesEnum.NUMBER.getType(), new ArrayList<>(), null, null);
		propsMap.put("stateChangeFieldsVersion", prop);
		prop = buildVesDataType("card or port name of the entity that changed state", VesSimpleTypesEnum.STRING.getType(), new ArrayList<>(), null, null);
		propsMap.put("stateInterface", prop);
		VesDataTypeDefinition def = buildVesDataType("stateChange fields", VesSimpleTypesEnum.OBJECT.getType(), Arrays.asList("newState", "oldState", "stateChangeFieldsVersion", "stateInterface"), propsMap, null);
		return def;
	}

	private VesDataTypeDefinition buildVesDataType(String description, String type, List<String> required, Map<String, VesDataTypeDefinition> properties, List<String> enums) {
		VesDataTypeDefinition def = new VesDataTypeDefinition();
		def.setDescription(description);
		def.setType(type);
		def.setRequired(required);
		def.setEnums(enums);
		def.setProperties(properties);
		return def;
	}

}
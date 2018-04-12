/*
 *                        AT&T - PROPRIETARY
 *          THIS FILE CONTAINS PROPRIETARY INFORMATION OF
 *        AT&T AND IS NOT TO BE DISCLOSED OR USED EXCEPT IN
 *             ACCORDANCE WITH APPLICABLE AGREEMENTS.
 *
 *          Copyright (c) 2014 AT&T Knowledge Ventures
 *              Unpublished and Not for Publication
 *                     All Rights Reserved
 */
package org.onap.sdc.dcae.db.neo4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Base64;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

/* A few less obvious design choices:
 *   - representing properties across type hierarchies (same for requirements
 * and capabilities, and will be for attributes and interfaces when we'll
 * add them): we attach to each type only those properties it declares (such a
 * declaration might be the re-definition of a property defined by a supertype).
 * Calculating the set of properties for a type (i.e. the one it declares plus
 * the ones it inherits, with respect to re-defintions) is a 2 step process:
 * 	 1. run a query matching all properties acrosss the type's hierarchy, from
 * leaf to root type (neo's job)
 * 	 2. collecting them in a set that accumulates them with respect to
 * re-definition (model catalog client library job)
 * A (viable) alternative would have been to calculate the entire property set
 * at model import time and associate them it the type node. It would simplify
 * the query and processing in the catalog API. It has the drawback of making
 * the reverse process (exporting a yaml model from neo) tedious.
 * As we get a better sense of were the optimizations are needed this might
 * be a change to be made ..
 *
 *
 *   - representing requirements and capability as nodes. At first glance
 * both can be represented as edges pointing from a Type Node or Template Node
 * to another Type Node or Template Node. While this is true for capabilities
 * it is not so for requirements: a requirement could point to a capability
 * of a Type Node, i.e. it is a hyperedge between a Type Node (or Tempate Node), * another Type Node (the target) and a capability of the target. As such, the
 * requirements ands up being represented as a node and the capability will need
 * to do the same in order to be able to be pointed at (and for the sake of
 * uniformity ..).
 *
 *
 */
public class Modeled {

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	private static HttpClientBuilder httpClientBuilder =
			HttpClientBuilder.create();
	private static String USAGE = "oil oil_stylesheet_path | bigdata | aws | awsdata input_file customer";

	private static List<String> ignoreMissing = new LinkedList<String>();

	static {
		Collections.addAll(ignoreMissing,
				"tosca.datatypes",
				"tosca.capabilities",
				"tosca.relationships",
				"tosca.interfaces",
				"tosca.nodes",
				"tosca.artifacts",
				"tosca.policies",
				"tosca.groups");
	}

	public static void main(String[] theArgs) {

		CommandLineParser parser = new BasicParser();

		// create the Options
		Options options = new Options();
		options.addOption(OptionBuilder.
				withArgName("target")
				.withLongOpt("target")
				.withDescription("target ice4j database uri")
				.hasArg()
				.isRequired()
				.create('t'));

		options.addOption(OptionBuilder.
				withArgName("action")
				.withLongOpt("action")
				.withDescription("one of import, annotate, list, remove")
				.hasArg()
				.isRequired()
				.create('a'));

		options.addOption(
				OptionBuilder.withArgName("input")
						.withLongOpt("input")
						.withDescription(
								"for import/annotate: the tosca template file, " +
										"for list: an optional json filter, " +
										"for remove: the template id")
						.hasArgs()
						.create('i')).addOption(
				OptionBuilder.withArgName("labels")
						.withLongOpt("labels")
						.withDescription(
								"for annotate: the ':' sepatated list of annotation labels")
						.hasArgs()
						.create('l'));

		options.addOption(OptionBuilder.
				withArgName("ignore")
				.withLongOpt("ignore")
				.isRequired(false)
				.withDescription(
						"for annotate: the ':' sepatated list of namespaces who's missing constructs can be ignored")
				.hasArgs()
				.create());


		CommandLine line;
		try {
			line = parser.parse(options, theArgs);
		} catch (ParseException exp) {
			errLogger.log(LogLevel.ERROR, Modeled.class.getName(), exp.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("import", options);
			return;
		}

		String ignores = line.getOptionValue("ignore");
		if (ignores != null)
			Collections.addAll(ignoreMissing, ignores.split(":"));

		Modeled modeled = new Modeled();
		try {
			modeled.setNeoUri(new URI(line.getOptionValue("target")));
		} catch (URISyntaxException urisx) {
			errLogger.log(LogLevel.ERROR, Modeled.class.getName(), "Invalid target specification: {}", urisx);
			return;
		}

		try {
			loadStorageSpec();

			String action = line.getOptionValue("action");
			if ("import".equals(action)) {
				modeled.importTemplate(line.getOptionValue("input"));
			} else if ("annotate".equals(action)) {
				modeled.annotateItem(line.getOptionValue("input"), line.getOptionValue("labels"));
			} else if ("list".equals(action)) {
				modeled.listTemplates(line.getOptionValue("input"));
			} else if ("remove".equals(action)) {
				modeled.removeTemplate(line.getOptionValue("input"));
			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("import", options);
			}
		} catch (Exception x) {
			errLogger.log(LogLevel.ERROR, Modeled.class.getName(), x.getMessage());
		}
	}

	private static Tracker<String> tracker = new Tracker<String>();
	private static Map toscaStorageSpec;

	private static void loadStorageSpec() {
		toscaStorageSpec = (Map) new Yaml().load(
				Modeled.class.getClassLoader().getResourceAsStream("tosca-schema.yaml"));

		Map storageSpec = (Map) new Yaml().load(
				Modeled.class.getClassLoader().getResourceAsStream("tosca-storage-schema.yaml"));

		JXPathContext jxPath = JXPathContext.newContext(toscaStorageSpec);
		for (Iterator<Map.Entry<String, Object>> ces =
			 storageSpec.entrySet().iterator();
			 ces.hasNext(); ) {
			Map.Entry<String, Object> ce = ces.next();
			try {
				Map m = (Map) jxPath.getValue(ce.getKey());
				if (m == null) {
					debugLogger.log(LogLevel.DEBUG, Modeled.class.getName(), "No schema entry '{}'", ce.getKey());
					continue;
				}

				m.putAll((Map) ce.getValue());
			} catch (JXPathException jxpx) {
				errLogger.log(LogLevel.WARN, Modeled.class.getName(), "Failed to apply storage info {}", jxpx);
			}
		}
	}


	private static JSONObject EMPTY_JSON_OBJECT = new JSONObject();

	private URI neoUri = null;

	private Modeled() {
	}

	private void setNeoUri(URI theUri) {
		this.neoUri = theUri;
	}

	public URI getNeoUri() {
		return this.neoUri;
	}

	/* Experimental in nature. I was reluctant creating another node to represent
	 * the set of constraints as they're integral part of the property (or other
	 * artifact) they're related to. I was also looking for a representation
	 * that would easily be processable into a TOSCA abstraction in the
	 * Catalog API. So ... we pack all the constraints as a JSON string and store
	 * them as a single property of the TOSCA artifact they belog to.
	 * Highs: easily un-winds in an object
	 * Lows: can't write query selectors based on constraints values ..
				//the TOSCA/yaml spec exposes constraints as a List .. where each
				//entry is a Map .. why??
	 */
	private static String yamlEncodeConstraints(List theConstraints) {
		Map allConstraints = new HashMap();
		for (Object c : theConstraints) {
			allConstraints.putAll((Map) c);
			//this would be the place to add dedicate processing of those
			//constraints with 'special' values, i.e. in_range: dual scalar,
			//valid_values: list
		}
		return JSONObject.valueToString(allConstraints);
	}

	/* TODO: attributes handling to be added, similar to properties.
	 */
	private void yamlNodeProperties(String theNodeId,
									Map<String, Object> theProperties,
									NeoTransaction theTrx)
			throws IOException {

		for (Map.Entry<String, Object> propertyEntry : theProperties.entrySet()) {
			String propName = propertyEntry.getKey();
			Object propObject = propertyEntry.getValue();

			Map propValues;
			if (propObject instanceof Map) {
				propValues = (Map) propObject;
			} else {
				//valuation, not of interest here
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "neoNode, unknown property representation {} for {}, node {}", propObject.getClass(), propObject, theNodeId);
				continue;
			}

			String constraintsValue = null;
			if (propValues.containsKey("constraints")) {
				constraintsValue = yamlEncodeConstraints(
						(List) propValues.get("constraints"));
			}

			String neoPropId = neoCreateNode(
					theTrx, false,
					new JSONObject()
							.put("name", propName)
							.put("type", propValues.getOrDefault("type", "string"))
							.put("required", propValues.getOrDefault("required", Boolean.TRUE))
							.putOpt("default", propValues.get("default"))
							.putOpt("description", propValues.get("description"))
							.putOpt("status", propValues.get("status"))
							.putOpt("constraints", constraintsValue),
					"TOSCA", "Property");

			neoEdge(theTrx, false,
					neoPropId,
					theNodeId,
					EMPTY_JSON_OBJECT,
					"PROPERTY_OF");
		}

	}

	private void yamlNodeTypeCapabilities(String theNodeId,
										  Map<String, Object> theCapabilities,
										  NeoTransaction theTrx)
			throws IOException {

		for (Map.Entry<String, Object> capability : theCapabilities.entrySet()) {
			String capabilityName = capability.getKey();
			Object capabilityValue = capability.getValue();

			String capabilityType = null,
					capabilityDesc = null;
			Map<String, Object> capabilitySpec = null;

			if (capabilityValue instanceof String) {
				//short notation was used, we get the name of a capability type
				capabilityType = (String) capabilityValue;

				capabilitySpec = Collections.singletonMap("type", capabilityType);
			} else if (capabilityValue instanceof Map) {
				//extended notation
				capabilitySpec = (Map<String, Object>) capabilityValue;

				capabilityType = (String) capabilitySpec.get("type");
				//cannot be missing
				if (capabilityType == null) {
					//ERROR!!
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "neoNode, missing capability type in {} for node {}", capabilitySpec, theNodeId);
					continue; //rollback ..
				}
				capabilityDesc = (String) capabilitySpec.get("description");
			}

			//
			String anonCapabilityTypeId = null;
			if (capabilitySpec.containsKey("properties")) {
				//we need an anonymous capability type (augmentation)
				//or they could be added to the 'Capabillity' node but anonymous
				//types make processing more uniform
				anonCapabilityTypeId =
						yamlAnonymousType(capabilitySpec,
								capabilityType,
//not a very nice owner string as theNodeId is cryptic (we should use
//node name but do not have it here ..
								theNodeId + "#" + capabilityName,
								true,
								false,
								theTrx);
			}

			JSONObject capabilityDef = new JSONObject()
					.put("name", capabilityName)
					.putOpt("description", capabilityDesc);
			if (capabilitySpec != null) {
				List occurrences = (List) capabilitySpec.get("occurrences");
				if (occurrences != null) {
					capabilityDef.put("occurrences", encodeRange(occurrences));
				}
				List valid_source_types = (List) capabilitySpec.get("valid_source_types");
				if (valid_source_types != null) {
					capabilityDef.put("validSourceTypes",
							new JSONArray(valid_source_types));
				}
			}

			String capabilityId = neoCreateNode(
					theTrx, false,
					capabilityDef,
					"TOSCA", "Capability");
			neoEdge(theTrx, false,
					capabilityId,
					theNodeId,
					EMPTY_JSON_OBJECT,
					"CAPABILITY_OF");

			if (anonCapabilityTypeId != null) {
				neoEdge(theTrx, false,
						capabilityId,
						anonCapabilityTypeId,
						new JSONObject()
								.put("name", capabilityName)
								.putOpt("description", capabilityDesc),
						"FEATURES"/* TARGETS */);
				//no reason this one would point to a non-existing capability as we just created one
			} else {
				if (null == neoEdge(theTrx, false,
						capabilityId,
						"Type",
						new JSONObject()
								.put("name", capabilityType),
						new JSONObject()
								.put("name", capabilityName)
								.putOpt("description", capabilityDesc),
						"FEATURES"/* TARGETS */)) {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlNodeTypeCapabilities, Node {}, capability {} (id: {}) seems to point to invalid capability type: {}", theNodeId, capabilityName, capabilityId, capabilityType);
					ignoreMissing(capabilityType);
				}
			}

		}

	}

	private void yamlNodeTypeRequirements(
			String theNodeTypeId,
			List<Map<String, Object>> theRequirements,
			NeoTransaction theTrx)
			throws IOException {

		for (Map<String, Object> arequirement : theRequirements) {
			//supposed to have only one entry
			Map.Entry<String, Object> requirement =
					arequirement.entrySet().iterator().next();

			String requirementName = requirement.getKey();
			Object requirementValue = requirement.getValue();

			String targetNode = null,
					targetCapability = null,
					targetRelationship = null;
			Map<String, Object> requirementSpec = null;

			if (requirementValue instanceof String) {
				//short form, points to a capability type
				targetCapability = (String) requirementValue;
			} else if (requirementValue instanceof Map) {
				//extended notation
				requirementSpec = (Map<String, Object>) requirementValue;

				targetCapability = (String) requirementSpec.get("capability");
				targetNode = (String) requirementSpec.get("node");
				//this assumes a short form for the relationship specification
				//it can actually be a map (indicating the relationship type and the
				//additional interface definitions).
				targetRelationship = (String) requirementSpec.get("relationship");
			}

			if (targetCapability == null) {
				throw new IOException(theNodeTypeId + "missing capability type");
			}

			JSONObject requirementDef = new JSONObject()
					.put("name", requirementName);
			if (requirementSpec != null) {
				List occurrences = (List) requirementSpec.get("occurrences");
				if (occurrences != null) {
					requirementDef.put("occurrences", encodeRange(occurrences));
				}
			}

			String requirementId = neoCreateNode(
					requirementDef,
					"TOSCA", "Requirement");
			neoEdge(theTrx, false,
					requirementId,
					theNodeTypeId,
					EMPTY_JSON_OBJECT,
					"REQUIREMENT_OF");

			//we're not verifying here that this a capability type .. just a type
			if (null == neoEdge(theTrx, false,
					requirementId,
					"Type",
					new JSONObject()
							.put("name", targetCapability),
					EMPTY_JSON_OBJECT,
					"CAPABILITY")) {
				errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlNodeTypeRequirements, Node {}, requirement {} (id: {}) seems to point to invalid capability type: {}", theNodeTypeId, requirementName, requirementId, targetCapability);
			}

			if (targetNode != null) {
				//points to a node type
				if (null == neoEdge(theTrx, false,
						requirementId,
						"Type",
						new JSONObject()
								.put("name", targetNode),
						EMPTY_JSON_OBJECT,
						"REQUIRES")) {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlNodeTypeRequirements, Node {}, requirement {} (id: {}) seems to point to invalid capability type: {}", theNodeTypeId, requirementName, requirementId, targetCapability);
				}
			}

			if (targetRelationship != null) {
				//points to a relationship type
				if (null == neoEdge(theTrx, false,
						requirementId,
						"Type",
						new JSONObject()
								.put("name", targetRelationship),
						EMPTY_JSON_OBJECT,
						"RELATIONSHIP")) {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlNodeTypeRequirements, Node {}, requirement {} (id: {}) seems to point to invalid relationship type: {}", theNodeTypeId, requirementName, requirementId, targetRelationship);
				}
			}
		}
	}

	/*
	 * handles the requirement assignments
	 */
	private void toscaRequirementsAssignment(
			String theNodeId,
			List<Map<String, Object>> theRequirements,
			NeoTransaction theTrx)
			throws IOException {

		for (Map<String, Object> arequirement : theRequirements) {
			//supposed to have only one entry
			Map.Entry<String, Object> requirement =
					arequirement.entrySet().iterator().next();

			String requirementName = requirement.getKey();
			Object requirementValue = requirement.getValue();

			String targetNode = null,
					targetCapability = null,
					targetRelationship = null;
			//TODO: targetFilter

			Map<String, Object> requirementSpec = null;

			if (requirementValue instanceof String) {
				//short notation was used, we get the name of a local node
				targetNode = (String) requirementValue;
			} else if (requirementValue instanceof Map) {
				//extended notation
				requirementSpec = (Map<String, Object>) requirementValue;

				targetNode = (String) requirementSpec.get("node");
				targetCapability = (String) requirementSpec.get("capability");
				targetRelationship = (String) requirementSpec.get("relationship");
			}

			/* TODO: add targetFilter definition in here (most likely place)
			 */
			String requirementId = neoCreateNode(
					theTrx, false,
					new JSONObject()
							.put("name", requirementName),
					"TOSCA", "Requirement");

			neoEdge(theTrx, false,
					requirementId,
					theNodeId,
					EMPTY_JSON_OBJECT,
					"REQUIREMENT_OF");

			String targetNodeTemplate = null;
			if (targetNode != null) {
				//check if the target is a node within the template (in which case the
				//requirement is really defined by that node type. i.e. its type's
				//capabilities
				targetNodeTemplate = tracker.lookupTemplate("Node", targetNode);
				if (targetNodeTemplate != null) {
					neoEdge(theTrx, false,
							requirementId,
							targetNodeTemplate,
							new JSONObject()
									.put("name", requirementName),
							"REQUIRES" /* TARGETS */);
				} else {
					//if not a local node template then it must be node type
					if (null == neoEdge(theTrx, false,
							requirementId,
							"Type",
							new JSONObject()
									.put("name", targetNode),
							EMPTY_JSON_OBJECT,
							"REQUIRES")) {
						errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlNodeTypeRequirements, Node {}, requirement {} (id: {}) seems to point to invalid node type: {}", theNodeId, requirementName, requirementId, targetNode);
					}
				}
			}

			if (targetCapability != null) {
				/*
				 * Can point to a capability of the targetNode (template or type,
				 * whatever was specified) or to a capability type;
				 */
				if (targetNode != null) {
					String stmt = null;
					if (targetNodeTemplate != null) {
						//a capability of a local node template
						//TODO: could be a capability type of a local node (and is up to the
						//orchestrator to pick) given that the target node has at least one 						//capability of that type
						stmt =
								"MATCH (c:Capability)-[:CAPABILITY_OF]->(n:Node), (r:Requirement) " +
										"WHERE id(n)=" + targetNodeTemplate + " " +
										"AND c.name = \"" + targetCapability + "\" " +
										"AND id(r)=" + requirementId + "  " +
										"MERGE (r)-[rq:REQUIRES_CAPABILITY]->(c) " +
										"RETURN id(rq)";
					} else {
						//a capability of the node type
						stmt =
								"MATCH (c:Type:Capability)-[:CAPABILITY_OF]->(t:Type), (r:Requirement) " +
										"WHERE t.name = \"" + targetNode + "\" " +
										"AND c.name = \"" + targetCapability + "\" " +
										"AND id(r)=" + requirementId + "  " +
										"MERGE (r)-[rq:REQUIRES_CAPABILITY]->(c) " +
										"RETURN id(rq)";
					}
					if (null == neoId(theTrx
							.statement(
									new JSONObject()
											.put("statement", stmt))
							.execute()
							.result())) {
						errLogger.log(LogLevel.WARN, this.getClass().getName(), "toscaRequirementsAssignment, Node {}, requirement {} (id: {}) seems to point to invalid node capability: {}", theNodeId, requirementName, requirementId, targetCapability);
					}
				} else {
					if (null == neoEdge(theTrx, false,
							requirementId,
							"Type",
							new JSONObject()
									.put("name", targetCapability),
							EMPTY_JSON_OBJECT,
							"REQUIRES_CAPABILITY")) {
						errLogger.log(LogLevel.WARN, this.getClass().getName(), "toscaRequirementsAssignment, Node {}, requirement {} (id: {}) seems to point to invalid capability type: {}", theNodeId, requirementName, requirementId, targetCapability);
					}
				}
			}

			if (targetRelationship != null) {
				if (null == neoEdge(theTrx, false,
						requirementId,
						"Type",
						new JSONObject()
								.put("name", targetRelationship),
						EMPTY_JSON_OBJECT,
						"RELATIONSHIP")) {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "toscaRequirementsAssignment, Node {}, requirement {} (id: {}) seems to point to invalid relationship type: {}", theNodeId, requirementName, requirementId, targetRelationship);
				}
			} else {
				//TODO: does the presence of properties/attributes/interfaces in the
				//requirement definition trigger the defintion of an anonymous
				//relationship type?? (maybe derived from the one under the
				//'relationship_type' key, if present?)
			}
		}
	}

	/* an anonymous type is created from a node specification (type,template)
	 */
	private String yamlAnonymousType(Map<String, Object> theInfo,
									 String theType,
									 String theOwner,
									 boolean doProperties,
									 boolean doCapabilities,
									 NeoTransaction theTrx)
			throws IOException {

		//is this naming scheme capable enough??NO!
		String anonTypeId = theOwner + "#" + (theType == null ? "" : theType);

		String neoAnonTypeId = neoMergeNode(
				theTrx, false,
				new JSONObject()
						.put("name", anonTypeId)
						.put("id", anonTypeId),
				"TOSCA", "Type");

		if (theType != null) {
			neoEdge(theTrx, false,
					neoAnonTypeId,
					"Type",
					new JSONObject()
							.put("name", theType),
					EMPTY_JSON_OBJECT,
					"DERIVED_FROM");
		}

		//shoudl the properties spec be passed explcitly??
		if (doProperties) {
			Map<String, Object> props = (Map<String, Object>) theInfo.get("properties");
			if (props != null) {
				yamlNodeProperties(neoAnonTypeId, props, theTrx);
			}
		}

		return neoAnonTypeId;
	}

	/*
	 * A first pass over a type spec provisions each type individually
	 * and its properties.
	 * We process here types for all constructs: data, capability, relationship,
	 * node, [interface, artifact]
	 */
	private void toscaTypeSpec(String theConstruct,
							   Map<String, Map> theTypes,
							   NeoTransaction theTrx)
			throws IOException {
		//first pass, provision each type individually (and their properties)
		String rule = "_" + theConstruct.toLowerCase() + "_type_definition";
		Map storageSpec = (Map) toscaStorageSpec.get(rule);

		for (Map.Entry<String, Map> toscaType : theTypes.entrySet()) {
			String typeName = toscaType.getKey();
			Map<String, Map> typeValue = (Map<String, Map>) toscaType.getValue();

			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Type: {}", typeName);

			JSONObject data = pack(storageSpec, typeValue)
					.put("name", typeName)
					.put("id", typeName);

			String neoTypeId = neoMergeNode(theTrx, false, data, "TOSCA", "Type", theConstruct);

			tracker.trackType(theConstruct, typeName, neoTypeId);

			Map<String, Object> toscaTypeProps = (Map<String, Object>) typeValue.get("properties");
			if (toscaTypeProps != null) {
				yamlNodeProperties(neoTypeId, toscaTypeProps, theTrx);
			} //type props
		} //types

		toscaTypePostProc(theConstruct, theTypes, theTrx);
	}

	/*
	 * A second pass to process the derived_from relationship and
	 * the capabilities (now that the capabilities types have been provisioned)
	 */
	private void toscaTypePostProc(String theConstruct,
								   Map<String, Map> theTypes,
								   NeoTransaction theTrx)
			throws IOException {
		for (Map.Entry<String, Map> typeEntry : theTypes.entrySet()) {
			Map typeValue = typeEntry.getValue();
			String typeName = typeEntry.getKey();

			//supertype and description: all types
			String superTypeName = (String) typeValue.get("derived_from");
			if (superTypeName != null) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{}-DERIVED_FROM->{}", typeName, superTypeName);

				if (tracker.tracksType(theConstruct, superTypeName)) {
					if (null == neoEdge(theTrx, false,
							tracker.lookupType(theConstruct, typeName),
							tracker.lookupType(theConstruct, superTypeName),
							EMPTY_JSON_OBJECT,
							"DERIVED_FROM")) {
						errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlTypePostProc, missing parent type {}, id {} for type {}, id {}", superTypeName, tracker.lookupType(theConstruct, superTypeName), typeName, tracker.lookupType(theConstruct, typeName));
					}
				} else {
					if (null == neoEdge(theTrx, false,
							tracker.lookupType(theConstruct, typeName),
							"Type",
							new JSONObject()
									.put("name", superTypeName),
							new JSONObject(),
							"DERIVED_FROM")) {
						errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlTypePostProc, missing parent type {} for type {}", superTypeName, typeName);
					}
				}
			}

			//requirements/capabilities: for node types
			Map<String, Object> capabilities =
					(Map<String, Object>) typeValue.get("capabilities");
			if (capabilities != null) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Processing: {}", capabilities);
				yamlNodeTypeCapabilities(
						tracker.lookupType(theConstruct, typeName), capabilities, theTrx);
			}

			List<Map<String, Object>> requirements =
					(List<Map<String, Object>>) typeValue.get("requirements");
			if (requirements != null) {
				yamlNodeTypeRequirements(
						tracker.lookupType(theConstruct, typeName), requirements, theTrx);
			}

			//interfaces: for node types or relationship types
			Object interfaces = typeValue.get("interfaces");
			if (interfaces != null) {
				errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlTypePostProc, Type {}: interfaces section declared but not handled", typeName);
				if (interfaces instanceof List) {
					//expect a list of interface types
				}
			}

			//valid targets: for relationship types
			List valid_targets = (List) typeValue.get("valid_targets");
			if (valid_targets != null) {
				//add as a property to the type node, can be used for validation
				//whereever this type is used
				//the list should contain node type names and we should check that we
				//have those types
				errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlTypePostProc, Type {}: valid_targets section declared but not handled", typeName);

			}

			List artifacts = (List) typeValue.get("artifacts");
			if (artifacts != null) {
				errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlTypePostProc, Type {}: artifacts section declared but not handled", typeName);
			}

			/* Artifact types can have "mime_type" and "file_ext" sections
			 */
		}
	}

	private void toscaTemplate(String theTopologyTemplateId,
							   String theConstruct,
							   Map<String, Object> theTemplates,
							   NeoTransaction theTrx)
			throws IOException {

		String rule = "_" + theConstruct.toLowerCase() + "_template_definition";
		Map storageSpec = (Map) toscaStorageSpec.get(rule);
		if (storageSpec == null) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "No rule '{}', can't make up the storage specification for {}", rule, theConstruct);
		}

		for (Map.Entry<String, Object> template : theTemplates.entrySet()) {

			String templateName = template.getKey();
			Map<String, Object> templateSpec = (Map<String, Object>) template.getValue();

			String templateType = (String) templateSpec.get("type");
			if (templateType == null) {
				errLogger.log(LogLevel.WARN, this.getClass().getName(), "neoNode, template {}'{}', does not have a type specification .. skipping", theConstruct, templateName);
				continue;
			}

			try {
				//we use create here as node names are not unique across templates
				JSONObject neoTemplateNode =
						pack(storageSpec, templateSpec)
								.put("name", templateName);

				String templateNodeId = neoCreateNode(
						theTrx, false, neoTemplateNode, "TOSCA", theConstruct);

				tracker.trackTemplate(theConstruct, templateName, templateNodeId);

				neoEdge(theTrx, false,
						templateNodeId,
						theTopologyTemplateId,
						new JSONObject(),
						theConstruct.toUpperCase() + "_OF");

				if (null == neoEdge(theTrx, false,
						templateNodeId,
						"Type",
						new JSONObject()
								.put("name", templateType),
						new JSONObject(),
						"OF_TYPE")) {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlSpec, Template {}, {} {}: failed to identify type {}", theTopologyTemplateId, theConstruct, templateName, templateType);
				}

				//facets

				//we handle properties for all constructs (as they all have them)
				Map<String, Object> templateProps =
						(Map<String, Object>) templateSpec.get("properties");
				if (templateProps != null) {
					for (Map.Entry<String, Object> templateProp :
							templateProps.entrySet()) {
						String templatePropName = templateProp.getKey();
						Object templatePropObject = templateProp.getValue();

						final Map templatePropValues;
						if (templatePropObject instanceof Map) {
							templatePropValues = (Map) templatePropObject;
						} else {

							//this is dealing with short form, if we ran the first 2 stages of the checker  //we'd always be working on a canonical form ..
							//
							templatePropValues = new HashMap();
							templatePropValues.put("value", templatePropObject);
						}

						//a node will contain the means for property valuation:
						//straight value or a call to get_input/get_property/get_attribute

						//find the property node (in the type) this valuation belongs to
						if (templatePropValues != null) {

							String propertyId =
									neoId(
											theTrx.statement(
													new JSONObject()
															.put("statement",
																	"MATCH (t:Type)-[:DERIVED_FROM*0..5]->(:Type)<-[:PROPERTY_OF]-(p:Property) " +
																			"WHERE t.name='" + templateType + "' " +
																			"AND p.name='" + templatePropName + "' " +
																			"RETURN id(p)"))
													.execute()
													.result()
									);

							if (propertyId == null) {
								errLogger.log(LogLevel.WARN, this.getClass().getName(), "yamlSpec, Template {},  {} template {}, property {} does not match the node type spec, skipping property", templateName, theConstruct, templateName, templatePropName);
								continue;
							}

							//remove valuation by function: for now handle only get_input
							String propInput = (String) templatePropValues.remove("get_input");

							List constraints = (List) templatePropValues.remove("constraints");
							if (constraints != null) {
								//flattening
								templatePropValues.put("constraints",
										yamlEncodeConstraints(constraints));
							}

							Object val = templatePropValues.remove("value");
							//check if the value is a collection or user defined data type, the cheap way
							if (val instanceof List ||
									val instanceof Map) {
								/* An interesting option here:
								 * 	1. store the whole flatten value under the 'value' property
													templatePropValues.put("value",	JsonFlattener.flatten(JsonObject.valueToString(val)));
											 Simpler but almost impossible to write queries based on property value
								 *	2. store each entry in the flatten map as a separate property (we prefix it with 'value' for
								 *		 clarity).
								 *       see below
								 */
					/*
								JsonFlattener.flattenAsMap(JSONObject.valueToString(Collections.singletonMap("value",val)))
									.entrySet()
										.stream()
											.forEach(e -> templatePropValues.put(e.getKey(), e.getValue()));
					*/
								//simply stores a collection in its (json) string representation. Cannot be used if
								//queries are necessary based on the value (on one of its elements).
								templatePropValues.put("value", JSONObject.valueToString(val));
							} else {
								/* scalar, store as such */
								templatePropValues.put("value", val);
							}

							String templatePropValueId =
									neoCreateNode(
											theTrx, false,
											new JSONObject(templatePropValues),
											"TOSCA", /*"Property",*/ "Assignment");

							neoEdge(theTrx, false,
									templatePropValueId,
									templateNodeId,
									new JSONObject(),
									"OF_TEMPLATE");

							neoEdge(theTrx, false,
									templatePropValueId,
									propertyId,
									new JSONObject(),
									"OF_" + theConstruct.toUpperCase() + "_PROPERTY");

							if (propInput != null) {
								String inputId = tracker.lookupTemplate("Input", propInput);
								if (inputId == null) {
									errLogger.log(LogLevel.WARN, this.getClass().getName(), "neoNode, Template {},node {}, property {} input {} not found", theTopologyTemplateId, templateName, templatePropName, propInput);
								}

								neoEdge(theTrx, false,
										templatePropValueId,
										inputId,
										new JSONObject(),
										"GET_INPUT");
							}
						}
					}
				}
				tracker.trackTemplate(theConstruct, templateName, templateNodeId);
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{} template {} of type {}", theConstruct, templateName, templateType);
			} catch (IOException iox) {
				errLogger.log(LogLevel.WARN, this.getClass().getName(), "toscaTemplate, Failed to persist template {}", iox);
				throw iox;
			}
		}
	}

	/* while we persist basic type values inline (in the assigment node) we store complex values
	 * in a graph of their own.
	 * We handle the neo4j 'limitation' stated below
	 * 	Neo4j can only store collections (map, list) of basic types.
	 *
	 * User defined data types can created undefinitely nested strctures of collections.
	 * We could store collections of basic types inline but it would make for a less uniform structure.
	 */
	private void toscaPropertyAssignment(
			String theAssignmentId,
			Object theValue,
			NeoTransaction theTrx)
			throws IOException {
		//look the grammar rules to see if we inline (stringify) or not

		if (theValue instanceof Map) {
			//a map type property or a user-defined datatype
			Map<String, Object> elements = (Map<String, Object>) theValue;
			for (Map.Entry element : elements.entrySet()) {

				String elementId = neoCreateNode(theTrx, false,
						new JSONObject().
								put("name", element.getKey()),
						"TOSCA", "Data", "Element");

				neoEdge(theTrx, false,
						elementId,
						theAssignmentId,
						EMPTY_JSON_OBJECT,
						"ELEMENT_OF");

				toscaPropertyAssignment(elementId, element.getValue(), theTrx);
			}
		} else if (theValue instanceof List) {
			//a list type property
			for (int i = 0; i < ((List) theValue).size(); i++) {

				String elementId = neoCreateNode(theTrx, false,
						new JSONObject().
								put("pos", i),
						"TOSCA", "Data", "Element");

				neoEdge(theTrx, false,
						elementId,
						theAssignmentId,
						EMPTY_JSON_OBJECT,
						"ELEMENT_OF");

				toscaPropertyAssignment(elementId, ((List) theValue).get(i), theTrx);
			}

			//update theAssignment with a length property
			neoNodeProperties(theTrx, false, theAssignmentId,
					new JSONObject().
							put("length", ((List) theValue).size()));
		} else {
			//update the assignment with a 'value' attribute
			neoNodeProperties(theTrx, false, theAssignmentId,
					new JSONObject().
							put("value", theValue));
		}
	}

	/*
	 * We only handle properties for now so we assume these are properties
	 * assignments
	 */
	private void toscaCapabilityAssignment(
			String theNodeTemplateId,
			String theCapabilityName,
			Map<String, Object> theValuations,
			NeoTransaction theTrx)
			throws IOException {

		for (Map.Entry<String, Object> valuation : theValuations.entrySet()) {
			String propertyName = valuation.getKey();
			Object propertyValueSpec = valuation.getValue();

			Map propertyValue = null;
			if (propertyValueSpec instanceof Map) {
				propertyValue = (Map) propertyValueSpec;
			} else {
				//this is dealing with short form, if we ran the first 2 stages of
				//the checker we'd always be working on a canonical form ..
				propertyValue = new HashMap();
				propertyValue.put("value", propertyValueSpec);
			}

			//we need to link the assignment to the node template, the capability
			//and the property of the capability type (a node can have multiple
			//capabilities of the same type).
			String[] ids =
					neoIds(
							theTrx.statement(
									new JSONObject()
											.put("statement",
													"MATCH (n:Node)-[:OF_TYPE]->(:Node:Type)<-[:CAPABILITY_OF]-(c:Capability)-[:FEATURES]->(:Capability:Type)-[:DERIVED_FROM*0..5]->(:Capability:Type)<-[:PROPERTY_OF]-(p:Property) " +
															"WHERE id(n) = " + theNodeTemplateId + " " +
															"AND c.name = '" + theCapabilityName + "' " +
															"AND p.name = '" + propertyName + "' " +
															"RETURN id(p), id(c)"))
									.execute()
									.result());

			if (ids == null) {
				throw new IOException("toscaCapabilityAssignment: " +
						"node template " + theNodeTemplateId + ",  " +
						"capability " + theCapabilityName + ", " +
						"property " + propertyName +
						" does not match the node type spec");
			}

			/* this node represents the assignment of a value to a capability property
			 * hence my doubts about hoe to label it ['Assignment', 'Property'] or ['Assignment','Capability']
			 * I am inclined towards the second option as there is no other capability assignment in itself.
			 */
			String assignmentId =
					neoCreateNode(
							theTrx, false,
							new JSONObject(propertyValue),
							"TOSCA", /*Capability,*/"Assignment");

			neoEdge(theTrx, false,
					assignmentId,
					theNodeTemplateId,
					new JSONObject(),
					"OF_TEMPLATE");

			neoEdge(theTrx, false,
					assignmentId,
					ids[1],
					new JSONObject(),
					"OF_CAPABILITY");

			neoEdge(theTrx, false,
					assignmentId,
					ids[0],
					new JSONObject(),
					"OF_CAPABILITY_PROPERTY");
		}
	}

	/*
	 *
	 * */
	private void importTemplate(String thePath) throws IOException {
		try (FileInputStream input = new FileInputStream(thePath)){
			for (Object yaml : new Yaml().loadAll(input)) {
				toscaSpec((Map) yaml);
			}
		}
	}

	private void toscaSpec(Map theSpec) throws IOException {

		// type specifications
		// at this time we do not record the relation between a type and the
		// template it was defined in.

		NeoTransaction trx = new NeoTransaction(this.neoUri);
		try {
			{
				Map<String, Map> types = (Map<String, Map>) theSpec.get("data_types");
				if (types != null) {
					toscaTypeSpec("Data", types, trx);
				}

				types = (Map<String, Map>) theSpec.get("capability_types");
				if (types != null) {
					toscaTypeSpec("Capability", types, trx);
				}

				types = (Map<String, Map>) theSpec.get("relationship_types");
				if (types != null) {
					toscaTypeSpec("Relationship", types, trx);
				}

				types = (Map<String, Map>) theSpec.get("node_types");
				if (types != null) {
					toscaTypeSpec("Node", types, trx);
				}

				types = (Map<String, Map>) theSpec.get("policy_types");
				if (types != null) {
					toscaTypeSpec("Policy", types, trx);
				}
			}

			Map<String, Map> topologyTemplate = (Map<String, Map>)
					theSpec.get("topology_template");
			if (topologyTemplate != null) {

				Map<String, Object> metadata = (Map<String, Object>) theSpec.get("metadata");
				if (metadata == null) {
					throw new IOException("Missing metadata, cannot register template");
				}
				String templateName = (String) metadata.get("template_name");
				String templateId = neoMergeNode(
						trx, false,
						new JSONObject()
								.put("name", templateName)
								.putOpt("description", (String) theSpec.get("description"))
								.putOpt("version", (String) metadata.get("template_version"))
								.putOpt("author", (String) metadata.get("template_author"))
								.putOpt("scope", (String) metadata.get("scope")),
						"TOSCA", "Template");

				/* inputs */
				Map<String, Map> toscaInputs = (Map) topologyTemplate.get("inputs");
				if (toscaInputs != null) {
					for (Map.Entry<String, Map> toscaInput : toscaInputs.entrySet()) {
						//we use create here as input names are not unique across templates
						//also, constraints require special encoding
						Map toscaInputSpec = toscaInput.getValue();

						List constraints = (List) toscaInputSpec.remove("constraints");
						if (constraints != null) {
							//flattening
							toscaInputSpec.put("constraints",
									yamlEncodeConstraints(constraints));
						}
						String neoInputNodeId =
								neoCreateNode(
										trx, false,
										new JSONObject(toscaInputSpec)
												.put("name", toscaInput.getKey())
												.putOpt("type", toscaInputSpec.get("type")),
										"TOSCA", "Input");

						tracker.trackTemplate(
								"Input", (String) toscaInput.getKey(), neoInputNodeId);

						neoEdge(trx, false,
								neoInputNodeId,
								templateId,
								new JSONObject(),
								"INPUT_OF");
					}
				}

				/*
				 * The main issue that I have here is with the defintion given to each
				 * section (properties, capabilities, requirements ..) of a Node template:
				 * they are said to 'augment' the information provided in its Node Type but
				 * without specifying the semantics of 'augment'. Can new properties be
				 * added? can interface specification contain new operations?
				 */
				Map<String, Object> toscaNodes = (Map) topologyTemplate.get("node_templates");
				if (toscaNodes != null) {
					toscaTemplate(templateId, "Node", toscaNodes, trx);

					//now that all nodes are in we need a second path over the nodes set in
					//order to handle the capabilities, requirements ..

					for (Map.Entry<String, Object> toscaNode : toscaNodes.entrySet()) {

						String toscaNodeName = toscaNode.getKey();
						Map<String, Object> toscaNodeValues = (Map<String, Object>) toscaNode.getValue();

						Map<String, Map> capabilities =
								(Map<String, Map>) toscaNodeValues.get("capabilities");
						if (capabilities != null) {
							for (Map.Entry<String, Map> capability : capabilities.entrySet()) {
								Map<String, Map> assignments = (Map<String, Map>) capability.getValue();
								Map<String, Object> propertiesAssignments =
										assignments.get("properties");
								if (propertiesAssignments != null) {
									toscaCapabilityAssignment(
											tracker.lookupTemplate("Node", toscaNodeName),
											capability.getKey(),
											propertiesAssignments,
											trx);
								}
							}
						}

						List<Map<String, Object>> requirements = (List<Map<String, Object>>)
								toscaNodeValues.get("requirements");
						if (requirements != null) {
							toscaRequirementsAssignment(
									tracker.lookupTemplate("Node", toscaNodeName), requirements, trx);
						}

						//interfaces
					}
				}

				List toscaPolicies = (List) topologyTemplate.get("policies");
				if (toscaPolicies != null) {
					for (Object toscaPolicy : toscaPolicies) {
						toscaTemplate(templateId, "Policy", (Map<String, Object>) toscaPolicy, trx);
					}
				}

				Map<String, Map> toscaOutputs = (Map) topologyTemplate.get("outputs");
				if (toscaOutputs != null) {
					for (Map.Entry<String, Map> toscaOutput : toscaOutputs.entrySet()) {
						Object outputValue = toscaOutput.getValue().get("value");
						if (outputValue instanceof Map) { //shouldn't I be doing this in all cases??
							outputValue = JSONObject.valueToString((Map) outputValue);
						}

						String neoOutputNodeId = neoCreateNode(
								trx, false,
								new JSONObject()
										.put("name", (String) toscaOutput.getKey())
										.putOpt("description", (String) toscaOutput.getValue().get("description"))
										.put("value", outputValue.toString()),
								"TOSCA", "Output");

						neoEdge(trx, false,
								neoOutputNodeId,
								templateId,
								new JSONObject(),
								"OUTPUT_OF");
					}
				}

				//if this is a service template look for its type mapping specification
				Map<String, Object> substitutionSpec =
						(Map<String, Object>) theSpec.get("substitution_mappings");
				if (substitutionSpec != null) {

					String nodeType = (String) substitutionSpec.get("node_type");
					if (nodeType != null) {
						neoEdge(trx, false,
								templateId,
								"Type",
								new JSONObject()
										.put("name", nodeType),
								new JSONObject(),
								"SUBSTITUTES");
					} else {
						errLogger.log(LogLevel.WARN, this.getClass().getName(), "neoProc, Template {} substitution_mapping is missing a node_type in spec: {}", templateName, substitutionSpec);
					}

					//process the rest of the mapping definition
				} else {
					errLogger.log(LogLevel.WARN, this.getClass().getName(), "neoProc, Template {} does not have a substitution mapping", templateName);
				}

				//try to connect template to catalog item if information was provided
				//
				String catalogItemSelector = (String) metadata.get("asc_catalog");
				if (catalogItemSelector != null) {
					if (null == neoEdge(trx, false,
							templateId,
							"CatalogItem",
							new JSONObject(catalogItemSelector),
							new JSONObject(),
							"MODEL_OF")) {
						throw new IOException("No such catalog item: " + catalogItemSelector);
					}
				}
			}
			trx.commit();
		} catch (IOException iox) {
			try {
				trx.rollback();
			} catch (IOException riox) {
				errLogger.log(LogLevel.ERROR, Modeled.class.getName(), riox.getMessage());
			}
			throw iox;
		}
	}

	private void annotateItem(String thePath, String theLabels) throws IOException {

		if (theLabels == null) {
			throw new IOException("Labels ??");
		}

		try (FileInputStream input = new FileInputStream(thePath)){
			for (Object yaml : new Yaml().loadAll(input)) {
				annotateItem((Map) yaml, theLabels);
			}
		}
	}

	private void annotateItem(Map theSpec, String theLabels) throws IOException {

		Map<String, Object> metadata = (Map<String, Object>) theSpec.get("metadata");
		if (metadata == null) {
			throw new IOException("Missing metadata, cannot register template");
		}

		String catalogItemSelector = (String) metadata.remove("asc_catalog");
		if (catalogItemSelector == null) {
			throw new IOException("Missing item selector");
		}

		JSONObject annotation = new JSONObject();
		for (Map.Entry<String, Object> e : metadata.entrySet()) {
			String key = e.getKey();
			if (key.startsWith("asc_")) {
				annotation.put(key.substring(4, key.length()), e.getValue());
			}
		}

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "annotation: " + annotation);

		NeoTransaction trx = new NeoTransaction(this.neoUri);
		try {
			String id = neoCreateNode(trx, false, annotation, ("Annotation:" + theLabels).split(":"));
			if (id == null) {
				throw new IOException("No such catalog item: " + catalogItemSelector);
			}

			id = neoEdge(trx, false,
					id,
					"CatalogItem",
					new JSONObject(catalogItemSelector),
					new JSONObject(),
					"ANNOTATION_OF");
			if (id == null) {
				throw new IOException("No such catalog item: " + catalogItemSelector);
			}

			trx.commit();
		} catch (IOException iox) {
			try {
				trx.rollback();
			} catch (IOException riox) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), riox.getMessage());
			}
			throw iox;
		}
	}

	private void listTemplates(String theSelector) throws IOException {

		JSONObject selector = null;

		if (theSelector != null) {
			selector = new JSONObject(theSelector);
		}

		NeoTransaction trx = new NeoTransaction(this.neoUri);

		JSONObject res = trx.statement(new JSONObject()
				.put("statement",
						"MATCH (t:TOSCA:Template" +
								(selector != null ? neoLiteralMap(selector) : "") + ") RETURN t, id(t)")
				.put("parameters",
						new JSONObject()
								.put("props", selector != null ? selector : new JSONObject())))
				.commit()
				.result();

		JSONArray data = res
				.getJSONArray("results")
				.getJSONObject(0)
				.getJSONArray("data");
		if (data.length() == 0) {
			return;
		}

		for (int i = 0; i < data.length(); i++) {
			JSONArray row = data.getJSONObject(i)
					.getJSONArray("row");
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{}: {}", row.getInt(1), row.getJSONObject(0));
		}
	}


	private void removeTemplate(String theId) throws IOException {

		//find the nodes to delete and then use 'detach delete'

		NeoTransaction trx = new NeoTransaction(this.neoUri);

		try {
			//Template elements are never more then three hops away and point towards the template
			JSONObject res = trx.statement(new JSONObject()
					.put("statement",
							"MATCH (t:TOSCA:Template)<-[*0..3]-(x) " +
									"WHERE id(t)=" + theId + " RETURN {labels:labels(x),id:id(x)} as tgt"))
					.execute()
					.result();

			JSONArray data = res
					.getJSONArray("results")
					.getJSONObject(0)
					.getJSONArray("data");
			if (data.length() == 0) {
				return;
			}

			for (int i = data.length() - 1; i >= 0; i--) {
				JSONArray row = data.getJSONObject(i)
						.getJSONArray("row");
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "> {}", row.getJSONObject(0));

				//double check


				res = trx.statement(new JSONObject()
						.put("statement",
								"MATCH (n) " +
										"WHERE id(n)=" + row.getJSONObject(0).getInt("id") + " " +
										"DETACH DELETE n"))
						.execute()
						.result();

				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "> {}", res);
			}

			trx.commit();
		} catch (IOException iox) {
			try {
				trx.rollback();
			} catch (IOException riox) {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Rollback failed: {}", riox);
			}
			throw iox;
		}
	}

	/*
	 */
	private static void ignoreMissing(String theTarget) throws IOException {

		for (String prefix : ignoreMissing) {
			//make sure they are only one name element away
			if ((theTarget.startsWith(prefix)) && (theTarget.substring(prefix.length()).lastIndexOf('.') == 0)) {
				return;
			}
		}

		throw new IOException("Not configured to ignore missing " + theTarget);
	}

	private static JSONArray encodeRange(List theRange) throws IOException {
		JSONArray range = new JSONArray();
		for (Object value : theRange) {
			if (value instanceof Number) {
				range.put(((Number) value).intValue());
			} else if (value instanceof String &&
					"UNBOUNDED".equals(value)) {
				range.put(Integer.MAX_VALUE);
			} else {
				throw new IOException("Unexpected value in range definition: " + value);
			}
		}
		return range;
	}

	private static String neoLiteralMap(JSONObject theProps) {
		return neoLiteralMap(theProps, "props");
	}

	private static String neoLiteralMap(JSONObject theProps, String theArg) {
		if (theProps.length() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder("");
		for (Iterator i = theProps.keys(); i.hasNext(); ) {
			String key = (String) i.next();
			sb.append("`")
					.append(key)
					.append("`: {")
					.append(theArg)
					.append("}.`")
					.append(key)
					.append("`,");
		}
		return "{ " + sb.substring(0, sb.length() - 1) + " }";
	}

	private static String neoLabelsString(int theStartPos, String... theLabels) {
		StringBuffer lbls = new StringBuffer("");
		for (int i = theStartPos; i < theLabels.length; i++) {
			lbls.append(":")
					.append(theLabels[i]);
		}
		return lbls.toString();
	}

	private String neoCreateNode(
			JSONObject theProperties,
			String... theLabels) throws IOException {
		return neoNode("CREATE", theProperties, theLabels);
	}

	/* executes the (up to 2) statements required to construct a node
		 in a dedicated transaction */
	private String neoNode(
			String theVerb,
			JSONObject theProperties,
			String... theLabels) throws IOException {
		NeoTransaction trx = new NeoTransaction(this.neoUri);
		try {
			return neoNode(trx, true,
					theVerb, theProperties, theLabels);
		} catch (IOException iox) {
			try {
				trx.rollback();
			} catch (IOException ioxx) {
				errLogger.log(LogLevel.ERROR, Modeled.class.getName(), ioxx.getMessage());
			}
			throw iox;
		}
	}

	private String neoCreateNode(
			NeoTransaction theTransaction,
			boolean doCommit,
			JSONObject theProperties,
			String... theLabels) throws IOException {
		return neoNode(theTransaction, doCommit, "CREATE", theProperties, theLabels);
	}

	private String neoMergeNode(
			NeoTransaction theTransaction,
			boolean doCommit,
			JSONObject theProperties,
			String... theLabels) throws IOException {
		return neoNode(theTransaction, doCommit, "MERGE", theProperties, theLabels);
	}

	/* execute the statements required to construct a node as part of the
	given transaction

	 */
	private String neoNode(
			NeoTransaction theTransaction,
			boolean doCommit,
			String theVerb,
			JSONObject theProperties,
			String... theLabels) throws IOException {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "neoNode {}", new Object[]{theProperties, theLabels});

		JSONObject node;
		String nodeId;

		node = theTransaction
				.statement(
						new JSONObject()
								.put("statement",
										theVerb + " (n:" + theLabels[0] + neoLiteralMap(theProperties) + " ) RETURN id(n)")
								.put("parameters",
										new JSONObject()
												.put("props", theProperties)))
				.execute()
				.result();


		nodeId = neoId(node);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "neoNode, node: {}", nodeId);

		if (theLabels.length > 1) {
			theTransaction.statement(
					new JSONObject()
							.put("statement",
									"START n=node(" + nodeId + ") SET n " + neoLabelsString(1, theLabels)));
		}
		theTransaction.execute(doCommit);

		return nodeId;
	}

	private void neoNodeProperties(
			NeoTransaction theTransaction,
			boolean doCommit,
			String theId,
			JSONObject theProperties) throws IOException {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "neoNodeProperties {}", new Object[]{theId, theProperties});
		theTransaction
				.statement(
						new JSONObject()
								.put("statement",
										"START n=node(" + theId + ") SET n+= " +
												neoLiteralMap(theProperties) + " RETURN id(n)")
								.put("parameters",
										new JSONObject()
												.put("props", theProperties)))
				.execute(doCommit);
	}

	private String neoEdge(
			NeoTransaction theTransaction,
			boolean doCommit,
			String theFrom, String theTo,
			JSONObject theProperties,
			String... theLabels) throws IOException {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "neoEdge: {}", new Object[]{theFrom, theTo, theProperties, theLabels});

		return neoEdge(
				theTransaction, doCommit,
				new JSONObject()
						.put("statement",
								"START a=node(" + theFrom + "),b=node(" + theTo + ") " +
										"MERGE (a)-[r:" + theLabels[0] + neoLiteralMap(theProperties) + "]->(b) " +
										"RETURN id(r)")
						.put("parameters",
								new JSONObject()
										.put("props", theProperties)));
	}

	private String neoEdge(
			NeoTransaction theTransaction, boolean doCommit,
			String theFromId,
			String theToLabel, JSONObject theToProps,
			JSONObject theProperties,
			String... theLabels) throws IOException {

		return neoEdge(theTransaction, doCommit,
				new JSONObject()
						.put("statement",
								//"START a=node(" + theFromId + ") " +
								"MATCH (a),(b:" + theToLabel + neoLiteralMap(theToProps, "toProps") + ") " +
										"WHERE id(a)=" + theFromId + " " +
										"MERGE (a)-[r:" + theLabels[0] + neoLiteralMap(theProperties) + "]->(b) " +
										"RETURN id(r)")
						.put("parameters",
								new JSONObject()
										.put("toProps", theToProps)
										.put("props", theProperties)));
	}

	private String neoEdge(NeoTransaction theTransaction,
						   boolean doCommit,
						   JSONObject theEdgeStatement)
			throws IOException {

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "neoEdge {}", new Object[]{theEdgeStatement});

		return neoId(
				theTransaction
						.statement(theEdgeStatement)
						.execute(doCommit)
						.result()
		);
	}

	private static String neoId(JSONObject theResult) throws IOException {
		try {
			JSONArray data = theResult
					.getJSONArray("results")
					.getJSONObject(0)
					.getJSONArray("data");
			if (data.length() == 0) {
				return null;
			}

			return String.valueOf(
					data.getJSONObject(0)
							.getJSONArray("row")
							.getInt(0));
		} catch (JSONException jsonx) {
			errLogger.log(LogLevel.WARN, Modeled.class.getName(), "neoId, No 'id' in result: {} {}", theResult, jsonx);
			throw new IOException("no 'id' in result", jsonx);
		}
	}

	private static String[] neoIds(JSONObject theResult) throws IOException {
		try {
			JSONArray data = theResult
					.getJSONArray("results")
					.getJSONObject(0)
					.getJSONArray("data");
			if (data.length() == 0) {
				return new String[]{};
			}

			JSONArray array = data.getJSONObject(0)
					.getJSONArray("row");

			String[] res = new String[array.length()];
			for (int i = 0; i < array.length(); i++) {
				res[i] = String.valueOf(array.getInt(i));
			}
			return res;
		} catch (JSONException jsonx) {
			errLogger.log(LogLevel.WARN, Modeled.class.getName(), "neoId, No 'id' in result: {} {}", theResult, jsonx);
			throw new IOException("no 'id' in result", jsonx);
		}
	}

	private static class NeoTransaction {

		private HttpClient client = null;
		private String uri = null;
		private String auth = null;
		private JSONObject result = null;
		private JSONArray stmts = new JSONArray();

		NeoTransaction(URI theTarget) {

			client = httpClientBuilder.build();
			this.uri = theTarget.getScheme() + "://" + theTarget.getHost() + ":" + theTarget.getPort() + "/db/data/transaction";

			String userInfo = theTarget.getUserInfo();
			if (userInfo != null) {
				this.auth = "Basic " + new String(
						Base64.encodeBase64(
								userInfo.getBytes(Charset.forName("ISO-8859-1"))));
			}
		}

		/* adds a statement to the next execution cycle */
		NeoTransaction statement(JSONObject theStatement) {
			if (this.client == null) {
				throw new IllegalStateException("Transaction was completed");
			}
			this.stmts.put(theStatement);
			return this;
		}

		/* executes all pending statements but does not commit the transaction */
		/* executing a transaction with no statements refreshes the transaction timer in order to keep the transaction alive */
		NeoTransaction execute() throws IOException {
			if (this.client == null) {
				throw new IllegalStateException("Transaction was completed");
			}
			post(this.uri);
			return this;
		}

		/* executes all pending statements and commits the transaction */
		NeoTransaction commit() throws IOException {
			if (this.client == null) {
				throw new IllegalStateException("Transaction was completed");
			}
			post(this.uri + "/commit");
			//mark the transaction as terminated
			this.client = null;
			return this;
		}

		/* just to simplify some code written on top of NeoTransaction */
		NeoTransaction execute(boolean doCommit) throws IOException {
			return doCommit ? commit() : execute();
		}

		private void post(String theUri) throws IOException {
			HttpPost post = new HttpPost(theUri);
			JSONObject payload = new JSONObject()
					.put("statements", this.stmts);
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "post> " + payload);
			post.setEntity(new StringEntity(payload.toString(),
					ContentType.APPLICATION_JSON));
			run(post);
		}

		/* rollbacks the transaction changes */
		NeoTransaction rollback() throws IOException {
			if (this.client == null) {
				throw new IllegalStateException("Transaction was completed");
			}
			if (this.uri == null) {
				throw new IllegalStateException("Transaction not started");
			}
			run(new HttpDelete(this.uri));
			return this;
		}

		/* retrieve the (raw) results of the last execute/commit cycle */
		JSONObject result() {
			return this.result;
		}

		private void run(HttpUriRequest theRequest) throws IOException {
			theRequest.setHeader(HttpHeaders.ACCEPT, "application/json; charset=UTF-8");
			if (this.auth != null) {
				theRequest.setHeader(HttpHeaders.AUTHORIZATION, this.auth);
			}

			HttpResponse response = this.client.execute(theRequest);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 300) {
				try {
					this.result = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
				} catch (Exception x) {
					errLogger.log(LogLevel.ERROR, Modeled.class.getName(), x.getMessage());
				}
				throw new IOException("Neo statement(s) '" + this.stmts + "' failed: " + response.getStatusLine());
			}

			try {
				this.result = new JSONObject(
						IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			} catch (Exception x) {
				throw new IOException("no json in response", x);
			}

			JSONArray errors = this.result.getJSONArray("errors");
			if (errors.length() > 0) {
				throw new IOException("Neo statement(s) '" + this.stmts + "' have errors: " + errors);
			}
			//we only get a header if this was not a one statement transaction
			Header hdr = response.getFirstHeader("Location");
			if (hdr != null) {
				if (!hdr.getValue().startsWith(this.uri)) {
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "new transaction location?? : {} vs. {}", this.uri, hdr.getValue());
				}
				this.uri = hdr.getValue();
			}
			this.stmts = new JSONArray();
		}
	}

	private static JSONObject pack(Map theRule, Map theDef) {
		JSONObject pack = new JSONObject();

		if (theRule == null) {
			return pack;
		}

		//these are the facets of the construct definition
		Map facets = (Map) theRule.get("mapping");
		if (facets == null) {
			return pack;
		}

		facets.entrySet().stream()
				.forEach(
						theEntry ->
						{
							Map.Entry entry = (Map.Entry) theEntry;
							Map facetDef = (Map) entry.getValue();

							String storage = (String) facetDef.getOrDefault("storage", "");
							String type = (String) facetDef.get("type");

							if ("none".equals(storage)) {
								return;
							}
							if ("map".equals(type)) {
								//maps are used for cross-references between constructs or for
								//constructs facets
								return;
							}
							Object val = theDef.get(entry.getKey());
							if ("seq".equals(type)) {
								//sequences can be stored inlined, if so instructed ..
								if ("inline".equals(storage)) {
									val = JSONObject.valueToString(val);
								} else {
									return;
								}
							}
							if ("no".equals(facetDef.getOrDefault("required", "no"))) {
								pack.putOpt((String) entry.getKey(), theDef.get(entry.getKey()));
							} else {
								pack.putOnce((String) entry.getKey(), theDef.get(entry.getKey()));
							}
						});
		return pack;
	}

	/* a sort of catalog of neo identifiers generated for the different
	 * constructs (or their types) we store
	 */
	private static class Tracker<T> {

		private Table<String, String, T>
				typeTracker = HashBasedTable.create(),
				templateTracker = HashBasedTable.create();

		void trackType(String theConstruct, String theName, T theInfo) {
			typeTracker.put(theConstruct, theName, theInfo);
		}

		T lookupType(String theConstruct, String theName) {
			return typeTracker.get(theConstruct, theName);
		}

		boolean tracksType(String theConstruct, String theName) {
			return typeTracker.contains(theConstruct, theName);
		}

		void trackTemplate(String theConstruct, String theName, T theInfo) {
			templateTracker.put(theConstruct, theName, theInfo);
		}

		T lookupTemplate(String theConstruct, String theName) {
			return templateTracker.get(theConstruct, theName);
		}

	}
}

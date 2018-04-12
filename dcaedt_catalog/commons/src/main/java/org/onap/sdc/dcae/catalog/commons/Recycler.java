package org.onap.sdc.dcae.catalog.commons;

import java.io.Reader;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.JXPathContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.yaml.snakeyaml.Yaml;


/**
 * Practically a copy of the Validator's service Recycler, minus the Spring framework aspects + picking up the
 * description of every node
 */
public class Recycler {

    private static final String PROPERTIES = "properties";
    private static final String VALUE = "value";
    private static final String ASSIGNMENT = "assignment";
    private static final String CAPABILITY = "capability";
    private static final String RELATIONSHIP = "relationship";
	private static final String NAME = "name";
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
    private List<Map> 	 imports;
    private List<String> metas;

    public Recycler() {
        withImports();
        withMetas(null);
    }

    public Recycler withImports(String... theImports) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Setting imports to {}", theImports);
        ListBuilder importsBuilder = new ListBuilder();
        for (int i = 0; i < theImports.length; i++) {
            importsBuilder.add(new MapBuilder()
                    .put("i" + i, theImports[i])
                    .build());
        }
        this.imports = importsBuilder.build();
        return this;
    }

    private List imports() {
        ListBuilder importsBuilder = new ListBuilder();
        for (Map e: this.imports) {
            importsBuilder.add(new MapBuilder()
                    .putAll(e)
                    .build());
        }
        return importsBuilder.build();
    }

    public Recycler withMetas(String... theMetas) {
        this.metas = (theMetas == null) ? Collections.emptyList() : Arrays.asList(theMetas);
        return this;
    }

    public Object recycle(final Reader theSource) throws Exception {
        return this.recycle(new ObjectMapper().readValue(theSource, (Class)HashMap.class));
    }
    
    public Object recycle(final Object theDump) {
  
        final JXPathContext jxroot = JXPathContext.newContext(theDump);
    	jxroot.setLenient(true);

        final Map<String, Object> nodeTemplates =
            (Map<String, Object>)new MapBuilder()
                .putAll(
                        StreamSupport
                            .stream(
                                Spliterators.spliteratorUnknownSize((Iterator<Pointer>)jxroot.iteratePointers("/nodes"), 16), false)
                            .map(p -> {
                                        JXPathContext jxnode = jxroot.getRelativeContext(p);
                                        return new AbstractMap.SimpleEntry<String,Object>(
                                            (String)jxnode.getValue(NAME) + "_" + (String)jxnode.getValue("nid"),
                                            new MapBuilder()
                                                    .put("type", jxnode.getValue("type/name"))
                                                    .put("description", jxnode.getValue("description"))
                                                    .putOpt("metadata", nodeMetadata(jxnode))
                                                    .putOpt(PROPERTIES, nodeProperties(jxnode))
                                                    .putOpt("requirements", nodeRequirements(jxnode))
                                                    .putOpt("capabilities", nodeCapabilities(jxnode))
                                                    .build());
                            })::iterator)
                .buildOpt();

            return new MapBuilder()
                                    .put("tosca_definitions_version", "tosca_simple_yaml_1_0_0")
                                    .put("imports", imports())
                                    .put("topology_template", new MapBuilder()
                                                                                            .putOpt("node_templates", nodeTemplates)
                                                                                            .build())
                                    .build();
    }

    /* */
    private Object nodeProperties(JXPathContext theNodeContext) {
        return
            new MapBuilder()
                .putAll(
                    StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize((Iterator<Map>)theNodeContext.iterate(PROPERTIES), 16), false)
                                                    .map(m -> new AbstractMap.SimpleEntry(m.get(NAME), this.nodeProperty(m)))
                                                    .filter(e -> e.getValue() != null)
                            ::iterator)
                .buildOpt();
    }
    
    /* */
    private Object nodeProperty(final Map theSpec) {
        Object value = theSpec.get(VALUE);
        if (value == null) {
            value = theSpec.get("default");
            if (value == null) {
                /*final*/ Map assign = (Map)theSpec.get(ASSIGNMENT);
                if (assign != null) {
                    value = assign.get(VALUE);
                }
            }
        }
        String type = (String)theSpec.get("type");
        if (value != null && type != null) {
			value = getValueByType(value, type);
		}
        return value;
    }

	private Object getValueByType(Object value, String type) {
    	Object returnValue = null;
		try {
            if ("map".equals(type) && !(value instanceof Map)) {
				returnValue = new ObjectMapper().readValue(value.toString(), new TypeReference<Map>(){});
            }
            else if ("list".equals(type) && !(value instanceof List)) {
				returnValue = new ObjectMapper().readValue(value.toString(), new TypeReference<List>(){});
            }
            else if ("integer".equals(type) && (value instanceof String)) {
				returnValue = Integer.valueOf((String)value);
            }
            else if ("float".equals(type) && (value instanceof String)) {
				returnValue = Double.valueOf((String)value); //double because that's how the yaml parser would encode it
            }
        }
        catch (NumberFormatException nfx) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Failed to process String representation {} of numeric data: {}", value, nfx);
        }
        catch (IOException iox) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Failed to process {} representation of a collection: {}", value.getClass().getName(), iox);
        }
		return returnValue;
	}

	/* */
    private List nodeRequirements(JXPathContext theNodeContext) {
        return
            new ListBuilder()
                .addAll(
                    StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize((Iterator<Map>)theNodeContext.iterate("requirements"), 16), false)
                                                    .flatMap(m -> this.nodeRequirement(m, theNodeContext).stream())
                    //nicer that the ListBuilder buy cannot handle the empty lists, i.e. it will generate empty requirement lists
                    //								.collect(Collectors.toList())
                                                    .toArray())
                .buildOpt();
    }

    /*
     * @param theSpec the requirement entry that appears within the node specification
     * @param theNodeContext .. Should I pass the root context instead of assuming that the nodes context has it as parent?
     * @return a List as one requirement (definition) could end up being instantiated multiple times
     */
    private List nodeRequirement(final Map theSpec, JXPathContext theNodeContext/*Iterator theTargets*/) {

        final ListBuilder value = new ListBuilder();

        final Map target = (Map)theSpec.get("target");
        final Map capability = (Map)theSpec.get(CAPABILITY);
        final Map relationship = (Map)theSpec.get(RELATIONSHIP);

        //this are actual assignments
        for (Iterator i = theNodeContext.getParentContext().iterate("/relations[@n2='" + theNodeContext.getValue("nid") + "']/meta[@p2='" + theSpec.get(NAME) +"']"); i.hasNext(); ) {

           String targetNodeName = (String)((Map)i.next()).get("n1");

           //make sure target exists
           Map targetNode = (Map)theNodeContext.getParentContext().getValue("/nodes[@nid='" + targetNodeName + "']");
           if (null == targetNode) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Relation points to non-existing node {}", targetNodeName);
                continue; //this risks of producing a partial template ..
           }

           value.add(new MapBuilder().put(theSpec.get(NAME), new MapBuilder()
                       .putOpt("node", targetNode.get(NAME) + "_" + targetNode.get("nid"))
                       .putOpt(CAPABILITY, capability == null ? null : capability.get(NAME))
                       .putOpt(RELATIONSHIP, relationship == null ? null : relationship.get("type"))
                       .build()).build());
        }
		addTemporary(theSpec, theNodeContext, value, capability, relationship);

		if (value.isEmpty()) {
           value.add(new MapBuilder().put(theSpec.get(NAME), new MapBuilder()
                       .putOpt("node", target == null ? null : target.get(NAME) + "_" + target.get("nid"))
                       .putOpt(CAPABILITY, capability == null ? null : capability.get(NAME))
                       .putOpt(RELATIONSHIP, relationship == null ? null : relationship.get("type"))
                       .build()).build());
        }

        return value.build();
    }

	private void addTemporary(Map theSpec, JXPathContext theNodeContext, ListBuilder value, Map capability, Map relationship) {
		//temporary
		for (Iterator i = theNodeContext.getParentContext().iterate("/relations[@n1='" + theNodeContext.getValue("nid") + "']/meta[@p1='" + theSpec.get(NAME) +"']"); i.hasNext(); ) {

           String targetNodeName = (String)((Map)i.next()).get("n2");

           Map targetNode = (Map)theNodeContext.getParentContext().getValue("/nodes[@nid='" + targetNodeName + "']");
           //make sure target exists
           if (null == targetNode) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Relation points to non-existing node {}", targetNode);
                continue; //this risks of producing a partial template ..
           }

           value.add(new MapBuilder().put(theSpec.get(NAME), new MapBuilder()
                       .putOpt("node", targetNode.get(NAME) + "_" + targetNode.get("nid"))
                       .putOpt(CAPABILITY, capability == null ? null : capability.get(NAME))
                       .putOpt(RELATIONSHIP, relationship == null ? null : relationship.get("type"))
                       .build()).build());
        }
		//end temporary
	}

	/* */
    private Map nodeCapabilities(JXPathContext theNodeContext) {
        return
            new MapBuilder()
                .putAll(
                    StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize((Iterator<Map>)theNodeContext.iterate("capabilities"), 16), false)
                                             .map(m -> this.nodeCapability(m))
                                             .filter(c -> c != null)
                                             ::iterator)
                .buildOpt();
    }

    /**
     * this handles a capability assignment which only includes properties and attributes so unless there
     * are any properties/attributes assignments we might not generate anything
     */
    private Map.Entry nodeCapability(final Map theSpec) {
        List<Map> properties = (List<Map>) theSpec.get(PROPERTIES);
        if (properties == null || properties.isEmpty()) {
            return null;
        }

        return new AbstractMap.SimpleEntry(theSpec.get(NAME),
                new MapBuilder()
                        .put(PROPERTIES,
                                new MapBuilder().putAll(properties.stream()
                                        .filter(p -> p.containsKey(ASSIGNMENT) ||
                                                p.containsKey(VALUE))
                                        .map(p -> new AbstractMap.SimpleEntry(
                                                p.get(NAME),
                                                p.containsKey(ASSIGNMENT) ?
                                            ((Map) p.get(ASSIGNMENT)).get(VALUE)
                                                        : p.get(VALUE))
                                                )
                                        ::iterator)
                                .build())
                        .build());
    }


    /* */
    private Object nodeMetadata(JXPathContext theNodeContext) {
        return
            new MapBuilder()
                .putAll(
                        this.metas
                            .stream()
                            .flatMap(m -> {
                                                Object v = theNodeContext.getValue(m);
                                                if (v == null) {
                                                    return Stream.empty();
                                                }
                                                if (v instanceof Map) {
                                                    return ((Map) v).entrySet()
                                                            .stream()
                                                            .map(e -> new AbstractMap.SimpleEntry<String, Object>
                                                                    (((Map.Entry) e).getKey().toString(),
                                                                            ((Map.Entry) e).getValue().toString()));
                                                }
                                                return Stream.of(new AbstractMap.SimpleEntry<String,Object>(m, v.toString()));
                                            })
                        ::iterator)
                .buildOpt();
    }


    public static String toString(Object theVal) {
        return new Yaml().dump(theVal);
    }
  

    public static void main(String[] theArgs) throws Exception {
        debugLogger.log(LogLevel.DEBUG, Recycler.class.getName(),
                Recycler.toString(
                new Recycler().recycle(new java.io.FileReader(theArgs[0]))));
    }
}

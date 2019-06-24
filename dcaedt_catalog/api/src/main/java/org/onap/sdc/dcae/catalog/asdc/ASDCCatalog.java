package org.onap.sdc.dcae.catalog.asdc;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.commons.*;
import org.onap.sdc.dcae.checker.*;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceDetailed;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class ASDCCatalog implements Catalog {

    private
    static final String JXPATH_NOT_FOUND_EXCEPTION = "JXPathNotFoundException {}";
    private
    static final String OCCURRENCES = "occurrences";
    private
    static final String TOPOLOGY_TEMPLATE_NODE_TEMPLATES = "/topology_template/node_templates";
    private
    static final String NODES_NAME = "/nodes[name='";
    private
    static final String ITEM_ID = "itemId";
    private
    static final String CAPABILITY = "capability";
    private
    static final String NAME = "name";
    private
    static final String ID = "id";
    private
    static final String DESCRIPTION = "description";
    private
    static final String PROPERTIES = "']/properties";
    private
    static final String TOPOLOGY_TEMPLATE_NODE_TEMPLATES1 = "/topology_template/node_templates/";
    private
    static final String PROPERTIES_NAME = "']/properties[name='";
    private
    static final String CAPABILITIES = "']/capabilities";
    private
    static final String CAPABILITIES_NAME = "']/capabilities[name='";

    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private ProxyBuilder proxies;
    private Map<Target, JXPathContext> contexts = new HashMap<>();

    // resource and its catalog
    private Map<UUID, org.onap.sdc.dcae.checker.Catalog> catalogs = new HashMap<>();

    public ASDCCatalog() {
        this.proxies = new ProxyBuilder().withConverter(v -> v == null ? null : UUID.fromString(v.toString()), UUID.class)
                .withExtensions(
                new ImmutableMap.Builder<String, BiFunction<Proxy, Object[], Object>>().put("data", (proxy, args) -> proxy.data())
                        .build()).withContext(new ImmutableMap.Builder<String, Object>().put("catalog", this).build());
    }

    public <T> T proxy(JSONObject theData, Class<T> theType) {
        return proxies.build(theData, theType);
    }


    public CatalogTemplateAction template(ResourceDetailed resource) {
        return new CatalogTemplateAction(resource);
    }

    public CatalogTypeAction type(String theItemId, String theName) {
        return new CatalogTypeAction(UUID.fromString(theItemId), theName);
    }

    private Object resolve(Target theTarget, String thePath) {
        try {
            return contexts.get(theTarget).getValue(thePath);
        } catch (JXPathNotFoundException pnfx) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), JXPATH_NOT_FOUND_EXCEPTION, pnfx);
            return null;
        }
    }

    // covers common TOSCA pattern of single entry maps
    private Map.Entry<String, Map> toEntry(Object theValue) {
        return (Map.Entry<String, Map>) ((Map) theValue).entrySet().iterator().next();
    }

    private Map selectEntries(Map theOriginal, String... theKeys) {
        Arrays.sort(theKeys);
        return ((Set<Map.Entry>) theOriginal.entrySet()).stream()
                .filter(e -> Arrays.binarySearch(theKeys, e.getKey().toString()) >= 0)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private Map evictEntries(Map theOriginal, String... theKeys) {
        Arrays.sort(theKeys);
        return ((Set<Map.Entry>) theOriginal.entrySet()).stream()
                .filter(e -> Arrays.binarySearch(theKeys, e.getKey().toString()) < 0)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    protected <T> Stream<T> stream(Iterator<T> theSource) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(theSource,
                Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.IMMUTABLE), false);
    }

    private static void dumpTargets(String theDirName, Collection<Target> theTargets) {
        File targetDir = new File(theDirName);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + theDirName);
        }
        for (Target t : theTargets) {
            try (FileWriter dump = new FileWriter(new File(theDirName, t.getName()))) {
                IOUtils.copy(t.open(), dump);
                dump.close();
            } catch (IOException e) {
                debugLogger.log(LogLevel.DEBUG, "ASDCCatalog", "IOException {}", e);
            }
        }
    }

    private static URI asURI(String theValue) {
        try {
            return new URI(theValue);
        } catch (URISyntaxException urisx) {
            throw new IllegalArgumentException("Invalid URI", urisx);
        }
    }

    private static UUID asUUID(String theValue) {
        return UUID.fromString(theValue);
    }

    private org.onap.sdc.dcae.checker.Catalog getCachedCatalogItem(UUID theResourceId) {
        return this.catalogs.get(theResourceId);
    }

    public boolean hasCachedItem(String uuid) {
    	return this.catalogs.containsKey(asUUID(uuid));
	}

    public class CatalogTemplateAction implements Catalog.TemplateAction {

        private ResourceDetailed resourceMetadata;
        private Target target;
        private org.onap.sdc.dcae.checker.Catalog catalog;
        private JXPathContext ctx = JXPathContext.newContext(new HashMap());

        private boolean doNodes, doNodeProperties, doNodePropertiesAssignments, doNodeRequirements, doNodeCapabilities,
                doNodeCapabilityProperties, doNodeCapabilityPropertyAssignments;


		CatalogTemplateAction(ResourceDetailed resourceMetadata) {
			this.resourceMetadata = resourceMetadata;
		}

        public CatalogTemplateAction withInputs() {
            return this;
        }

        public CatalogTemplateAction withOutputs() {
            return this;
        }

        public CatalogTemplateAction withNodes() {
            this.doNodes = true;
            return this;
        }

        CatalogTemplateAction doNodes() {
            if (!this.doNodes) {
                return this;
            }

            Map nodes = (Map) resolve(this.target, TOPOLOGY_TEMPLATE_NODE_TEMPLATES);
            if (nodes == null) {
                return this;
            }

            ctx.setValue("/nodes",
                    nodes.entrySet().stream()
                            .map(nodeEntry -> new MapBuilder().put(NAME, ((Map.Entry) nodeEntry).getKey())
                                    .put(DESCRIPTION, resourceMetadata.getToscaModelURL())
                                    .putAll(selectEntries((Map) ((Map.Entry) nodeEntry).getValue(), "type")).build())
                            .collect(Collectors.toList()));

            return this;
        }

        // pre-requisite: a call to 'withNodes'
        public CatalogTemplateAction withNodeProperties() {
            this.doNodeProperties = true;
            return this;
        }

        protected CatalogTemplateAction doNodeProperties() {
            if (!this.doNodeProperties) {
                return this;
            }

            Map nodes = (Map) resolve(this.target, TOPOLOGY_TEMPLATE_NODE_TEMPLATES);
            if (nodes == null) {
                return this;
            }

            nodes.entrySet().stream().forEach(node -> ctx.setValue(
                    NODES_NAME + ((Map.Entry) node).getKey() + PROPERTIES,
                    stream(catalog.facets(Construct.Node, Facet.properties,
                            ((Map) ((Map.Entry) node).getValue()).get("type").toString()))
                                    .map(propEntry -> new MapBuilder().put(NAME, propEntry.getKey())
                                            .putAll((Map) propEntry.getValue()).build())
                                    .collect(Collectors.toList())));

            return this;
        }

        // pre-requisite: a call to 'withNodesProperties'
        public CatalogTemplateAction withNodePropertiesAssignments() {
            this.doNodePropertiesAssignments = true;
            return this;
        }

        CatalogTemplateAction doNodePropertiesAssignments() {
            if (!this.doNodePropertiesAssignments) {
                return this;
            }

            Map nodes = (Map) resolve(this.target, TOPOLOGY_TEMPLATE_NODE_TEMPLATES);
            if (nodes == null) {
                return this;
            }

            nodes.entrySet().forEach(node -> {
                List nodeProps;
                try {
                    nodeProps = (List) ctx.getValue(NODES_NAME + ((Map.Entry) node).getKey() + PROPERTIES);
                } catch (JXPathNotFoundException pnfx) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), JXPATH_NOT_FOUND_EXCEPTION, pnfx);
                    return;
                }

                nodeProps.forEach(prop -> {
                    // pick from
                    String propPath = TOPOLOGY_TEMPLATE_NODE_TEMPLATES1 + ((Map.Entry) node).getKey()
                            + "/properties/" + ((Map) prop).get(NAME);
                    Object propValue = resolve(this.target, propPath);
                    // to conform with the db based api we should analyze the
                    // value for function calls
                    // dump at ..
                    propPath = NODES_NAME + ((Map.Entry) node).getKey() + PROPERTIES_NAME
                            + ((Map) prop).get(NAME) + "']";
                    if (propValue != null) {
                        ctx.setValue(propPath + "/assignment",
                                new ImmutableMap.Builder().put("value", propValue).build());
                    }
                });
            });

            return this;
        }

        Map renderRequirementDefinition(Map.Entry theReq) {
            Map def = (Map) theReq.getValue();
            return new MapBuilder().put(NAME, theReq.getKey())
                    // capability must be present
                    .put(CAPABILITY,
                            new MapBuilder().put(NAME, def.get(CAPABILITY))
                                    .put(ID, this.target.getName() + "/" + def.get(CAPABILITY)).build())
                    .putAll(evictEntries(def, CAPABILITY)).build();
        }

        // TODO: see how this comes out of neo and match it
        Map renderRequirementAssignment(Map.Entry theReq) {
            Map def = (Map) theReq.getValue();
            return new MapBuilder().put(NAME, theReq.getKey())
                    // capability must be present
                    .put(CAPABILITY,
                            new MapBuilder().put(NAME, def.get(CAPABILITY))
                                    // we provide an id only if the capability
                                    // points to a type
                                    .putOpt(ID,
                                            catalog.hasType(Construct.Capability, (String) def.get(CAPABILITY))
                                                    ? (this.target.getName() + "/" + def.get(CAPABILITY)) : null)
                                    .build())
                    .putAll(evictEntries(def, CAPABILITY)).build();
        }

        public CatalogTemplateAction withNodeRequirements() {
            this.doNodeRequirements = true;
            return this;
        }

        CatalogTemplateAction doNodeRequirements() {
            if (!this.doNodeRequirements) {
                return this;
            }

            // requirements come first from the type and then can be further
            // refined by their assignment within the
            // node template
            Map nodes = (Map) resolve(this.target, TOPOLOGY_TEMPLATE_NODE_TEMPLATES);
            if (nodes == null) {
                return this;
            }

            // type
            nodes.entrySet()
                    .forEach(
                            node -> ctx
                                    .setValue(
                                            NODES_NAME
                                                    + ((Map.Entry) node)
                                                            .getKey()
                                                    + "']/requirements",
                                            StreamSupport
                                                    .stream(Spliterators.spliteratorUnknownSize(
                                                            catalog.requirements(((Map) ((Map.Entry) node).getValue())
                                                                    .get("type").toString()),
                                                            Spliterator.NONNULL | Spliterator.DISTINCT
                                                                    | Spliterator.IMMUTABLE),
                                                            false)
                                                    .map((Map.Entry reqEntry) -> renderRequirementDefinition(reqEntry))
                                                    .collect(Collectors.toList())));

            // merge assignments on top of definitions
            nodes.entrySet().forEach(node -> {
                List nodeReqsAssigns = (List) resolve(this.target,
                        TOPOLOGY_TEMPLATE_NODE_TEMPLATES1 + ((Map.Entry) node).getKey() + "/requirements");
                if (nodeReqsAssigns == null) {
                    return;
                }
                nodeReqsAssigns.forEach(req -> {
                    Map.Entry reqAssign = toEntry(req);
                    catalog.mergeDefinitions((Map) ctx.getValue(NODES_NAME + ((Map.Entry) node).getKey()
                            + "']/requirements[name='" + reqAssign.getKey() + "']"),
                            renderRequirementAssignment(reqAssign));
                });
            });

            return this;
        }

        public CatalogTemplateAction withNodeCapabilities() {
            this.doNodeCapabilities = true;
            return this;
        }

        Map renderCapabilityDefinition(Map.Entry theCap) {
            Map def = (Map) theCap.getValue();
            return new MapBuilder().put(NAME, theCap.getKey())
                    .put("type",
                            new MapBuilder().put(NAME, def.get("type"))
                                    .put(ID, this.target.getName() + "/" + def.get("type")).build())
                    .putAll(evictEntries(def, "properties", "type")).build();
        }

        CatalogTemplateAction doNodeCapabilities() {
            if (!this.doNodeCapabilities) {
                return this;
            }

            Map nodes = (Map) resolve(this.target, TOPOLOGY_TEMPLATE_NODE_TEMPLATES);
            if (nodes == null) {
                return this;
            }

            // collect capabilities through the node type hierarchy

            // we evict the properties from the node type capability declaration
            // (when declaring a capability with the
            // node type some re-definition of capability properties can take
            // place).
            nodes.entrySet().stream()
                    .forEach(node -> ctx.setValue(NODES_NAME + ((Map.Entry) node).getKey() + CAPABILITIES,

                            stream(catalog.facets(Construct.Node, Facet.capabilities,
                                    ((Map) ((Map.Entry) node).getValue()).get("type").toString()))
                                            .map(this::renderCapabilityDefinition)
                                            .collect(Collectors.toList())));

            return this;
        }

        public CatalogTemplateAction withNodeCapabilityProperties() {
            this.doNodeCapabilityProperties = true;
            return this;
        }

        CatalogTemplateAction doNodeCapabilityProperties() {

            if (!this.doNodeCapabilityProperties) {
                return this;
            }

            Map nodes = (Map) resolve(this.target, TOPOLOGY_TEMPLATE_NODE_TEMPLATES);
            if (nodes == null) {
                return this;
            }

            // pick up all the properties from the capability type hierarchy
            // definition
            nodes.entrySet().forEach(node -> {
                List nodeCapabilities = (List) ctx
                        .getValue(NODES_NAME + ((Map.Entry) node).getKey() + CAPABILITIES);
                if (nodeCapabilities == null) {
                    return;
                }

                // collect properties from the capability type hierarchy
                nodeCapabilities.forEach(capability -> {
                    List capabilityProperties = StreamSupport
                            .stream(Spliterators.spliteratorUnknownSize(
                                    catalog.facets(Construct.Capability, Facet.properties,
                                            ((Map)((Map)capability).get("type")).get(NAME).toString()),
                                    Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.IMMUTABLE), false)
                            .map((Map.Entry capEntry) -> new MapBuilder().put(NAME, capEntry.getKey())
                                    .putAll((Map) capEntry.getValue()).build())
                            .collect(Collectors.toList());

                    if (!capabilityProperties.isEmpty()) {
                        ctx.setValue(NODES_NAME + ((Map.Entry) node).getKey() + CAPABILITIES_NAME
                                + ((Map) capability).get(NAME) + PROPERTIES, capabilityProperties);
                    }
                });

                // and go over the node type (hierarchy) and pick up any
                // re-definitions from there.
                StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(
                                catalog.facets(Construct.Node, Facet.capabilities,
                                        ((Map) ((Map.Entry) node).getValue()).get("type").toString()),
                                Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.IMMUTABLE), false)
                        .forEach((Map.Entry capability) -> {
                            // for each capability property that has some node
                            // type level re-definition
                            Map properties = (Map) ((Map) capability.getValue()).get("properties");
                            if (properties == null) {
                                return;
                            }

                            properties.entrySet().forEach(property -> {
                                String propertyLoc = NODES_NAME + ((Map.Entry) node).getKey()
                                        + CAPABILITIES_NAME + ((Map) capability).get(NAME)
                                        + PROPERTIES_NAME + ((Map.Entry) property).getKey() + "']";
                                ctx.setValue(propertyLoc, catalog.mergeDefinitions((Map) ctx.getValue(propertyLoc),
                                        (Map) ((Map.Entry) property).getValue()));
                            });
                        });
            });

            return this;
        }

        public CatalogTemplateAction withNodeCapabilityPropertyAssignments() {
            this.doNodeCapabilityPropertyAssignments = true;
            return this;
        }

        CatalogTemplateAction doNodeCapabilityPropertyAssignments() {
            if (!this.doNodeCapabilityPropertyAssignments) {
                return this;
            }

            // this is a wasteful: we go over all declared
            // nodes/capabilities/properties and check if there is an assigned
            // value in the actual template. It is optimal to approach the
            // problem from the other direction: go over delared
            // assignments and set them in the output structure ..

            List nodes = null;
            try {
                nodes = (List) ctx.getValue("/nodes");
            } catch (JXPathNotFoundException pnfx) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), JXPATH_NOT_FOUND_EXCEPTION, pnfx);
                return this;
            }

            nodes.forEach(node -> {
                List capabilities = (List) ctx.getValue(NODES_NAME + ((Map) node).get(NAME) + CAPABILITIES);
                if (capabilities == null) {
                    return;
                }

                capabilities.forEach(capability -> {
                    List properties;
                    try {
                        properties = (List) ctx.getValue(NODES_NAME + ((Map) node).get(NAME)
                                + CAPABILITIES_NAME + ((Map) capability).get(NAME) + PROPERTIES);
                    } catch (JXPathNotFoundException pnfx) {
                        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), JXPATH_NOT_FOUND_EXCEPTION, pnfx);
                        return;
                    }

                    properties.forEach(property -> {
                        String location = NODES_NAME + ((Map) node).get(NAME) + CAPABILITIES_NAME
                                + ((Map) capability).get(NAME) + PROPERTIES_NAME + ((Map) property).get(NAME)
                                + "']/assignment";

                        // pick the value from the original
                        try {
                            Object assignment = resolve(this.target,
                                    TOPOLOGY_TEMPLATE_NODE_TEMPLATES1 + ((Map) node).get(NAME) + "/capabilities/"
                                            + ((Map) capability).get(NAME) + "/properties/"
                                            + ((Map) property).get(NAME));
                            if (assignment != null) {
                                ctx.setValue(location, new ImmutableMap.Builder().put("value", assignment).build());
                            }
                        } catch (JXPathNotFoundException pnfx) {
                            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), JXPATH_NOT_FOUND_EXCEPTION, pnfx);
                            // it's ok, no assignment
                        }
                    });
                });
            });

            return this;
        }

        public CatalogTemplateAction withPolicies() {
            return this;
        }

        public CatalogTemplateAction withPolicyProperties() {
            return this;
        }

        public CatalogTemplateAction withPolicyPropertiesAssignments() {
            return this;
        }

        public Future<Template> execute() {

            if (this.target == null) {

                UUID resourceId = asUUID(resourceMetadata.getUuid());
                this.catalog = ASDCCatalog.this.catalogs.get(resourceId);

                // if we find a catalog for this resource we have to figure out
                // if it contains the required target ..

                try {

                    Checker checker = new Checker();
					TargetLocator locator = new ASDCLocator(resourceMetadata.getArtifacts(), ASDCCatalog.this.catalogs.get(resourceId));
                    checker.setTargetLocator(locator);
                    Target template = locator.resolve("template");
                    if (template == null) {
                        return Futures.failedFuture(new Exception("Failed to locate template in " + resourceMetadata));
                    }

                    checker.check(template);

                    for (Target t : checker.targets()) {
                        if (t.getReport().hasErrors()) {
                            dumpTargets(resourceId.toString(), checker.targets());
                            return Futures.failedFuture(new Exception("Failed template validation: " + t.getReport()));
                        }
                    }

                    this.target = template;
                    this.catalog = checker.catalog();
                    ASDCCatalog.this.catalogs.put(resourceId, this.catalog);
                    // we should only be doing this if we discovered an update
                    // (by checking timestamps). Actually, we should
                    // only do the artifact fetching if we detect an update
                    ASDCCatalog.this.contexts.put(template, JXPathContext.newContext(template.getTarget()));
                } catch (Exception x) {
                    return Futures.failedFuture(x);
                }
            }

            this.doNodes().doNodeProperties().doNodePropertiesAssignments().doNodeRequirements().doNodeCapabilities()
                    .doNodeCapabilityProperties().doNodeCapabilityPropertyAssignments();

            JSONObject pack = new JSONObject((Map) ctx.getContextBean()).put(NAME, this.target.getName())
                    .put(ID, resourceMetadata.getUuid())
                    .put(ITEM_ID, resourceMetadata.getToscaModelURL());
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), pack.toString(2));

            return Futures.succeededFuture(proxies.build(pack, Template.class));
        }
    }

    public class CatalogTypeAction implements Catalog.TypeAction {

        private String name;
        private UUID resourceId;
        private JXPathContext ctx;

        private boolean doHierarchy = false, doRequirements = false, doCapabilities = false;

        private CatalogTypeAction(UUID theResourceId, String theName) {
            this.resourceId = theResourceId;
            this.name = theName;
        }

        public CatalogTypeAction withHierarchy() {
            this.doHierarchy = true;
            return this;
        }

        CatalogTypeAction doHierarchy(org.onap.sdc.dcae.checker.Catalog theCatalog) {
            if (!this.doHierarchy) {
                return this;
            }

            ctx.setValue("/hierarchy",
                    stream(theCatalog.hierarchy(Construct.Node, this.name)).skip(1) // skip
                                                                                    // self
                            .map((Map.Entry type) -> new MapBuilder()
                                    .put(NAME, type.getKey()).put(ID, resourceId + "/" + type.getKey())
                                    .putOpt(DESCRIPTION, ((Map) type.getValue()).get(DESCRIPTION)).build())
                            // renderEntry((Map.Entry)type,
                            // "description").build())
                            .collect(Collectors.toList()));
            return this;
        }

        public CatalogTypeAction withRequirements() {
            this.doRequirements = true;
            return this;
        }

        CatalogTypeAction doRequirements(org.onap.sdc.dcae.checker.Catalog theCatalog) {
            if (!this.doRequirements) {
                return this;
            }

            ctx.setValue("requirements", stream(theCatalog.requirements(this.name)).map((Map.Entry req) -> {
                String capability = (String) ((Map) req.getValue()).get(CAPABILITY),
                        node = (String) ((Map) req.getValue()).get(CAPABILITY);
                return new MapBuilder().put(NAME, req.getKey()).put(ID, resourceId + "/" + req.getKey())
                        .put(OCCURRENCES, ((Map) req.getValue()).get(OCCURRENCES))
                        .put(CAPABILITY,
                                new MapBuilder().put(NAME, capability)
                                        // if the capability points to a
                                        // capability type then encode
                                        // the type reference, else it is a name
                                        // (within a node type)
                                        .put(ID,
                                                getCachedCatalogItem(resourceId).hasType(Construct.Capability, capability)
                                                        ? (resourceId + "/" + capability) : capability)
                                        .build())
                        .put("node", new MapBuilder().putOpt(NAME, node).putOpt(ID, node == null ? null
                                : (resourceId + "/" + node)).buildOpt())
                        .put("relationship", ((Map) req.getValue()).get("relationship"))
                        // renderEntry((Map.Entry)requirement, "occurrences",
                        // "node", "capability", "relationship")
                        .build();
            }).collect(Collectors.toList()));

            return this;
        }

        public CatalogTypeAction withCapabilities() {
            this.doCapabilities = true;
            return this;
        }

        CatalogTypeAction doCapabilities(org.onap.sdc.dcae.checker.Catalog theCatalog) {
            if (!this.doCapabilities) {
                return this;
            }

            ctx.setValue("capabilities",
                    stream(theCatalog
                            .facets(Construct.Node, Facet.capabilities,
                                    this.name))
                                            .map((Map.Entry capability) -> new MapBuilder()
                                                    .put(NAME, capability.getKey()).put("type",
                                                            new MapBuilder()
                                                                    .put(NAME, ((Map) capability.getValue())
                                                                            .get("type"))
                                                                    .put(ID,
                                                                            resourceId + "/"
                                                                                    + ((Map) capability.getValue())
                                                                                            .get("type"))
                                                                    .build())
                                                    .put(OCCURRENCES,
                                                            ((Map) capability.getValue()).get(OCCURRENCES))
                                                    .putOpt("validSourceTypes",
                                                            ((Map) capability.getValue()).get("validSourceTypes"))
                                                    .build()
                                            ).collect(Collectors.toList()));
            return this;
        }

        public Future<Type> execute() {
            org.onap.sdc.dcae.checker.Catalog catalog = ASDCCatalog.this.catalogs.get(this.resourceId);
            if (catalog == null) {
                return Futures.failedFuture(new Exception("No catalog available for resource " + this.resourceId
                        + ". You might want to fetch the model first."));
            }

            if (!catalog.hasType(Construct.Node, this.name)) {
                return Futures.failedFuture(
                        new Exception("No " + this.name + " type in catalog for resource " + this.resourceId));
            }

            this.ctx = JXPathContext
                    .newContext(new MapBuilder().put(NAME, this.name).put(ID, this.resourceId + "/" + this.name)
                            .put(ITEM_ID, this.resourceId + "/" + this.name).build());

            this.doHierarchy(catalog).doRequirements(catalog).doCapabilities(catalog);

            JSONObject pack = new JSONObject((Map) this.ctx.getContextBean());
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), pack.toString(2));

            return Futures.succeededFuture(proxies.build((Map) ctx.getContextBean(), Type.class));
        }
    }

    public class ASDCLocator implements TargetLocator {

		private List<Artifact> artifacts;
        private org.onap.sdc.dcae.checker.Catalog catalog;

        private ASDCLocator(List<Artifact> theArtifacts, org.onap.sdc.dcae.checker.Catalog theCatalog) {
            this.artifacts = theArtifacts;
            this.catalog = theCatalog;
        }

        public boolean addSearchPath(URI theURI) {
            return false;
        }

        public boolean addSearchPath(String thePath) {
            return false;
        }

        public Iterable<URI> searchPaths() {
            return Collections.emptySet();
        }

        public Target resolve(String theName) {

			Artifact targetArtifact = this.artifacts.stream().filter(a -> StringUtils.containsIgnoreCase(a.getArtifactName(), theName)).findAny().orElse(null);

            if (targetArtifact == null) {
                return null;
            }

            ASDCTarget target;
            if (this.catalog != null) {
                // this is the caching!!
                target = (ASDCTarget) this.catalog.getTarget(asURI(targetArtifact.getArtifactURL()));
                if (target != null) {
                    return target;
                }
            }

            return new ASDCTarget(targetArtifact);
        }
    }

    public class ASDCTarget extends Target {

        private Artifact artifact;

        private ASDCTarget(Artifact theArtifact) {
            super(theArtifact.getArtifactName(), asURI(theArtifact.getArtifactURL()));
            this.artifact = theArtifact;
        }

        // here is a chance for caching within the catalog! Do not go fetch the
        // artifact if it has not been changed since the
        // last fetch.

        @Override
        public Reader open() throws IOException {
            return new StringReader(this.artifact.getPayloadData());
        }

    }
}

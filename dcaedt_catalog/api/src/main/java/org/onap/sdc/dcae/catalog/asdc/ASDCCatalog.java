package org.onap.sdc.dcae.catalog.asdc;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.commons.*;
import org.onap.sdc.dcae.checker.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("ALL")
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
    static final String LABELS = "labels";
    private
    static final String ARTIFACT_URL = "artifactURL";
    private
    static final String CAPABILITY = "capability";
    private
    static final String DATABASE = "Database";
    private
    static final String COLLECTOR = "Collector";
    private
    static final String MICROSERVICE = "Microservice";
    private
    static final String ANALYTICS = "Analytics";
    private
    static final String POLICY = "Policy";
    private
    static final String SOURCE = "Source";
    private
    static final String UTILITY = "Utility";
    private
    static final String NAME = "name";
    private
    static final String ID = "id";
    private
    static final String ARTIFACT_NAME = "artifactName";
    private
    static final String DESCRIPTION = "description";
    private
    static final String MODELS = "models";
    private
    static final String ARTIFACTS = "artifacts";
    private
    static final String ITEMS = "items";
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

    private ASDC asdc;

    private JSONObject folders = new JSONObject();
    private String[] folderFields = new String[] {ID, ITEM_ID, NAME};

    private ProxyBuilder proxies;
    private Map<Target, JXPathContext> contexts = new HashMap<>();

    // resource and its catalog
    private Map<UUID, org.onap.sdc.dcae.checker.Catalog> catalogs = new HashMap<>();

    public ASDCCatalog(URI theURI) {

        this.asdc = new ASDC();
        this.asdc.setUri(theURI);

        initFolders();

        this.proxies = new ProxyBuilder().withConverter(v -> v == null ? null : UUID.fromString(v.toString()), UUID.class)
                .withExtensions(
                new ImmutableMap.Builder<String, BiFunction<Proxy, Object[], Object>>().put("data", (proxy, args) -> proxy.data())
                        .build()).withContext(new ImmutableMap.Builder<String, Object>().put("catalog", this).build());
    }

    private void initFolders() {

        JSONArray labels = new JSONArray();
        labels.put("Folder");
        labels.put("DCAE");
        labels.put("Superportfolio"); // for CCD compatibility

        folders.put(DATABASE, new JSONObject().put(NAME, DATABASE).put(ID, "dcae_database")
                .put(ITEM_ID, DATABASE).put(LABELS, labels));
        folders.put(COLLECTOR, new JSONObject().put(NAME, COLLECTOR).put(ID, "dcae_collector")
                .put(ITEM_ID, COLLECTOR).put(LABELS, labels));
        folders.put(MICROSERVICE, new JSONObject().put(NAME, MICROSERVICE).put(ID, "dcae_microservice")
                .put(ITEM_ID, MICROSERVICE).put(LABELS, labels));
        folders.put(ANALYTICS, new JSONObject().put(NAME, ANALYTICS).put(ID, "dcae_analytics")
                .put(ITEM_ID, ANALYTICS).put(LABELS, labels));
        folders.put(POLICY, new JSONObject().put(NAME, POLICY).put(ID, "dcae_policy").put(ITEM_ID, POLICY)
                .put(LABELS, labels));
        folders.put(SOURCE, new JSONObject().put(NAME, SOURCE).put(ID, "dcae_source").put(ITEM_ID, SOURCE)
                .put(LABELS, labels));
        folders.put(UTILITY, new JSONObject().put(NAME, UTILITY).put(ID, "dcae_utility")
                .put(ITEM_ID, UTILITY).put(LABELS, labels));
    }

    public URI getUri() {
        return this.asdc.getUri();
    }

    public String namespace() {
        return "asdc";
    }

    public boolean same(Catalog theCatalog) {
        return true;
    }

    public <T> T proxy(JSONObject theData, Class<T> theType) {
        return proxies.build(theData, theType);
    }

    /** */
    public Future<Folders> roots() {

        Folders roots = new Folders();
        for (Iterator fi = folders.keys(); fi.hasNext();) {
            roots.add(proxies.build(folders.getJSONObject((String) fi.next()), Folder.class));
        }
        return Futures.succeededFuture(roots);
    }

    /** */
    public Future<Folders> rootsByLabel(String theLabel) {

        Folders roots = new Folders();
        for (Iterator fi = folders.keys(); fi.hasNext();) {
            JSONObject folder = folders.getJSONObject((String) fi.next());
            JSONArray labels = folder.getJSONArray(LABELS);

            for (int i = 0; i < labels.length(); i++) {
                if (labels.get(i).equals(theLabel)) {
                    roots.add(proxies.build(folder, Folder.class));
                }
            }
        }
        return Futures.succeededFuture(roots);
    }

    public Future<Mixels> lookup(JSONObject theSelector) {
        return Futures.succeededFuture(new Mixels());
    }

    public Future<Mixels> lookup(String theAnnotation, JSONObject theSelector) {
        return Futures.succeededFuture(new Mixels());
    }

    public ItemAction item(String theItemId) {
        return new ResourceAction(UUID.fromString(theItemId));
    }

    public CatalogFolderAction folder(String theFolderId) {
        return new CatalogFolderAction(theFolderId);
    }

    public CatalogTemplateAction template(String theId) {
        return new CatalogTemplateAction(theId);
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

    private JSONArray selectModels(JSONArray theArtifacts) {
        JSONArray models = new JSONArray();
        if (theArtifacts == null) {
            return models;
        }

        for (int i = 0; i < theArtifacts.length(); i++) {
            JSONObject artifact = theArtifacts.getJSONObject(i);
            String name = artifact.optString(ARTIFACT_NAME);
            if (name != null && StringUtils.containsIgnoreCase(name, "template")) {
                models.put(new JSONObject().putOpt(NAME, artifact.optString(ARTIFACT_NAME))
                        .putOpt("version", artifact.optString("artifactVersion"))
                        .putOpt(DESCRIPTION, artifact.optString("artifactType"))
                        .putOpt(ID, artifact.optString(ARTIFACT_URL))
                        .putOpt(ITEM_ID, artifact.optString(ARTIFACT_URL)));
            }
        }
        return models;
    }

    private JSONObject patchResource(JSONObject theResource) {

        theResource.remove("resources");
        theResource.putOpt(ID, theResource.opt("uuid"));
        theResource.putOpt(ITEM_ID, theResource.opt("uuid"));

        return theResource;
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

    private org.onap.sdc.dcae.checker.Catalog getCatalog(UUID theResourceId) {
        return this.catalogs.get(theResourceId);
    }

    private String getArtifactVersion(JSONObject theData) {
        return theData.getString("artifactVersion");
    }

    private String getArtifactName(JSONObject theData) {
        return theData.getString(ARTIFACT_NAME);
    }

    private String getArtifactURL(JSONObject theData) {
        return theData.getString(ARTIFACT_URL);
    }

    private URI getArtifactURI(JSONObject theData) {
        return asURI(theData.getString(ARTIFACT_URL));
    }

    /** */
    public class ResourceAction implements Catalog.ItemAction<Resource> {

        private UUID iid;
        private boolean doModels;

        ResourceAction(UUID theItemId) {
            this.iid = theItemId;
        }

        public ResourceAction withModels() {
            this.doModels = true;
            return this;
        }

        public ResourceAction withAnnotations() {
            return this;
        }

        @Override
        public Future<Resource> execute() {

            return Futures.advance(asdc.getResource(this.iid, JSONObject.class), resourceData -> {
                if (doModels) {
                    resourceData.put(MODELS, selectModels(resourceData.optJSONArray(ARTIFACTS)));
                }
                return proxies.build(patchResource(resourceData), Resource.class);
            });
        }

        protected Future<JSONObject> executeRaw() {

            return Futures.advance(asdc.getResource(this.iid, JSONObject.class), resourceData -> {
                if (doModels) {
                    resourceData.put(MODELS, selectModels(resourceData.optJSONArray(ARTIFACTS)));
                }
                return resourceData;
            }, resourceError -> new RuntimeException("Failed to retrieve item " + this.iid, resourceError));
        }
    }

    public class CatalogFolderAction implements Catalog.FolderAction {

        private boolean doItemModels;
        private String folderName;

        // use the id/UUID of the folder ??
        private CatalogFolderAction(String theFolderName) {
            this.folderName = theFolderName;
        }

        public CatalogFolderAction withAnnotations() {
            return this;
        }

        public CatalogFolderAction withAnnotations(String theSelector) {
            return this;
        }

        public CatalogFolderAction withItems() {
            return this;
        }

        public CatalogFolderAction withItemAnnotations() {
            return this;
        }

        public CatalogFolderAction withItemAnnotations(String theSelector) {
            return this;
        }

        public CatalogFolderAction withItemModels() {
            doItemModels = true;
            return this;
        }

        public CatalogFolderAction withParts() {
            return this;
        }

        public CatalogFolderAction withPartAnnotations() {
            return this;
        }

        public CatalogFolderAction withPartAnnotations(String theSelector) {
            return this;
        }

        @Override
        public Future<Folder> execute() {

            JSONObject folder = folders.optJSONObject(this.folderName);
            if (folder == null) {
                return Futures.failedFuture(new RuntimeException("No such folder " + this.folderName));
            }

            final JSONObject folderView = new JSONObject(folder, folderFields);

            return Futures.advance(asdc.getResources(JSONArray.class, "DCAE Component", this.folderName),
                    resourcesData -> {

                        Actions.CompoundAction<Resource> itemsAction = new Actions.BasicCompoundAction<>();
                        for (int i = 0; i < resourcesData.length(); i++) {
                            JSONObject resource = resourcesData.getJSONObject(i);

                            if (doItemModels) {
                                itemsAction
                                        .addAction(new ResourceAction(asUUID(resource.getString("uuid"))).withModels());
                            } else {
                                folderView.append(ITEMS, patchResource(resource));
                            }
                        }

                        try {
                            List<Resource> items = itemsAction.execute().waitForResult();
                            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Number of DCAE item for : {} is {}", this.folderName, items.size());

                            for (Resource res : filterLatestVersion(items)) {
                                folderView.append(ITEMS, patchResource(res.data()));
                            }
                        } catch (Exception x) {
                            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Exception {}", x);
                            throw new RuntimeException("Failed to retrieve folder items", x);
                        }

                        return proxies.build(folderView, Folder.class);
                    }, resourcesError -> new RuntimeException("Failed to retrieve resources", resourcesError));
        }

        public Collection<Resource> filterLatestVersion(Collection<Resource> items) {
            if (items == null) {
                throw new IllegalArgumentException("null is not acceptable as a list of items");
            }
            Map<UUID, Resource> itemsMap = new HashMap<UUID, Resource>(items.size());
            for (Resource r : items) {
                if (itemsMap.containsKey(r.invariantUUID()) && isNewerVersion(itemsMap, r)) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Avoiding adding item {} since it has a advanced version already", r.toString());
                    continue;
                }
                itemsMap.put(r.invariantUUID(), r);
            }
            return itemsMap.values();
        }

        private boolean isNewerVersion(Map<UUID, Resource> itemsMap, Resource r) {
            return Float.valueOf(itemsMap.get(r.invariantUUID()).version()) > Float.valueOf(r.version());
        }

    }

    /** */
    public class CatalogTemplateAction implements Catalog.TemplateAction {

        private String artifactId;
        private Target target;
        private org.onap.sdc.dcae.checker.Catalog catalog;
        private JXPathContext ctx = JXPathContext.newContext(new HashMap());

        private boolean doNodes, doNodeProperties, doNodePropertiesAssignments, doNodeRequirements, doNodeCapabilities,
                doNodeCapabilityProperties, doNodeCapabilityPropertyAssignments;

        /*
         * expected to be the relative url provided by asdc for the template
         * artifact
         */
        CatalogTemplateAction(String theArtifactId) {
            this.artifactId = theArtifactId;
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
                                    .put(DESCRIPTION, this.artifactId)
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

                String[] parts = this.artifactId.split("/");
                if (parts.length != 8) {
                    return Futures
                            .failedFuture(new Exception("Unexpected artifact id for template " + this.artifactId));
                }

                UUID resourceId = asUUID(parts[5]);
                this.catalog = ASDCCatalog.this.catalogs.get(resourceId);

                // if we find a catalog for this resource we have to figure out
                // if it contains the required target ..

                try {
                    JSONObject resource = new ResourceAction(resourceId).executeRaw().waitForResult();

                    Checker checker = new Checker();
                    TargetLocator locator = new ASDCLocator(resource.getJSONArray(ARTIFACTS),
                            ASDCCatalog.this.catalogs.get(resourceId));
                    checker.setTargetLocator(locator);

                    Target template = locator.resolve("template");
                    if (template == null) {
                        return Futures.failedFuture(new Exception("Failed to locate template in " + resource));
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
                    .put(ID, this.target.getLocation().toString())
                    .put(ITEM_ID, this.target.getLocation().toString());
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), pack.toString(2));

            return Futures.succeededFuture(proxies.build(pack, Template.class));
        }
    }

    public class CatalogTypeAction implements Catalog.TypeAction {

        private String name;
        private UUID resourceId;
        private JXPathContext ctx;

        private boolean doHierarchy = false, doRequirements = false, doCapabilities = false;

        private CatalogTypeAction(UUID theResourceId, /* Construct theConstruct, */ String theName) {
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
                                                getCatalog(resourceId).hasType(Construct.Capability, capability)
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
                                            // renderEntry((Map.Entry)capability,
                                            // "occurrences",
                                            // "validSourceTypes")
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

    public interface Resource extends Catalog.Item<Resource> {

        @Override
        @Proxy.DataMap(map = "uuid")
        public String id();

        public UUID uuid();

        public UUID invariantUUID();

        public String category();

        public String subCategory();

        public String lastUpdaterFullName();

        public String version();

        @Proxy.DataMap(proxy = true, elementType = Artifact.class)
        public Artifacts artifacts();

    }

    public static class Resources extends Elements<Resource> {
    }

    public interface Artifact extends Catalog.Element<Artifact> {

        @Proxy.DataMap(map = ARTIFACT_NAME)
        String name();

        @Proxy.DataMap(map = "artifactType")
        String type();

        @Proxy.DataMap(map = "artifactDescription")
        String description();

        @Proxy.DataMap(map = "artifactUUID")
        UUID uuid();

        @Proxy.DataMap(map = "artifactVersion")
        int version();

    }

    public static class Artifacts extends Elements<Artifact> {
    }

    public class ASDCLocator implements TargetLocator {

        private JSONArray artifacts;
        private org.onap.sdc.dcae.checker.Catalog catalog;

        private ASDCLocator(JSONArray theArtifacts, org.onap.sdc.dcae.checker.Catalog theCatalog) {
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
            JSONObject targetArtifact = null;

            for (int i = 0; i < this.artifacts.length(); i++) {
                JSONObject artifact = this.artifacts.getJSONObject(i);
                String artifactName = artifact.getString(ARTIFACT_NAME);
                if (StringUtils.containsIgnoreCase(artifactName, theName)) {
                    targetArtifact = artifact;
                }
            }

            if (targetArtifact == null) {
                return null;
            }

            ASDCTarget target;
            if (this.catalog != null) {
                // this is the caching!!
                target = (ASDCTarget) this.catalog.getTarget(ASDCCatalog.this.getArtifactURI(targetArtifact));
                if (target != null && target.getVersion().equals(ASDCCatalog.this.getArtifactVersion(targetArtifact))) {
                    return target;
                }
            }

            return new ASDCTarget(targetArtifact);
        }
    }

    public class ASDCTarget extends Target {

        private String content;
        private JSONObject artifact;

        private ASDCTarget(JSONObject theArtifact) {
            super(ASDCCatalog.this.getArtifactName(theArtifact), ASDCCatalog.this.getArtifactURI(theArtifact));
            this.artifact = theArtifact;
        }

        // here is a chance for caching within the catalog! Do not go fetch the
        // artifact if it has not been changed since the
        // last fetch.

        @Override
        public Reader open() throws IOException {
            if (this.content == null) {
                try {
                    this.content = ASDCCatalog.this.asdc
                            .fetch(ASDCCatalog.this.getArtifactURL(this.artifact), String.class).waitForResult();
                } catch (Exception x) {
                    throw new IOException("Failed to load " + ASDCCatalog.this.getArtifactURL(this.artifact), x);
                }
            }

            // should return immediately a reader blocked until content
            // available .. hard to handle errors
            return new StringReader(this.content);
        }

        public String getVersion() {
            return ASDCCatalog.this.getArtifactVersion(this.artifact);
        }

    }

    public static void main(String[] theArgs) throws Exception {

        ASDCCatalog catalog = new ASDCCatalog(new URI(theArgs[0]));

        Folder f = catalog.folder(theArgs[1]).withItems().withItemModels().execute().waitForResult();

        debugLogger.log(LogLevel.DEBUG, ASDCCatalog.class.getName(), "folder: {}", f.data());

        Resources items = f.elements(ITEMS, Resources.class);
        if (items != null) {
            for (Resource item : items) {
                executeItemsNodePropertiesAssignments(catalog, item);
            }
        }
    }

    private static void executeItemsNodePropertiesAssignments(ASDCCatalog catalog, Resource item) throws Exception {
        debugLogger.log(LogLevel.DEBUG, ASDCCatalog.class.getName(), "\titem: {} : {}",item.name(), item.data());
        Templates templates = item.elements(MODELS, Templates.class);
        if (templates != null) {
            for (Template t : templates) {
                Template ft = catalog.template(t.id()).withNodes().withNodeProperties()
                        .withNodePropertiesAssignments().execute().waitForResult();

                debugLogger.log(LogLevel.DEBUG, ASDCCatalog.class.getName(), "template data: {}", ft.data());
            }
        }
    }

}

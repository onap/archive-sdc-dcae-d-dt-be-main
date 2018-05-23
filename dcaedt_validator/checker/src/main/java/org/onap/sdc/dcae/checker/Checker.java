package org.onap.sdc.dcae.checker;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.io.File;
import java.io.Reader;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.Maps;
import com.google.common.collect.MapDifference;
import com.google.common.reflect.Invokable;

import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

import kwalify.Validator;
import kwalify.Rule;
import kwalify.Types;
import kwalify.ValidationException;
import kwalify.SchemaException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.onap.sdc.dcae.checker.annotations.Catalogs;
import org.onap.sdc.dcae.checker.annotations.Checks;
import org.reflections.Reflections;
import org.reflections.util.FilterBuilder;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner; 
import org.reflections.scanners.MethodAnnotationsScanner;

/*
 * To consider: model consistency checking happens now along with validation
 * (is implemented as part of the validation hooks). It might be better to
 * separate the 2 stages and perform all the consistency checking once 
 * validation is completed.
 */
public class Checker {
    private static final String PROPERTIES = "properties";
    private static final String DEFAULT = "default";
    private static final String ATTRIBUTES = "attributes";
    private static final String DATA_TYPES = "data_types";
    private static final String CAPABILITY_TYPES = "capability_types";
    private static final String VALID_SOURCE_TYPES = "valid_source_types";
    private static final String RELATIONSHIP_TYPES = "relationship_types";
    private static final String INTERFACES = "interfaces";
    private static final String VALID_TARGET_TYPES = "valid_target_types";
    private static final String ARTIFACT_TYPES = "artifact_types";
    private static final String INTERFACE_TYPES = "interface_types";
    private static final String NODE_TYPES = "node_types";
    private static final String REQUIREMENTS = "requirements";
    private static final String CAPABILITIES = "capabilities";
    private static final String GROUP_TYPES = "group_types";
    private static final String TARGETS_CONSTANT = "targets";
    private static final String POLICY_TYPES = "policy_types";
    private static final String IS_NONE_OF_THOSE = "' is none of those";
    private static final String INPUTS = "inputs";
    private static final String CAPABILITY = "capability";
    private static final String ARTIFACTS = "artifacts";
    private static final String WAS_DEFINED_FOR_THE_NODE_TYPE = " was defined for the node type ";
    private static final String UNKNOWN = "Unknown ";
    private static final String TYPE = " type ";
    public static final String IMPORTED_FROM = "',imported from ";

    private Target target = null; //what we're validating at the moment

    private Map<String, Target> grammars = new HashMap<>(); //grammars for the different tosca versions

    private Catalog catalog;
    private TargetLocator locator = new CommonLocator();

    private Table<String, Method, Object> checks = HashBasedTable.create();
    private Table<String, Method, Object> catalogs = HashBasedTable.create();

    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private static Catalog commonsCatalogInstance = null;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /* Need a proper way to indicate where the grammars are and how they should be identified */
    private static final String[] grammarFiles = new String[]{"tosca/tosca_simple_yaml_1_0.grammar",
            "tosca/tosca_simple_yaml_1_1.grammar"};

    private Pattern spacePattern = Pattern.compile("\\s");

    private Pattern indexPattern = Pattern.compile("/\\p{Digit}+");

    //this is getting silly ..
    private static Class[][] checkHookArgTypes =
            new Class[][]{
                    new Class[]{Map.class, CheckContext.class},
                    new Class[]{List.class, CheckContext.class}};

    private static Class[] validationHookArgTypes =
            new Class[]{Object.class, Rule.class, Validator.ValidationContext.class};

    public Checker() throws CheckerException {
        loadGrammars();
        loadAnnotations();
    }

    public static void main(String[] theArgs) {
        if (theArgs.length == 0) {
            errLogger.log(LogLevel.ERROR, Checker.class.getName(), "checker resource_to_validate [processor]*");
            return;
        }

        try {
            Catalog cat = Checker.check(new File(theArgs[0]));

            for (Target t : cat.targets()) {
                errLogger.log(LogLevel.ERROR, Checker.class.getName(), "{}\n{}\n{}", t.getLocation(), cat.importString(t), t.getReport());
            }

            for (Target t : cat.sortedTargets()) {
                errLogger.log(LogLevel.ERROR, Checker.class.getName(), t.toString());
            }

        } catch (Exception x) {
            errLogger.log(LogLevel.ERROR, Checker.class.getName(),"Exception {}", x);
        }
    }

    private void loadGrammars() throws CheckerException {

        for (String grammarFile : grammarFiles) {
            Target grammarTarget = this.locator.resolve(grammarFile);
            if (grammarTarget == null) {
                errLogger.log(LogLevel.WARN, this.getClass().getName(), "Failed to locate grammar {}", grammarFile);
                continue;
            }

            parseTarget(grammarTarget);
            if (grammarTarget.getReport().hasErrors()) {
                errLogger.log(LogLevel.WARN, this.getClass().getName(), "Invalid grammar {}: {}", grammarFile, grammarTarget.getReport().toString());
                continue;
            }

            List versions = null;
            try {
                versions = (List)
                        ((Map)
                                ((Map)
                                        ((Map) grammarTarget.getTarget())
                                                .get("mapping"))
                                        .get("tosca_definitions_version"))
                                .get("enum");
            } catch (Exception x) {
                errLogger.log(LogLevel.WARN, this.getClass().getName(), "Invalid grammar {}: cannot locate tosca_definitions_versions. Exception{}", grammarFile, x);
            }
            if (versions == null || versions.isEmpty()) {
                errLogger.log(LogLevel.WARN, this.getClass().getName(), "Invalid grammar {}: no tosca_definitions_versions specified", grammarFile);
                continue;
            }

            for (Object version : versions) {
                this.grammars.put(version.toString(), grammarTarget);
            }
        }

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Loaded grammars: {}", this.grammars);
    }

    private void loadAnnotations() {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackages("org.onap.sdc.dcae")
                        .filterInputsBy(new FilterBuilder()
                                        .include(".*\\.class")
                        )
                        .setScanners(new TypeAnnotationsScanner(),
                                new SubTypesScanner(),
                                new MethodAnnotationsScanner())
                        .setExpandSuperTypes(false)
        );

        Map<Class, Object> handlers = new HashMap<>();

        Set<Method> checkHandlers = reflections.getMethodsAnnotatedWith(Checks.class);
        for (Method checkHandler : checkHandlers) {
            checks.put(checkHandler.getAnnotation(Checks.class).path(),
                    checkHandler,
                    handlers.computeIfAbsent(checkHandler.getDeclaringClass(),
                            type -> {
                                try {
                                    return (getClass() == type) ? this
                                            : type.newInstance();
                                } catch (Exception x) {
                                    throw new RuntimeException(x);
                                }
                            }));
        }

        Set<Method> catalogHandlers = reflections.getMethodsAnnotatedWith(Catalogs.class);
        for (Method catalogHandler : catalogHandlers) {
            catalogs.put(catalogHandler.getAnnotation(Catalogs.class).path(),
                    catalogHandler,
                    handlers.computeIfAbsent(catalogHandler.getDeclaringClass(),
                            type -> {
                                try {
                                    return (getClass() == type) ? this
                                            : type.newInstance();
                                } catch (Exception x) {
                                    throw new RuntimeException(x);
                                }
                            }));
        }
    }


    public void setTargetLocator(TargetLocator theLocator) {
        this.locator = theLocator;
    }

    public Collection<Target> targets() {
        if (this.catalog == null) {
            throw new IllegalStateException("targets are only available after check");
        }

        return this.catalog.targets();
    }

    public Catalog catalog() {
        return this.catalog;
    }

    public void process(Processor theProcessor) {

        theProcessor.process(this.catalog);
    }

    /* a facility for handling all files in a target directory .. */
    public static Catalog check(File theSource)
            throws CheckerException {

        Catalog catalog = new Catalog(commonsCatalog());
        Checker checker = new Checker();
        try {
            if (theSource.isDirectory()) {
                for (File f : theSource.listFiles()) {
                    if (f.isFile()) {
                        checker.check(new Target(theSource.getCanonicalPath(), f.toURI().normalize()), catalog);
                    }
                }
            } else {
                checker.check(new Target(theSource.getCanonicalPath(), theSource.toURI().normalize()), catalog);
            }
        } catch (IOException iox) {
            throw new CheckerException("Failed to initialize target", iox);
        }

        return catalog;
    }

    public void check(String theSource)
            throws CheckerException {
        check(theSource, buildCatalog());
    }

    public void check(String theSource, Catalog theCatalog)
            throws CheckerException {
        Target tgt =
                this.locator.resolve(theSource);
        if (null == tgt) {
            throw new CheckerException("Unable to locate the target " + theSource);
        }

        check(tgt, theCatalog);
    }

    public void check(Target theTarget) throws CheckerException {
        check(theTarget, buildCatalog());
    }

    public void check(Target theTarget, Catalog theCatalog) throws CheckerException {

        this.catalog = theCatalog;
        this.locator.addSearchPath(theTarget.getLocation());

        if (this.catalog.addTarget(theTarget, null)) {
            List<Target> targets = parseTarget(theTarget);
            if (theTarget.getReport().hasErrors()) {
                return;
            }
            for (Target targetItr : targets) {
                this.catalog.addTarget(targetItr, null);
                if (!validateTarget(targetItr).getReport().hasErrors()) {
                    checkTarget(targetItr);
                }
            }
        }
    }

    public void validate(Target theTarget) throws CheckerException {
        validate(theTarget, buildCatalog());
    }

    public void validate(Target theTarget, Catalog theCatalog) throws CheckerException {
        this.catalog = theCatalog;
        this.locator.addSearchPath(theTarget.getLocation());

        if (this.catalog.addTarget(theTarget, null)) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "@validateTarget");
            if (!validateTarget(theTarget).getReport().hasErrors()) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "@checkTarget");
                checkTarget(theTarget);
            }
        }
    }

    private List<Target> parseTarget(final Target theTarget)
            throws CheckerException {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "parseTarget {}", theTarget);

        Reader source = null;
        try {
            source = theTarget.open();
        } catch (IOException iox) {
            throw new CheckerException("Failed to open target " + theTarget, iox);
        }


        ArrayList<Object> yamlRoots = new ArrayList<>();
        try {
            Yaml yaml = new Yaml();
            for (Object yamlRoot : yaml.loadAll(source)) {
                yamlRoots.add(yamlRoot);
            }


        } catch (Exception x) {
            theTarget.report(x);
            return Collections.emptyList();
        } finally {
            try {
                source.close();
            } catch (IOException iox) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "Exception {}", iox);
            }
        }

        ArrayList targets = new ArrayList(yamlRoots.size());
        if (yamlRoots.size() == 1) {
            //he target turned out to be a bare document
            theTarget.setTarget(yamlRoots.get(0));
            targets.add(theTarget);
        } else {
            //the target turned out to be a stream containing multiple documents
            for (int i = 0; i < yamlRoots.size(); i++) {
/*
!!We're changing the target below, i.e. we're changing the target implementation hence caching implementation will suffer!!
*/
                Target newTarget = new Target(theTarget.getName(),
                        fragmentTargetURI(theTarget.getLocation(), String.valueOf(i)));
                newTarget.setTarget(yamlRoots.get(i));
                targets.add(newTarget);
            }
        }

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), " exiting parseTarget {}", theTarget);
        return targets;
    }

    private URI fragmentTargetURI(URI theRoot, String theFragment) {
        try {
            return new URI(theRoot.getScheme(),
                    theRoot.getSchemeSpecificPart(),
                    theFragment);
        } catch (URISyntaxException urisx) {
            throw new RuntimeException(urisx);
        }
    }

    private Target validateTarget(Target theTarget)
            throws CheckerException {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "entering validateTarget {}", theTarget);

        String version = (String)
                ((Map) theTarget.getTarget())
                        .get("tosca_definitions_version");
        if (version == null) {
            throw new CheckerException("Target " + theTarget + " does not specify a tosca_definitions_version");
        }

        Target grammar = this.grammars.get(version);
        if (grammar == null) {
            throw new CheckerException("Target " + theTarget + " specifies unknown tosca_definitions_version " + version);
        }

        TOSCAValidator validator = null;
        try {
            validator = new TOSCAValidator(theTarget, grammar.getTarget());
        } catch (SchemaException sx) {
            throw new CheckerException("Grammar error at: " + sx.getPath(), sx);
        }

        theTarget.getReport().addAll(
                validator.validate(theTarget.getTarget()));

        if (!theTarget.getReport().hasErrors()) {
            applyCanonicals(theTarget.getTarget(), validator.canonicals);
        }

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), " exiting validateTarget {}", theTarget);
        return theTarget;
    }

    private Target checkTarget(Target theTarget) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "entering checkTarget {}", theTarget);

        CheckContext ctx = new CheckContext(theTarget);
        //start at the top
        checkServiceTemplateDefinition(
                (Map<String, Object>) theTarget.getTarget(), ctx);

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "exiting checkTarget {}", theTarget);
        return theTarget;
    }

    public void checkProperties(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(PROPERTIES);
        try {
            if (!checkDefinition(PROPERTIES, theDefinitions, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkPropertyDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkPropertyDefinition(
            String theName, Map theDefinition, CheckContext theContext) {
        theContext.enter(theName);
        if (!checkDefinition(theName, theDefinition, theContext)) {
            return;
        }
        //check the type
        if (!checkDataType(theDefinition, theContext)) {
            return;
        }
        //check default value is compatible with type
        Object defaultValue = theDefinition.get(DEFAULT);
        if (defaultValue != null) {
            checkDataValuation(defaultValue, theDefinition, theContext);
        }

        theContext.exit();
    }

    private void checkAttributes(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(ATTRIBUTES);
        try {
            if (!checkDefinition(ATTRIBUTES, theDefinitions, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkAttributeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkAttributeDefinition(
            String theName, Map theDefinition, CheckContext theContext) {
        theContext.enter(theName);
        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }
            if (!checkDataType(theDefinition, theContext)) {
                return;
            }
        } finally {
            theContext.exit();
        }
    }

    /* top level rule, we collected the whole information set.
     * this is where checking starts
     */
    private void checkServiceTemplateDefinition(
            Map<String, Object> theDef, CheckContext theContext) {
        theContext.enter("");

        if (theDef == null) {
            theContext.addError("Empty template", null);
            return;
        }

//!!! imports need to be processed first now that catalogging takes place at check time!! 

        //first catalog whatever it is there to be cataloged so that the checks can perform cross-checking
        for (Iterator<Map.Entry<String, Object>> ri = theDef.entrySet().iterator();
             ri.hasNext(); ) {
            Map.Entry<String, Object> e = ri.next();
            catalogs(e.getKey(), e.getValue(), theContext);
        }

        for (Iterator<Map.Entry<String, Object>> ri = theDef.entrySet().iterator();
             ri.hasNext(); ) {
            Map.Entry<String, Object> e = ri.next();
            checks(e.getKey(), e.getValue(), theContext);
        }
        theContext.exit();
    }

    @Catalogs(path = "/data_types")
    protected void catalog_data_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(DATA_TYPES);
        try {
            catalogTypes(Construct.Data, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/data_types")
    protected void check_data_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(DATA_TYPES);

        try {
            if (!checkDefinition(DATA_TYPES, theDefinitions, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkDataTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkDataTypeDefinition(String theName,
                                         Map theDefinition,
                                         CheckContext theContext) {
        theContext.enter(theName, Construct.Data);
        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
                checkTypeConstructFacet(Construct.Data, theName, theDefinition,
                        Facet.properties, theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    @Catalogs(path = "/capability_types")
    protected void catalog_capability_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(CAPABILITY_TYPES);
        try {
            catalogTypes(Construct.Capability, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/capability_types")
    protected void check_capability_types(
            Map<String, Map> theTypes, CheckContext theContext) {
        theContext.enter(CAPABILITY_TYPES);
        try {
            if (!checkDefinition(CAPABILITY_TYPES, theTypes, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theTypes.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkCapabilityTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkCapabilityTypeDefinition(String theName,
                                               Map theDefinition,
                                               CheckContext theContext) {
        theContext.enter(theName, Construct.Capability);

        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
                checkTypeConstructFacet(Construct.Capability, theName, theDefinition,
                        Facet.properties, theContext);
            }

            if (theDefinition.containsKey(ATTRIBUTES)) {
                checkAttributes(
                        (Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext);
                checkTypeConstructFacet(Construct.Capability, theName, theDefinition,
                        Facet.attributes, theContext);
            }

            //valid_source_types: see capability_type_definition
            //unclear: how is the valid_source_types list definition eveolving across
            //the type hierarchy: additive, overwriting, ??
            if (theDefinition.containsKey(VALID_SOURCE_TYPES)) {
                checkTypeReference(Construct.Node, theContext,
                        ((List<String>) theDefinition.get(VALID_SOURCE_TYPES)).toArray(EMPTY_STRING_ARRAY));
            }
        } finally {
            theContext.exit();
        }
    }

    @Catalogs(path = "/relationship_types")
    protected void catalog_relationship_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(RELATIONSHIP_TYPES);
        try {
            catalogTypes(Construct.Relationship, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/relationship_types")
    protected void check_relationship_types(
            Map<String, Map> theDefinition, CheckContext theContext) {
        theContext.enter(RELATIONSHIP_TYPES);
        try {
            if (!checkDefinition(RELATIONSHIP_TYPES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkRelationshipTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkRelationshipTypeDefinition(String theName,
                                                 Map theDefinition,
                                                 CheckContext theContext) {
        theContext.enter(theName, Construct.Relationship);
        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
                checkTypeConstructFacet(Construct.Relationship, theName, theDefinition,
                        Facet.properties, theContext);
            }

            if (theDefinition.containsKey(ATTRIBUTES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext);
                checkTypeConstructFacet(Construct.Relationship, theName, theDefinition,
                        Facet.attributes, theContext);
            }

            Map<String, Map> interfaces = (Map<String, Map>) theDefinition.get(INTERFACES);
            if (interfaces != null) {
                theContext.enter(INTERFACES);
                for (Iterator<Map.Entry<String, Map>> i =
                     interfaces.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<String, Map> e = i.next();
                    check_type_interface_definition(
                            e.getKey(), e.getValue(), theContext);
                }
                theContext.exit();
            }

            if (theDefinition.containsKey(VALID_TARGET_TYPES)) {
                checkTypeReference(Construct.Capability, theContext,
                        ((List<String>) theDefinition.get(VALID_TARGET_TYPES)).toArray(EMPTY_STRING_ARRAY));
            }
        } finally {
            theContext.exit();
        }
    }

    @Catalogs(path = "/artifact_types")
    protected void catalog_artifact_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(ARTIFACT_TYPES);
        try {
            catalogTypes(Construct.Artifact, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/artifact_types")
    protected void check_artifact_types(
            Map<String, Map> theDefinition, CheckContext theContext) {
        theContext.enter(ARTIFACT_TYPES);
        try {
            if (!checkDefinition(ARTIFACT_TYPES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkArtifactTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkArtifactTypeDefinition(String theName,
                                             Map theDefinition,
                                             CheckContext theContext) {
        theContext.enter(theName, Construct.Artifact);
        try {
            checkDefinition(theName, theDefinition, theContext);
        } finally {
            theContext.exit();
        }
    }

    @Catalogs(path = "/interface_types")
    protected void catalog_interface_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(INTERFACE_TYPES);
        try {
            catalogTypes(Construct.Interface, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/interface_types")
    protected void check_interface_types(
            Map<String, Map> theDefinition, CheckContext theContext) {
        theContext.enter(INTERFACE_TYPES);
        try {
            if (!checkDefinition(INTERFACE_TYPES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkInterfaceTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkInterfaceTypeDefinition(String theName,
                                              Map theDefinition,
                                              CheckContext theContext) {
        theContext.enter(theName, Construct.Interface);
        try {
            checkDefinition(theName, theDefinition, theContext);
        } finally {
            theContext.exit();
        }
    }

    @Catalogs(path = "/node_types")
    protected void catalog_node_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(NODE_TYPES);
        try {
            catalogTypes(Construct.Node, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/node_types")
    protected void check_node_types(
            Map<String, Map> theDefinition, CheckContext theContext) {
        theContext.enter(NODE_TYPES);
        try {
            if (!checkDefinition(NODE_TYPES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkNodeTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkNodeTypeDefinition(String theName,
                                         Map theDefinition,
                                         CheckContext theContext) {
        theContext.enter(theName, Construct.Node);

        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
                checkTypeConstructFacet(Construct.Node, theName, theDefinition,
                        Facet.properties, theContext);
            }

            if (theDefinition.containsKey(ATTRIBUTES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext);
                checkTypeConstructFacet(Construct.Node, theName, theDefinition,
                        Facet.attributes, theContext);
            }

            //requirements
            if (theDefinition.containsKey(REQUIREMENTS)) {
                check_requirements(
                        (List<Map>) theDefinition.get(REQUIREMENTS), theContext);
            }

            //capabilities
            if (theDefinition.containsKey(CAPABILITIES)) {
                check_capabilities(
                        (Map<String, Map>) theDefinition.get(CAPABILITIES), theContext);
            }

            //interfaces:
            Map<String, Map> interfaces =
                    (Map<String, Map>) theDefinition.get(INTERFACES);
            checkMapTypeInterfaceDefinition(theContext, interfaces);
        } finally {
            theContext.exit();
        }
    }

    private void checkMapTypeInterfaceDefinition(CheckContext theContext, Map<String, Map> interfaces) {
        if (interfaces != null) {
            try {
                theContext.enter(INTERFACES);
                for (Iterator<Map.Entry<String, Map>> i =
                     interfaces.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<String, Map> e = i.next();
                    check_type_interface_definition(
                            e.getKey(), e.getValue(), theContext);
                }
            } finally {
                theContext.exit();
            }
        }
    }

    @Catalogs(path = "/group_types")
    protected void catalog_group_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(GROUP_TYPES);
        try {
            catalogTypes(Construct.Group, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/group_types")
    protected void check_group_types(
            Map<String, Map> theDefinition, CheckContext theContext) {
        theContext.enter(GROUP_TYPES);
        try {
            if (!checkDefinition(GROUP_TYPES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkGroupTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkGroupTypeDefinition(String theName,
                                          Map theDefinition,
                                          CheckContext theContext) {
        theContext.enter(theName, Construct.Group);

        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
                checkTypeConstructFacet(Construct.Group, theName, theDefinition,
                        Facet.properties, theContext);
            }

            if (theDefinition.containsKey(TARGETS_CONSTANT)) {
                checkTypeReference(Construct.Node, theContext,
                        ((List<String>) theDefinition.get(TARGETS_CONSTANT)).toArray(EMPTY_STRING_ARRAY));
            }

            //interfaces
            Map<String, Map> interfaces =
                    (Map<String, Map>) theDefinition.get(INTERFACES);
            checkMapTypeInterfaceDefinition(theContext, interfaces);

        } finally {
            theContext.exit();
        }
    }

    @Catalogs(path = "/policy_types")
    protected void catalog_policy_types(
            Map<String, Map> theDefinitions, CheckContext theContext) {
        theContext.enter(POLICY_TYPES);
        try {
            catalogTypes(Construct.Policy, theDefinitions, theContext);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/policy_types")
    protected void check_policy_types(
            Map<String, Map> theDefinition, CheckContext theContext) {
        theContext.enter(POLICY_TYPES);
        try {
            if (!checkDefinition(POLICY_TYPES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkPolicyTypeDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkPolicyTypeDefinition(String theName,
                                           Map theDefinition,
                                           CheckContext theContext) {
        theContext.enter(theName, Construct.Policy);

        try {
            if (!checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            if (theDefinition.containsKey(PROPERTIES)) {
                checkProperties(
                        (Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
                checkTypeConstructFacet(Construct.Policy, theName, theDefinition,
                        Facet.properties, theContext);
            }

            //the targets can be known node types or group types
            List<String> targets = (List<String>) theDefinition.get(TARGETS_CONSTANT);
            if ((targets != null) && (checkDefinition(TARGETS_CONSTANT, targets, theContext))) {
                for (String targetItr : targets) {
                    if (!(this.catalog.hasType(Construct.Node, targetItr) ||
                            this.catalog.hasType(Construct.Group, targetItr))) {
                        theContext.addError("The 'targets' entry must contain a reference to a node type or group type, '" + target + IS_NONE_OF_THOSE, null);
                    }
                }
            }
        } finally {
            theContext.exit();
        }
    }

    //checking of actual constructs (capability, ..)

    /* First, interface types do not have a hierarchical organization (no
     * 'derived_from' in a interface type definition).
     * So, when interfaces (with a certain type) are defined in a node
     * or relationship type (and they can define new? operations), what
     * is there to check:
     * 	Can operations here re-define their declaration from the interface
     * type spec?? From A.5.11.3 we are to understand indicates override to be
     * the default interpretation .. but they talk about sub-classing so it
     * probably intended as a reference to the node or relationship type
     * hierarchy and not the interface type (no hierarchy there).
     *	Or is this a a case of augmentation where new operations can be added??
     */
    private void check_type_interface_definition(
            String theName, Map theDef, CheckContext theContext) {
        theContext.enter(theName);
        try {
            if (!checkDefinition(theName, theDef, theContext)) {
                return;
            }

            if (!checkType(Construct.Interface, theDef, theContext)) {
                return;
            }

            if (theDef.containsKey(INPUTS)) {
                check_inputs((Map<String, Map>) theDef.get(INPUTS), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void check_capabilities(Map<String, Map> theDefinition,
                                   CheckContext theContext) {
        theContext.enter(CAPABILITIES);
        try {
            if (!checkDefinition(CAPABILITIES, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkCapabilityDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    /* A capability definition appears within the context ot a node type */
    private void checkCapabilityDefinition(String theName,
                                           Map theDef,
                                           CheckContext theContext) {
        theContext.enter(theName, Construct.Capability);

        try {
            if (!checkDefinition(theName, theDef, theContext)) {
                return;
            }

            //check capability type
            if (!checkType(Construct.Capability, theDef, theContext)) {
                return;
            }

            //check properties
            if (!checkFacetAugmentation(
                    Construct.Capability, theDef, Facet.properties, theContext)) {
                return;
            }

            //check attributes
            if (!checkFacetAugmentation(
                    Construct.Capability, theDef, Facet.attributes, theContext)) {
                return;
            }

            //valid_source_types: should point to valid template nodes
            if (theDef.containsKey(VALID_SOURCE_TYPES)) {
                checkTypeReference(Construct.Node, theContext,
                        ((List<String>) theDef.get(VALID_SOURCE_TYPES)).toArray(EMPTY_STRING_ARRAY));
                //per A.6.1.4 there is an additinal check to be performed here:
                //"Any Node Type (names) provides as values for the valid_source_types keyname SHALL be type-compatible (i.e., derived from the same parent Node Type) with any Node Types defined using the same keyname in the parent Capability Type."
            }
            //occurences: were verified in range_definition

        } finally {
            theContext.exit();
        }
    }

    private void check_requirements(List<Map> theDefinition,
                                   CheckContext theContext) {
        theContext.enter(REQUIREMENTS);
        try {
            if (!checkDefinition(REQUIREMENTS, theDefinition, theContext)) {
                return;
            }

            for (Iterator<Map> i = theDefinition.iterator(); i.hasNext(); ) {
                Map e = i.next();
                Iterator<Map.Entry<String, Map>> ei =
                        (Iterator<Map.Entry<String, Map>>) e.entrySet().iterator();
                Map.Entry<String, Map> eie = ei.next();
                checkRequirementDefinition(eie.getKey(), eie.getValue(), theContext);
                assert !ei.hasNext();
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkRequirementDefinition(String theName,
                                            Map theDef,
                                            CheckContext theContext) {
        theContext.enter(theName, Construct.Requirement);

        try {
            if (!checkDefinition(theName, theDef, theContext)) {
                return;
            }
            //check capability type
            String capabilityType = (String) theDef.get(CAPABILITY);
            if (null != capabilityType) {
                checkTypeReference(Construct.Capability, theContext, capabilityType);
            }

            //check node type
            String nodeType = (String) theDef.get("node");
            if (null != nodeType) {
                checkTypeReference(Construct.Node, theContext, nodeType);
            }

            //check relationship type
            Map relationshipSpec = (Map) theDef.get("relationship");
            String relationshipType = null;
            if (null != relationshipSpec) {
                relationshipType = (String) relationshipSpec.get("type");
                if (relationshipType != null) { //should always be the case
                    checkTypeReference(Construct.Relationship, theContext, relationshipType);
                }

                Map<String, Map> interfaces = (Map<String, Map>)
                        relationshipSpec.get(INTERFACES);
                if (interfaces != null) {
                    //augmentation (additional properties or operations) of the interfaces
                    //defined by the above relationship types

                    //check that the interface types are known
                    for (Map interfaceDef : interfaces.values()) {
                        checkType(Construct.Interface, interfaceDef, theContext);
                    }
                }
            }

            //cross checks

            //the capability definition might come from the capability type or from the capability definition
            //within the node type. We might have more than one as a node might specify multiple capabilities of the
            //same type.
            //the goal here is to cross check the compatibility of the valid_source_types specification in the
            //target capability definition (if that definition contains a valid_source_types entry).
            List<Map> capabilityDefs = new LinkedList<>();
            //nodeType exposes capabilityType
            if (nodeType != null) {
                Map<String, Map> capabilities =
                        findTypeFacetByType(Construct.Node, nodeType,
                                Facet.capabilities, capabilityType);
                if (capabilities.isEmpty()) {
                    theContext.addError("The node type " + nodeType + " does not appear to expose a capability of a type compatible with " + capabilityType, null);
                } else {
                    for (Map.Entry<String, Map> capability : capabilities.entrySet()) {
                        //this is the capability as it was defined in the node type
                        Map capabilityDef = capability.getValue();
                        //if it defines a valid_source_types then we're working with it,
                        //otherwise we're working with the capability type it points to.
                        //The spec does not make it clear if the valid_source_types in a capability definition augments or
                        //overwrites the one from the capabilityType (it just says they must be compatible).
                        if (capabilityDef.containsKey(VALID_SOURCE_TYPES)) {
                            capabilityDefs.add(capabilityDef);
                        } else {
                            capabilityDef =
                                    catalog.getTypeDefinition(Construct.Capability, (String) capabilityDef.get("type"));
                            if (capabilityDef.containsKey(VALID_SOURCE_TYPES)) {
                                capabilityDefs.add(capabilityDef);
                            } else {
                                //!!if there is a capability that does not have a valid_source_type than there is no reason to
                                //make any further verification (as there is a valid node_type/capability target for this requirement)
                                capabilityDefs.clear();
                                break;
                            }
                        }
                    }
                }
            } else {
                Map capabilityDef = catalog.getTypeDefinition(Construct.Capability, capabilityType);
                if (capabilityDef.containsKey(VALID_SOURCE_TYPES)) {
                    capabilityDefs.add(capabilityDef);
                }
            }

            //check that the node type enclosing this requirement definition
            //is in the list of valid_source_types
            if (!capabilityDefs.isEmpty()) {
                String enclosingNodeType =
                        theContext.enclosingConstruct(Construct.Node);
                assert enclosingNodeType != null;

                if (!capabilityDefs.stream().anyMatch(
                        (Map capabilityDef) -> {
                            List<String> valid_source_types =
                                    (List<String>) capabilityDef.get(VALID_SOURCE_TYPES);
                            return valid_source_types.stream().anyMatch(
                                    (String source_type) -> catalog.isDerivedFrom(
                                            Construct.Node, enclosingNodeType, source_type));
                        })) {
                    theContext.addError("Node type: " + enclosingNodeType + " not compatible with any of the valid_source_types provided in the definition of compatible capabilities", null);
                }
            }

            //if we have a relationship type, check if it has a valid_target_types
            //if it does, make sure that the capability type is compatible with one
            //of them
            if (relationshipType != null) { //should always be the case
                Map relationshipTypeDef = catalog.getTypeDefinition(
                        Construct.Relationship, relationshipType);
                if (relationshipTypeDef != null) {
                    List<String> valid_target_types =
                            (List<String>) relationshipTypeDef.get(VALID_TARGET_TYPES);
                    if (valid_target_types != null) {
                        boolean found = false;
                        for (String target_type : valid_target_types) {
                            if (catalog.isDerivedFrom(
                                    Construct.Capability, capabilityType, target_type)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            theContext.addError("Capability type: " + capabilityType + " not compatible with any of the valid_target_types " + valid_target_types + " provided in the definition of relationship type " + relationshipType, null);
                        }
                    }
                }
            }

            //relationship declares the capabilityType in its valid_target_type set
            //in A.6.9 'Relationship Type' the spec does not indicate how	inheritance
            //is to be applied to the valid_target_type spec: cumulative, overwrites,
            //so we treat it as an overwrite.
        } finally {
            theContext.exit();
        }
    }

    //topology_template_definition and sub-rules
    /* */
    @Checks(path = "/topology_template")
    protected void check_topology_template(
            Map theDef, CheckContext theContext) {

        theContext.enter("topology_template");

        for (Iterator<Map.Entry<String, Object>> ri = theDef.entrySet().iterator();
             ri.hasNext(); ) {
            Map.Entry<String, Object> e = ri.next();
            checks(e.getKey(), e.getValue(), theContext);
        }
        theContext.exit();
    }

    /*
     * Once the syntax of the imports section is validated parse/validate/catalog    * all the imported template information
     */
    @Checks(path = "/imports")
    protected void check_imports(List theImports, CheckContext theContext) {
        theContext.enter("imports");

        for (ListIterator li = theImports.listIterator(); li.hasNext(); ) {
            Object importEntry = li.next();
            Object importFile = ((Map) mapEntry(importEntry).getValue()).get("file");
            Target tgt = null;
            try {
                tgt = catalog.getTarget((URI) importFile);
            } catch (ClassCastException ccx) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Import is {}. Exception {}", importFile, ccx);
            }

            if (tgt == null || tgt.getReport().hasErrors()) {
                //import failed parsing or validation, we skip it
                continue;
            }

            //import should have been fully processed by now ???
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "Processing import {}.", tgt);
            checkTarget(tgt);

        }
        theContext.exit();
    }

    /* */
    @Checks(path = "/topology_template/substitution_mappings")
    protected void check_substitution_mappings(Map<String, Object> theSub,
                                               CheckContext theContext) {
        theContext.enter("substitution_mappings");
        try {
            //type is mandatory
            String type = (String) theSub.get("node_type");
            if (!checkTypeReference(Construct.Node, theContext, type)) {
                theContext.addError("Unknown node type: " + type + "", null);
                return; //not much to go on with
            }

            Map<String, List> capabilities = (Map<String, List>) theSub.get(CAPABILITIES);
            if (null != capabilities) {
                for (Map.Entry<String, List> ce : capabilities.entrySet()) {
                    //the key must be a capability of the type
                    if (null == findTypeFacetByName(Construct.Node, type,
                            Facet.capabilities, ce.getKey())) {
                        theContext.addError("Unknown node type capability: " + ce.getKey() + ", type " + type, null);
                    }
                    //the value is a 2 element list: first is a local node,
                    //second is the name of one of its capabilities
                    List targetList = ce.getValue();
                    if (targetList.size() != 2) {
                        theContext.addError("Invalid capability mapping: " + target + ", expecting 2 elements", null);
                        continue;
                    }

                    String targetNode = (String) targetList.get(0);
                    String targetCapability = (String) targetList.get(1);

                    Map<String, Object> targetNodeDef = (Map<String, Object>)
                            this.catalog.getTemplate(theContext.target(), Construct.Node, targetNode);
                    if (null == targetNodeDef) {
                        theContext.addError("Invalid capability mapping node template: " + targetNode, null);
                        continue;
                    }

                    String targetNodeType = (String) targetNodeDef.get("type");
                    if (null == findTypeFacetByName(Construct.Node, targetNodeType,
                            Facet.capabilities, targetCapability)) {
                        theContext.addError("Invalid capability mapping capability: " + targetCapability + ". No such capability found for node template " + targetNode + ", of type " + targetNodeType, null);
                    }
                }
            }

            Map<String, List> requirements = (Map<String, List>) theSub.get(REQUIREMENTS);
            if (null != requirements) {
                for (Map.Entry<String, List> re : requirements.entrySet()) {
                    //the key must be a requirement of the type
                    if (null == findNodeTypeRequirementByName(type, re.getKey())) {
                        theContext.addError("Unknown node type requirement: " + re.getKey() + ", type " + type, null);
                    }

                    List targetList = re.getValue();
                    if (targetList.size() != 2) {
                        theContext.addError("Invalid requirement mapping: " + targetList + ", expecting 2 elements", null);
                        continue;
                    }

                    String targetNode = (String) targetList.get(0);
                    String targetRequirement = (String) targetList.get(1);

                    Map<String, Object> targetNodeDef = (Map<String, Object>)
                            this.catalog.getTemplate(theContext.target(), Construct.Node, targetNode);
                    if (null == targetNodeDef) {
                        theContext.addError("Invalid requirement mapping node template: " + targetNode, null);
                        continue;
                    }

                    String targetNodeType = (String) targetNodeDef.get("type");
                    if (null == findNodeTypeRequirementByName(targetNodeType, targetRequirement)) {
                        theContext.addError("Invalid requirement mapping requirement: " + targetRequirement + ". No such requirement found for node template " + targetNode + ", of type " + targetNodeType, null);
                    }
                }
            }
        } finally {
            theContext.exit();
        }
    }


    /* */
    @Checks(path = "/topology_template/inputs")
    protected void check_inputs(Map<String, Map> theInputs,
                                CheckContext theContext) {
        theContext.enter(INPUTS);

        try {
            if (!checkDefinition(INPUTS, theInputs, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theInputs.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkInputDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkInputDefinition(String theName,
                                      Map theDef,
                                      CheckContext theContext) {
        theContext.enter(theName);
        try {
            if (!checkDefinition(theName, theDef, theContext)) {
                return;
            }
            //
            if (!checkDataType(theDef, theContext)) {
                return;
            }
            //check default value
            Object defaultValue = theDef.get(DEFAULT);
            if (defaultValue != null) {
                checkDataValuation(defaultValue, theDef, theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "topology_template/outputs")
    protected void check_outputs(Map<String, Map> theOutputs,
                                 CheckContext theContext) {
        theContext.enter("outputs");

        try {
            if (!checkDefinition("outputs", theOutputs, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theOutputs.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkOutputDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkOutputDefinition(String theName,
                                       Map theDef,
                                       CheckContext theContext) {
        theContext.enter(theName);
        try {
            checkDefinition(theName, theDef, theContext);
            //check the expression
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/topology_template/groups")
    protected void check_groups(Map<String, Map> theGroups,
                                CheckContext theContext) {
        theContext.enter("groups");

        try {
            if (!checkDefinition("groups", theGroups, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theGroups.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkGroupDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkGroupDefinition(String theName,
                                      Map theDef,
                                      CheckContext theContext) {
        theContext.enter(theName);
        try {
            if (!checkDefinition(theName, theDef, theContext)) {
                return;
            }

            if (!checkType(Construct.Group, theDef, theContext)) {
                return;
            }

            if (!checkFacet(
                    Construct.Group, theDef, Facet.properties, theContext)) {
                return;
            }

            if (theDef.containsKey(TARGETS_CONSTANT)) {

                List<String> targetsTypes = (List<String>)
                        this.catalog.getTypeDefinition(Construct.Group,
                                (String) theDef.get("type"))
                                .get(TARGETS_CONSTANT);

                List<String> targets = (List<String>) theDef.get(TARGETS_CONSTANT);
                for (String targetItr : targets) {
                    if (!this.catalog.hasTemplate(theContext.target(), Construct.Node, targetItr)) {
                        theContext.addError("The 'targets' entry must contain a reference to a node template, '" + targetItr + "' is not one", null);
                    } else {
                        if (targetsTypes != null) {
                            String targetType = (String)
                                    this.catalog.getTemplate(theContext.target(), Construct.Node, targetItr).get("type");

                            boolean found = false;
                            for (String type : targetsTypes) {
                                found = this.catalog
                                        .isDerivedFrom(Construct.Node, targetType, type);
                                if (found) {
                                    break;
                                }
                            }

                            if (!found) {
                                theContext.addError("The 'targets' entry '" + targetItr + "' is not type compatible with any of types specified in policy type targets", null);
                            }
                        }
                    }
                }
            }
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/topology_template/policies")
    protected void check_policies(List<Map<String, Map>> thePolicies,
                                  CheckContext theContext) {
        theContext.enter("policies");

        try {
            if (!checkDefinition("policies", thePolicies, theContext)) {
                return;
            }

            for (Map<String, Map> policy : thePolicies) {
                assert policy.size() == 1;
                Map.Entry<String, Map> e = policy.entrySet().iterator().next();
                checkPolicyDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkPolicyDefinition(String theName,
                                       Map theDef,
                                       CheckContext theContext) {
        theContext.enter(theName);
        try {
            if (!checkDefinition(theName, theDef, theContext)) {
                return;
            }

            if (!checkType(Construct.Policy, theDef, theContext)) {
                return;
            }

            if (!checkFacet(
                    Construct.Policy, theDef, Facet.properties, theContext)) {
                return;
            }

            //targets: must point to node or group templates (that are of a type
            //specified in the policy type definition, if targets were specified
            //there).
            if (theDef.containsKey(TARGETS_CONSTANT)) {
                List<String> targetsTypes = (List<String>)
                        this.catalog.getTypeDefinition(Construct.Policy,
                                (String) theDef.get("type"))
                                .get(TARGETS_CONSTANT);

                List<String> targets = (List<String>) theDef.get(TARGETS_CONSTANT);
                for (String targetItr : targets) {
                    Construct targetConstruct = null;

                    if (this.catalog.hasTemplate(theContext.target(), Construct.Group, targetItr)) {
                        targetConstruct = Construct.Group;
                    } else if (this.catalog.hasTemplate(theContext.target(), Construct.Node, targetItr)) {
                        targetConstruct = Construct.Node;
                    } else {
                        theContext.addError("The 'targets' entry must contain a reference to a node template or group template, '" + target + IS_NONE_OF_THOSE, null);
                    }

                    if (targetConstruct != null &&
                            targetsTypes != null) {
                        //get the target type and make sure is compatible with the types
                        //indicated in the type spec
                        String targetType = (String)
                                this.catalog.getTemplate(theContext.target(), targetConstruct, targetItr).get("type");

                        boolean found = false;
                        for (String type : targetsTypes) {
                            found = this.catalog
                                    .isDerivedFrom(targetConstruct, targetType, type);
                            if (found) {
                                break;
                            }
                        }

                        if (!found) {
                            theContext.addError("The 'targets' " + targetConstruct + " entry '" + targetItr + "' is not type compatible with any of types specified in policy type targets", null);
                        }
                    }
                }
            }

        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/topology_template/node_templates")
    protected void check_node_templates(Map<String, Map> theTemplates,
                                        CheckContext theContext) {
        theContext.enter("node_templates");
        try {
            if (!checkDefinition("node_templates", theTemplates, theContext)) {
                return;
            }

            for (Iterator<Map.Entry<String, Map>> i = theTemplates.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, Map> e = i.next();
                checkNodeTemplateDefinition(e.getKey(), e.getValue(), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    /* */
    private void checkNodeTemplateDefinition(String theName,
                                             Map theNode,
                                             CheckContext theContext) {
        theContext.enter(theName, Construct.Node);

        try {
            if (!checkDefinition(theName, theNode, theContext)) {
                return;
            }

            if (!checkType(Construct.Node, theNode, theContext)) {
                return;
            }

            //copy
            String copy = (String) theNode.get("copy");
            if (copy != null) {
                if (!checkTemplateReference(Construct.Node, theContext, copy)) {
                    theContext.addError("The 'copy' reference " + copy + " does not point to a known node template", null);
                } else {
                    //the 'copy' node specification should be used to provide 'defaults'
                    //for this specification
                }
            }

      /* check that we operate on properties and attributes within the scope of
        the specified node type */
            if (!checkFacet(
                    Construct.Node, /*theName,*/theNode, Facet.properties, theContext)) {
                return;
            }

            if (!checkFacet(
                    Construct.Node, /*theName,*/theNode, Facet.attributes, theContext)) {
                return;
            }

            //requirement assignment seq
            if (theNode.containsKey(REQUIREMENTS)) {
                checkRequirementsAssignmentDefinition(
                        (List<Map>) theNode.get(REQUIREMENTS), theContext);
            }

            //capability assignment map: subject to augmentation
            if (theNode.containsKey(CAPABILITIES)) {
                checkCapabilitiesAssignmentDefinition(
                        (Map<String, Map>) theNode.get(CAPABILITIES), theContext);
            }

            //interfaces
            if (theNode.containsKey(INTERFACES)) {
                checkTemplateInterfacesDefinition(
                        (Map<String, Map>) theNode.get(INTERFACES), theContext);
            }

            //artifacts: artifacts do not have different definition forms/syntax
            //depending on the context (type or template) but they are still subject
            //to 'augmentation'
            if (theNode.containsKey(ARTIFACTS)) {
                check_template_artifacts_definition(
                        (Map<String, Object>) theNode.get(ARTIFACTS), theContext);
            }

            /* node_filter: the context to which the node filter is applied is very
             * wide here as opposed to the node filter specification in a requirement
             * assignment which has a more strict context (target node/capability are
             * specified).
             * We could check that there are nodes in this template having the
             * properties/capabilities specified in this filter, i.e. the filter has
             * a chance to succeed.
             */
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/topology_template/relationship_templates")
    protected void check_relationship_templates(Map theTemplates,
                                                CheckContext theContext) {
        theContext.enter("relationship_templates");

        for (Iterator<Map.Entry<String, Map>> i = theTemplates.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<String, Map> e = i.next();
            checkRelationshipTemplateDefinition(e.getKey(), e.getValue(), theContext);
        }
        theContext.exit();
    }

    private void checkRelationshipTemplateDefinition(
            String theName,
            Map theRelationship,
            CheckContext theContext) {
        theContext.enter(theName, Construct.Relationship);
        try {
            if (!checkDefinition(theName, theRelationship, theContext)) {
                return;
            }

            if (!checkType(Construct.Relationship, theRelationship, theContext)) {
                return;
            }

      /* check that we operate on properties and attributes within the scope of
        the specified relationship type */
            if (!checkFacet(Construct.Relationship, theRelationship,
                    Facet.properties, theContext)) {
                return;
            }

            if (!checkFacet(Construct.Relationship, theRelationship,
                    Facet.attributes, theContext)) {
                return;
            }

    /* interface definitions
           note: augmentation is allowed here so not clear what to check ..
             maybe report augmentations if so configured .. */

        } finally {
            theContext.exit();
        }
    }

    //requirements and capabilities assignment appear in a node templates
    private void checkRequirementsAssignmentDefinition(
            List<Map> theRequirements, CheckContext theContext) {
        theContext.enter(REQUIREMENTS);
        try {
            if (!checkDefinition(REQUIREMENTS, theRequirements, theContext)) {
                return;
            }

            //the node type for the node template enclosing these requirements
            String nodeType = (String) catalog.getTemplate(
                    theContext.target(),
                    Construct.Node,
                    theContext.enclosingConstruct(Construct.Node))
                    .get("type");

            for (Iterator<Map> ri = theRequirements.iterator(); ri.hasNext(); ) {
                Map<String, Map> requirement = (Map<String, Map>) ri.next();

                Iterator<Map.Entry<String, Map>> rai = requirement.entrySet().iterator();

                Map.Entry<String, Map> requirementEntry = rai.next();
                assert !rai.hasNext();

                String requirementName = requirementEntry.getKey();
                Map requirementDef = findNodeTypeRequirementByName(
                        nodeType, requirementName);

                if (requirementDef == null) {
                    theContext.addError("No requirement " + requirementName + WAS_DEFINED_FOR_THE_NODE_TYPE + nodeType, null);
                    continue;
                }

                checkRequirementAssignmentDefinition(
                        requirementName, requirementEntry.getValue(), requirementDef, theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkRequirementAssignmentDefinition(
            String theRequirementName,
            Map theAssignment,
            Map theDefinition,
            CheckContext theContext) {
        theContext//.enter("requirement_assignment")
                .enter(theRequirementName, Construct.Requirement);

        //grab the node type definition to verify compatibility

        try {
            //node assignment
            boolean targetNodeIsTemplate = false;
            String targetNode = (String) theAssignment.get("node");
            if (targetNode == null) {
                targetNode = (String) theDefinition.get("node");
                //targetNodeIsTemplate stays false, targetNode must be a type
            } else {
                //the value must be a node template or a node type
                targetNodeIsTemplate = isTemplateReference(
                        Construct.Node, theContext, targetNode);
                if ((!targetNodeIsTemplate) && (!isTypeReference(Construct.Node, targetNode))){
                        theContext.addError("The 'node' entry must contain a reference to a node template or node type, '" + targetNode + IS_NONE_OF_THOSE, null);
                        return;
                    }

                //additional checks
                String targetNodeDef = (String) theDefinition.get("node");
                if (targetNodeDef != null && targetNode != null) {
                    if (targetNodeIsTemplate) {
                        //if the target is node template, it must be compatible with the
                        //node type specification in the requirement defintion
                        String targetNodeType = (String)
                                catalog.getTemplate(theContext.target(), Construct.Node, targetNode).get("type");
                        if (!catalog.isDerivedFrom(
                                Construct.Node, targetNodeType, targetNodeDef)) {
                            theContext.addError("The required target node type '" + targetNodeType + "' of target node " + targetNode + " is not compatible with the target node type found in the requirement definition: " + targetNodeDef, null);
                            return;
                        }
                    } else {
                        //if the target is a node type it must be compatible (= or derived
                        //from) with the node type specification in the requirement definition
                        if (!catalog.isDerivedFrom(
                                Construct.Node, targetNode, targetNodeDef)) {
                            theContext.addError("The required target node type '" + targetNode + "' is not compatible with the target node type found in the requirement definition: " + targetNodeDef, null);
                            return;
                        }
                    }
                }
            }

            String targetNodeType = targetNodeIsTemplate ?
                    (String) catalog.getTemplate(theContext.target(), Construct.Node, targetNode).get("type") :
                    targetNode;

            //capability assignment
            boolean targetCapabilityIsType = false;
            String targetCapability = (String) theAssignment.get(CAPABILITY);
            if (targetCapability == null) {
                targetCapability = (String) theDefinition.get(CAPABILITY);
                //in a requirement definition the target capability can only be a
                //capability type (and not a capability name within some target node
                //type)
                targetCapabilityIsType = true;
            } else {
                targetCapabilityIsType = isTypeReference(Construct.Capability, targetCapability);

                //check compatibility with the target compatibility type specified
                //in the requirement definition, if any
                String targetCapabilityDef = (String) theDefinition.get(CAPABILITY);
                if (targetCapabilityDef != null && targetCapability != null) {
                    if (targetCapabilityIsType) {
                        if (!catalog.isDerivedFrom(
                                Construct.Capability, targetCapability, targetCapabilityDef)) {
                            theContext.addError("The required target capability type '" + targetCapability + "' is not compatible with the target capability type found in the requirement definition: " + targetCapabilityDef, null);
                            return;
                        }
                    } else {
                        //the capability is from a target node. Find its definition and
                        //check that its type is compatible with the capability type
                        //from the requirement definition

                        //check target capability compatibility with target node
                        if (targetNode == null) {
                            theContext.addError("The capability '" + targetCapability + "' is not a capability type, hence it has to be a capability of the node template indicated in 'node', which was not specified", null);
                            return;
                        }
                        if (!targetNodeIsTemplate) {
                            theContext.addError("The capability '" + targetCapability + "' is not a capability type, hence it has to be a capability of the node template indicated in 'node', but there you specified a node type", null);
                            return;
                        }
                        //check that the targetNode (its type) indeed has the
                        //targetCapability

                        Map<String, Object> targetNodeCapabilityDef =
                                findTypeFacetByName(
                                        Construct.Node, targetNodeType,
                                        Facet.capabilities, targetCapability);
                        if (targetNodeCapabilityDef == null) {
                            theContext.addError("No capability '" + targetCapability + "' was specified in the node " + targetNode + " of type " + targetNodeType, null);
                            return;
                        }

                        String targetNodeCapabilityType = (String) targetNodeCapabilityDef.get("type");

                        if (!catalog.isDerivedFrom(Construct.Capability,
                                targetNodeCapabilityType,
                                targetCapabilityDef)) {
                            theContext.addError("The required target capability type '" + targetCapabilityDef + "' is not compatible with the target capability type found in the target node type capability definition : " + targetNodeCapabilityType + ", targetNode " + targetNode + ", capability name " + targetCapability, null);
                            return;
                        }
                    }
                }
            }

            //relationship assignment
            Map targetRelationship = (Map) theAssignment.get("relationship");
            if (targetRelationship != null) {
                //this has to be compatible with the relationship with the same name
                //from the node type
                //check the type
            }

            //node_filter; used jxpath to simplify the navigation somewhat
            //this is too cryptic
            JXPathContext jxPath = JXPathContext.newContext(theAssignment);
            jxPath.setLenient(true);

            List<Map> propertiesFilter =
                    (List<Map>) jxPath.getValue("/node_filter/properties");
            if (propertiesFilter != null) {
                for (Map propertyFilter : propertiesFilter) {
                    if (targetNode != null) {
                        //if we have a target node or node template then it must have
                        //have these properties
                        for (Object propertyName : propertyFilter.keySet()) {
                            if (null == findTypeFacetByName(Construct.Node,
                                    targetNodeType,
                                    Facet.properties,
                                    propertyName.toString())) {
                                theContext.addError("The node_filter property " + propertyName + " is invalid: requirement target node " + targetNode + " does not have such a property", null);
                            }
                        }
                    }
                }
            }

            List<Map> capabilitiesFilter =
                    (List<Map>) jxPath.getValue("node_filter/capabilities");
            if (capabilitiesFilter != null) {
                for (Map capabilityFilterDef : capabilitiesFilter) {
                    assert capabilityFilterDef.size() == 1;
                    Map.Entry<String, Map> capabilityFilterEntry =
                            (Map.Entry<String, Map>) capabilityFilterDef.entrySet().iterator().next();
                    String targetFilterCapability = capabilityFilterEntry.getKey();
                    Map<String, Object> targetFilterCapabilityDef = null;

                    //if we have a targetNode capabilityName must be a capability of
                    //that node (type); or it can be simply capability type (but the node
                    //must have a capability of that type)

                    String targetFilterCapabilityType = null;
                    if (targetNode != null) {
                        targetFilterCapabilityDef =
                                findTypeFacetByName(Construct.Node, targetNodeType,
                                        Facet.capabilities, targetFilterCapability);
                        if (targetFilterCapabilityDef != null) {
                            targetFilterCapabilityType =
                                    (String) targetFilterCapabilityDef/*.values().iterator().next()*/.get("type");
                        } else {
                            Map<String, Map> targetFilterCapabilities =
                                    findTypeFacetByType(Construct.Node, targetNodeType,
                                            Facet.capabilities, targetFilterCapability);

                            if (!targetFilterCapabilities.isEmpty()) {
                                if (targetFilterCapabilities.size() > 1) {
                                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "checkRequirementAssignmentDefinition: filter check, target node type '{}' has more than one capability of type '{}', not supported", targetNodeType, targetFilterCapability);
                                }
                                //pick the first entry, it represents a capability of the required type
                                Map.Entry<String, Map> capabilityEntry = targetFilterCapabilities.entrySet().iterator().next();
                                targetFilterCapabilityDef = Collections.singletonMap(capabilityEntry.getKey(),
                                        capabilityEntry.getValue());
                                targetFilterCapabilityType = targetFilterCapability;
                            }
                        }
                    } else {
                        //no node (type) specified, it can be a straight capability type
                        targetFilterCapabilityDef = catalog.getTypeDefinition(
                                Construct.Capability, targetFilterCapability);
                        //here comes the odd part: it can still be a just a name in which
                        //case we should look at the requirement definition, see which
                        //capability (type) it indicates
                        assert targetCapabilityIsType; //cannot be otherwise, we'd need a node
                        targetFilterCapabilityDef = catalog.getTypeDefinition(
                                Construct.Capability, targetCapability);
                        targetFilterCapabilityType = targetCapability;
                    }

                    if (targetFilterCapabilityDef == null) {
                        theContext.addError("Capability (name or type) " + targetFilterCapability + " is invalid: not a known capability (type) " +
                                ((targetNodeType != null) ? (" of node type" + targetNodeType) : ""), null);
                        continue;
                    }

                    for (Map propertyFilter :
                            (List<Map>) jxPath.getValue("/node_filter/capabilities/" + targetFilterCapability + "/properties")) {
                        //check that the properties are in the scope of the
                        //capability definition
                        for (Object propertyName : propertyFilter.keySet()) {
                            if (null == findTypeFacetByName(Construct.Capability,
                                    targetCapability,
                                    Facet.properties,
                                    propertyName.toString())) {
                                theContext.addError("The capability filter " + targetFilterCapability + " property " + propertyName + " is invalid: target capability " + targetFilterCapabilityType + " does not have such a property", null);
                            }
                        }
                    }
                }
            }

        } finally {
            theContext//.exit()
                    .exit();
        }
    }

    private void checkCapabilitiesAssignmentDefinition(
            Map<String, Map> theCapabilities, CheckContext theContext) {
        theContext.enter(CAPABILITIES);
        try {
            if (!checkDefinition(CAPABILITIES, theCapabilities, theContext)) {
                return;
            }

            //the node type for the node template enclosing these requirements
            String nodeType = (String) catalog.getTemplate(
                    theContext.target(),
                    Construct.Node,
                    theContext.enclosingConstruct(Construct.Node))
                    .get("type");

            for (Iterator<Map.Entry<String, Map>> ci =
                 theCapabilities.entrySet().iterator();
                 ci.hasNext(); ) {

                Map.Entry<String, Map> ce = ci.next();

                String capabilityName = ce.getKey();
                Map capabilityDef = findTypeFacetByName(Construct.Node, nodeType,
                        Facet.capabilities, capabilityName);
                if (capabilityDef == null) {
                    theContext.addError("No capability " + capabilityName + WAS_DEFINED_FOR_THE_NODE_TYPE + nodeType, null);
                    continue;
                }

                checkCapabilityAssignmentDefinition(
                        capabilityName, ce.getValue(), capabilityDef, theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkCapabilityAssignmentDefinition(
            String theCapabilityName,
            Map theAssignment,
            Map theDefinition,
            CheckContext theContext) {

        theContext.enter(theCapabilityName, Construct.Capability);
        try {
            String capabilityType = (String) theDefinition.get("type");
            //list of property and attributes assignments
            checkFacet(Construct.Capability, theAssignment, capabilityType,
                    Facet.properties, theContext);
            checkFacet(Construct.Capability, theAssignment, capabilityType,
                    Facet.attributes, theContext);
        } finally {
            theContext.exit();
        }
    }

    private void checkTemplateInterfacesDefinition(
            Map<String, Map> theInterfaces,
            CheckContext theContext) {
        theContext.enter(INTERFACES);
        try {
            if (!checkDefinition(INTERFACES, theInterfaces, theContext)) {
                return;
            }

            //the node type for the node template enclosing these requirements
            String nodeType = (String) catalog.getTemplate(
                    theContext.target(),
                    Construct.Node,
                    theContext.enclosingConstruct(Construct.Node))
                    .get("type");

            for (Iterator<Map.Entry<String, Map>> ii =
                 theInterfaces.entrySet().iterator();
                 ii.hasNext(); ) {

                Map.Entry<String, Map> ie = ii.next();

                String interfaceName = ie.getKey();
                Map interfaceDef = findTypeFacetByName(Construct.Node, nodeType,
                        Facet.interfaces, interfaceName);

                if (interfaceDef == null) {
                    /* this is subject to augmentation: this could be a warning but not an error */
                    theContext.addError("No interface " + interfaceName + WAS_DEFINED_FOR_THE_NODE_TYPE + nodeType, null);
                    continue;
                }

                checkTemplateInterfaceDefinition(
                        interfaceName, ie.getValue(), interfaceDef, theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    private void checkTemplateInterfaceDefinition(
            String theInterfaceName,
            Map theAssignment,
            Map theDefinition,
            CheckContext theContext) {

        theContext.enter(theInterfaceName, Construct.Interface);
        try {
            //check the assignment of the common inputs
            checkFacet(Construct.Interface,
                    theAssignment,
                    (String) theDefinition.get("type"),
                    Facet.inputs,
                    theContext);
        } finally {
            theContext.exit();
        }
    }


    @Checks(path = "/topology_template/artifacts")
    protected void check_template_artifacts_definition(
            Map<String, Object> theDefinition,
            CheckContext theContext) {
        theContext.enter(ARTIFACTS);
        theContext.exit();
    }

    //generic checking actions, not related to validation rules

    /* will check the validity of the type specification for any construct containing a 'type' entry */
    private boolean checkType(Construct theCategory, Map theSpec, CheckContext theContext) {
        String type = (String) theSpec.get("type");
        if (type == null) {
            theContext.addError("Missing type specification", null);
            return false;
        }

        if (!catalog.hasType(theCategory, type)) {
            theContext.addError(UNKNOWN + theCategory + " type: " + type, null);
            return false;
        }

        return true;
    }

    /* the type can be:
     *   a known type: predefined or user-defined
     *   a collection (list or map) and then check that the entry_schema points to one of the first two cases (is that it?)
     */
    private boolean checkDataType(Map theSpec, CheckContext theContext) {

        if (!checkType(Construct.Data, theSpec, theContext)) {
            return false;
        }

        String type = (String) theSpec.get("type");
        if (/*isCollectionType(type)*/
                "list".equals(type) || "map".equals(type)) {
            Map entrySchema = (Map) theSpec.get("entry_schema");
            if (entrySchema == null) {
                //maybe issue a warning ?? or is 'string' the default??
                return true;
            }

            if (!catalog.hasType(Construct.Data, (String) entrySchema.get("type"))) {
                theContext.addError("Unknown entry_schema type: " + entrySchema, null);
                return false;
            }
        }
        return true;
    }

    /* Check that a particular facet (properties, attributes) of a construct type
     * (node type, capability type, etc) is correctly (consistenly) defined
     * across a type hierarchy
     */
    private boolean checkTypeConstructFacet(Construct theConstruct,
                                            String theTypeName,
                                            Map theTypeSpec,
                                            Facet theFacet,
                                            CheckContext theContext) {
        Map<String, Map> defs =
                (Map<String, Map>) theTypeSpec.get(theFacet.name());
        if (null == defs) {
            return true;
        }

        boolean res = true;

        //given that the type was cataloged there will be at least one entry
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theConstruct, theTypeName);
        if (!i.hasNext()) {
            theContext.addError(
                    "The type " + theTypeName + " needs to be cataloged before attempting 'checkTypeConstruct'", null);
            return false;
        }
        i.next(); //skip self
        while (i.hasNext()) {
            Map.Entry<String, Map> e = i.next();
            Map<String, Map> superDefs = (Map<String, Map>) e.getValue()
                    .get(theFacet.name());
            if (null == superDefs) {
                continue;
            }
            //this computes entries that appear on both collections but with different values, i.e. the re-defined properties
            Map<String, MapDifference.ValueDifference<Map>> diff = Maps.difference(defs, superDefs).entriesDiffering();

            for (Iterator<Map.Entry<String, MapDifference.ValueDifference<Map>>> di = diff.entrySet().iterator(); di.hasNext(); ) {
                Map.Entry<String, MapDifference.ValueDifference<Map>> de = di.next();
                MapDifference.ValueDifference<Map> dediff = de.getValue();
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{} type {}: {} has been re-defined between the {} types {} and {}", theConstruct, theFacet, de.getKey(), theConstruct, e.getKey(), theTypeName);
                //for now we just check that the type is consistenly re-declared
                if (!this.catalog.isDerivedFrom(theFacet.construct(),
                        (String) dediff.leftValue().get("type"),
                        (String) dediff.rightValue().get("type"))) {
                    theContext.addError(
                            theConstruct + TYPE + theFacet + ", redefiniton changed its type: " + de.getKey() + " has been re-defined between the " + theConstruct + " types " + e.getKey() + " and " + theTypeName + " in an incompatible manner", null);
                    res = false;
                }
            }
        }

        return res;
    }

    /*
     * Checks the validity of a certain facet of a construct
     * (properties of a node) across a type hierarchy.
     * For now the check is limited to a verifying that a a facet was declared
     * somewhere in the construct type hierarchy (a node template property has
     * been declared in the node type hierarchy).
     *
     * 2 versions with the more generic allowing the specification of the type
     * to be done explicitly.
     */
    private boolean checkFacet(Construct theConstruct,
                               Map theSpec,
                               Facet theFacet,
                               CheckContext theContext) {
        return checkFacet(theConstruct, theSpec, null, theFacet, theContext);
    }

    /**
     * We walk the hierarchy and verify the assignment of a property with respect to its definition.
     * We also collect the names of those properties defined as required but for which no assignment was provided.
     */
    private boolean checkFacet(Construct theConstruct,
                               Map theSpec,
                               String theSpecType,
                               Facet theFacet,
                               CheckContext theContext) {

        Map<String, Map> defs = (Map<String, Map>) theSpec.get(theFacet.name());
        if (null == defs) {
            return true;
        }
        defs = Maps.newHashMap(defs); //

        boolean res = true;
        if (theSpecType == null) {
            theSpecType = (String) theSpec.get("type");
        }
        if (theSpecType == null) {
            theContext.addError("No specification type available", null);
            return false;
        }

        Map<String, Byte> missed = new HashMap<>();  //keeps track of the missing required properties, the value is
        //false if a default was found along the hierarchy
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theConstruct, theSpecType);
        while (i.hasNext() && !defs.isEmpty()) {
            Map.Entry<String, Map> type = i.next();

            Map<String, Map> typeDefs = (Map<String, Map>) type.getValue()
                    .get(theFacet.name());
            if (null == typeDefs) {
                continue;
            }

            MapDifference<String, Map> diff = Maps.difference(defs, typeDefs);

            //this are the ones this type and the spec have in common (same key,
            //different values)
            Map<String, MapDifference.ValueDifference<Map>> facetDefs =
                    diff.entriesDiffering();
            //TODO: this assumes the definition of the facet is not cumulative, i.e.
            //subtypes 'add' something to the definition provided by the super-types
            //it considers the most specialized definition stands on its own
            for (MapDifference.ValueDifference<Map> valdef : facetDefs.values()) {
                checkDataValuation(valdef.leftValue(), valdef.rightValue(), theContext);
            }

            //remove from properties all those that appear in this type: unfortunately this returns an unmodifiable map ..
            defs = Maps.newHashMap(diff.entriesOnlyOnLeft());
        }

        if (!defs.isEmpty()) {
            theContext.addError(UNKNOWN + theConstruct + " " + theFacet + " (not declared by the type " + theSpecType + ") were used: " + defs, null);
            res = false;
        }

        if (!missed.isEmpty()) {
            List missedNames =
                    missed.entrySet()
                            .stream()
                            .filter(e -> e.getValue().byteValue() == (byte) 1)
                            .map(e -> e.getKey())
                            .collect(Collectors.toList());
            if (!missedNames.isEmpty()) {
                theContext.addError(theConstruct + " " + theFacet + " missing required values for: " + missedNames, null);
                res = false;
            }
        }

        return res;
    }

    /* Augmentation occurs in cases such as the declaration of capabilities within a node type.
     * In such cases the construct facets (the capabilitity's properties) can redefine (augment) the
     * specification found in the construct type.
     */
    private boolean checkFacetAugmentation(Construct theConstruct,
                                           Map theSpec,
                                           Facet theFacet,
                                           CheckContext theContext) {
        return checkFacetAugmentation(theConstruct, theSpec, null, theFacet, theContext);
    }

    private boolean checkFacetAugmentation(Construct theConstruct,
                                           Map theSpec,
                                           String theSpecType,
                                           Facet theFacet,
                                           CheckContext theContext) {

        Map<String, Map> augs = (Map<String, Map>) theSpec.get(theFacet.name());
        if (null == augs) {
            return true;
        }

        boolean res = true;
        if (theSpecType == null) {
            theSpecType = (String) theSpec.get("type");
        }
        if (theSpecType == null) {
            theContext.addError("No specification type available", null);
            return false;
        }

        for (Map.Entry<String, Map> ae : augs.entrySet()) {
            //make sure it was declared by the type
            Map facetDef = catalog.getFacetDefinition(theConstruct, theSpecType, theFacet, ae.getKey());
            if (facetDef == null) {
                theContext.addError(UNKNOWN + theConstruct + " " + theFacet + " (not declared by the type " + theSpecType + ") were used: " + ae.getKey(), null);
                res = false;
                continue;
            }

            //check the compatibility of the augmentation: only the type cannot be changed
            //can the type be changed in a compatible manner ??
            if (!facetDef.get("type").equals(ae.getValue().get("type"))) {
                theContext.addError(theConstruct + " " + theFacet + " " + ae.getKey() + " has a different type than its definition: " + ae.getValue().get("type") + " instead of " + facetDef.get("type"), null);
                res = false;
                continue;
            }

            //check any valuation (here just defaults)
            Object defaultValue = ae.getValue().get(DEFAULT);
            if (defaultValue != null) {
                checkDataValuation(defaultValue, ae.getValue(), theContext);
            }
        }

        return res;
    }

    private boolean catalogTypes(Construct theConstruct, Map<String, Map> theTypes, CheckContext theContext) {

        boolean res = true;
        for (Map.Entry<String, Map> typeEntry : theTypes.entrySet()) {
            res &= catalogType(theConstruct, typeEntry.getKey(), typeEntry.getValue(), theContext);
        }

        return res;
    }

    private boolean catalogType(Construct theConstruct,
                                String theName,
                                Map theDef,
                                CheckContext theContext) {

        if (!catalog.addType(theConstruct, theName, theDef)) {
            theContext.addError(theConstruct + TYPE + theName + " re-declaration", null);
            return false;
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "{} type {} has been cataloged", theConstruct, theName);

        String parentType = (String) theDef.get("derived_from");
        if (parentType != null && !catalog.hasType(theConstruct, parentType)) {
            theContext.addError(
                    theConstruct + TYPE + theName + " indicates a supertype that has not (yet) been declared: " + parentType, null);
            return false;
        }
        return true;
    }

    private boolean checkTypeReference(Construct theConstruct,
                                       CheckContext theContext,
                                       String... theTypeNames) {
        boolean res = true;
        for (String typeName : theTypeNames) {
            if (!isTypeReference(theConstruct, typeName)) {
                theContext.addError("Reference to " + theConstruct + " type '" + typeName + "' points to unknown type", null);
                res = false;
            }
        }
        return res;
    }

    private boolean isTypeReference(Construct theConstruct,
                                    String theTypeName) {
        return this.catalog.hasType(theConstruct, theTypeName);
    }

    /* node or relationship templates */
    private boolean checkTemplateReference(Construct theConstruct,
                                           CheckContext theContext,
                                           String... theTemplateNames) {
        boolean res = true;
        for (String templateName : theTemplateNames) {
            if (!isTemplateReference(theConstruct, theContext, templateName)) {
                theContext.addError("Reference to " + theConstruct + " template '" + templateName + "' points to unknown template", null);
                res = false;
            }
        }
        return res;
    }

    private boolean isTemplateReference(Construct theConstruct,
                                        CheckContext theContext,
                                        String theTemplateName) {
        return this.catalog.hasTemplate(theContext.target(), theConstruct, theTemplateName);
    }

    /*
     * For inputs/properties/attributes/(parameters). It is the caller's
     * responsability to provide the value (from a 'default', inlined, ..)
     *
     * @param theDef the definition of the given construct/facet as it appears in
     * 			its enclosing type definition.
     * @param
     */
    private boolean checkDataValuation(Object theExpr,
                                       Map<String, ?> theDef,
                                       CheckContext theContext) {
        //first check if the expression is a function, if not handle it as a value assignment
        Data.Function f = Data.function(theExpr);
        if (f != null) {
            return f.evaluator()
                    .eval(theExpr, theDef, theContext);
        } else {
            Data.Type type = Data.typeByName((String) theDef.get("type"));
            if (type != null) {
                Data.Evaluator evaluator;

                evaluator = type.evaluator();
                if (evaluator == null) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "No value evaluator available for type {}", type);
                } else {
                    if ((theExpr != null) && (!evaluator.eval(theExpr, theDef, theContext))) {
                        return false;
                    }
                }


                evaluator = type.constraintsEvaluator();
                if (evaluator == null) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "No constraints evaluator available for type {}", type);
                } else {
                    if (theExpr != null) {
                        if (!evaluator.eval(theExpr, theDef, theContext)) {
                            return false;
                        }
                    } else {
                        //should have a null value validatorT
                    }
                }

                return true;
            } else {
                theContext.addError("Expression " + theExpr + " of " + theDef + " could not be evaluated", null);
                return false;
            }
        }
    }

    /**
     * Given the type of a certain construct (node type for example), look up
     * in one of its facets (properties, capabilities, ..) for one of the given
     * facet type (if looking in property, one of the given data type).
     *
     * @return a map of all facets of the given type, will be empty to signal
     * none found
     * <p>
     * Should we look for a facet construct of a compatible type: any type derived
     * from the given facet's construct type??
     */
    private Map<String, Map>
    findTypeFacetByType(Construct theTypeConstruct,
                        String theTypeName,
                        Facet theFacet,
                        String theFacetType) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType {}, {}: {} {}", theTypeName, theTypeConstruct, theFacetType, theFacet);
        Map<String, Map> res = new HashMap<>();
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theTypeConstruct, theTypeName);
        while (i.hasNext()) {
            Map.Entry<String, Map> typeSpec = i.next();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType, Checking {} type {}", theTypeConstruct, typeSpec.getKey());
            Map<String, Map> typeFacet =
                    (Map<String, Map>) typeSpec.getValue().get(theFacet.name());
            if (typeFacet == null) {
                continue;
            }
            Iterator<Map.Entry<String, Map>> fi = typeFacet.entrySet().iterator();
            while (fi.hasNext()) {
                Map.Entry<String, Map> facet = fi.next();
                String facetType = (String) facet.getValue().get("type");
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType, Checking {} type {}", facet.getKey(), facetType);

                //here is the question: do we look for an exact match or ..
                //now we check that the type has a capability of a type compatible
                //(equal or derived from) the given capability type.
                if (catalog.isDerivedFrom(
                        theFacet.construct(), facetType, theFacetType)) {
                    res.putIfAbsent(facet.getKey(), facet.getValue());
                }
            }
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByType, found {}", res);

        return res;
    }

    private Map<String, Object>
    findTypeFacetByName(Construct theTypeConstruct,
                        String theTypeName,
                        Facet theFacet,
                        String theFacetName) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByName {} {}", theTypeConstruct, theTypeName);
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(theTypeConstruct, theTypeName);
        while (i.hasNext()) {
            Map.Entry<String, Map> typeSpec = i.next();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findTypeFacetByName, Checking {} type {}", theTypeConstruct, typeSpec.getKey());
            Map<String, Map> typeFacet =
                    (Map<String, Map>) typeSpec.getValue().get(theFacet.name());
            if (typeFacet == null) {
                continue;
            }
            Map<String, Object> facet = typeFacet.get(theFacetName);
            if (facet != null) {
                return facet;
            }
        }
        return null;
    }

    /* Requirements are the odd ball as they are structured as a sequence .. */
    private Map<String, Map> findNodeTypeRequirementByName(
            String theNodeType, String theRequirementName) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findNodeTypeRequirementByName {}/{}", theNodeType, theRequirementName);
        Iterator<Map.Entry<String, Map>> i =
                catalog.hierarchy(Construct.Node, theNodeType);
        while (i.hasNext()) {
            Map.Entry<String, Map> nodeType = i.next();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "findNodeTypeRequirementByName, Checking node type {}", nodeType.getKey());
            List<Map<String, Map>> nodeTypeRequirements =
                    (List<Map<String, Map>>) nodeType.getValue().get(REQUIREMENTS);
            if (nodeTypeRequirements == null) {
                continue;
            }

            for (Map<String, Map> requirement : nodeTypeRequirements) {
                Map requirementDef = requirement.get(theRequirementName);
                if (requirementDef != null) {
                    return requirementDef;
                }
            }
        }
        return null;
    }

    /*
     * Additional generics checks to be performed on any definition: construct,
     * construct types, etc ..
     */
    public boolean checkDefinition(String theName,
                                   Map theDefinition,
                                   CheckContext theContext) {
        if (theDefinition == null) {
            theContext.addError("Missing definition for " + theName, null);
            return false;
        }

        if (theDefinition.isEmpty()) {
            theContext.addError("Empty definition for " + theName, null);
            return false;
        }

        return true;
    }

    private boolean checkDefinition(String theName,
                                    List theDefinition,
                                    CheckContext theContext) {
        if (theDefinition == null) {
            theContext.addError("Missing definition for " + theName, null);
            return false;
        }

        if (theDefinition.isEmpty()) {
            theContext.addError("Empty definition for " + theName, null);
            return false;
        }

        return true;
    }

    /* plenty of one entry maps around */
    private Map.Entry mapEntry(Object theMap) {
        return (Map.Entry) ((Map) theMap).entrySet().iterator().next();
    }

    /**
     * Given that we remembered the canonical forms that were needed during
     * validation to replace the short forms we can apply them to the target
     * yaml.
     * We take advantage here of the fact that the context path maintained
     * during validation is compatible with (j)xpath, with the exception of
     * sequence/array indentation ..
     */

    private String patchIndexes(CharSequence thePath) {
        Matcher m = indexPattern.matcher(thePath);
        StringBuffer path = new StringBuffer();
        while (m.find()) {
            String index = m.group();
            index = "[" + (Integer.valueOf(index.substring(1)).intValue() + 1) + "]";
            m.appendReplacement(path, Matcher.quoteReplacement(index));
        }
        m.appendTail(path);
        return path.toString();
    }

    private String patchWhitespaces(String thePath) {
        String[] elems = thePath.split("/");
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < elems.length; i++) {
            if (spacePattern.matcher(elems[i]).find()) {
                path.append("[@name='")
                        .append(elems[i])
                        .append("']");
            } else {
                path.append("/")
                        .append(elems[i]);
            }
        }
        return path.toString();
    }

    private void applyCanonicals(Object theTarget,
                                 Map<String, Object> theCanonicals) {
        if (theCanonicals.isEmpty()) {
            return;
        }
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "applying canonicals: {}", theCanonicals);
        applyCanonicals(theTarget, theCanonicals, "/", false);
    }

    /*
     * applies canonicals selectively
     */
    private void applyCanonicals(Object theTarget,
                                 Map<String, Object> theCanonicals,
                                 String thePrefix,
                                 boolean doRemove) {

        JXPathContext jxPath = JXPathContext.newContext(theTarget);
        for (Iterator<Map.Entry<String, Object>> ces =
             theCanonicals.entrySet().iterator();
             ces.hasNext(); ) {
            Map.Entry<String, Object> ce = ces.next();
            //should we check prefix before or after normalization ??
            String path = ce.getKey();
            if (path.startsWith(thePrefix)) {
                path = patchWhitespaces(
                        patchIndexes(path));
                try {
                    jxPath.setValue(path, ce.getValue());
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Applied canonical form (prefix '{}') at: {}", thePrefix, path);

                    if (doRemove) {
                        ces.remove();
                    }
                } catch (JXPathException jxpx) {
                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "Failed to apply canonical to {} {}", theTarget, jxpx);
                }
            }
        }
    }

    /*
     * commons are built-in and supposed to be bulletproof so any error in here
     * goes out loud.
     */
    private static Catalog commonsCatalog() {

        synchronized (Catalog.class) {

            if (commonsCatalogInstance != null) {
                return commonsCatalogInstance;
            }

            //if other templates are going to be part of the common type system
            //add them to this list. order is relevant.
            final String[] commons = new String[]{
                    "tosca/tosca-common-types.yaml"};

            Checker commonsChecker;
            try {
                commonsChecker = new Checker();

                for (String common : commons) {
                    commonsChecker.check(common, buildCatalog(false));
                    Report commonsReport = commonsChecker.targets().iterator().next().getReport();

                    if (commonsReport.hasErrors()) {
                        throw new RuntimeException("Failed to process commons:\n" +
                                commonsReport);
                    }
                }
            } catch (CheckerException cx) {
                throw new RuntimeException("Failed to process commons", cx);
            }
            commonsCatalogInstance = commonsChecker.catalog;
            return commonsCatalogInstance;
        }
    }

    public static Catalog buildCatalog() {
        return buildCatalog(true);
    }

    private static Catalog buildCatalog(boolean doCommons) {

        Catalog catalog = new Catalog(doCommons ? commonsCatalog() : null);
        if (!doCommons) {
            //add core TOSCA types
            for (Data.CoreType type : Data.CoreType.class.getEnumConstants()) {
                catalog.addType(Construct.Data, type.toString(), Collections.emptyMap());
            }
        }
        return catalog;
    }

    private boolean invokeHook(String theHookName,
                               Class[] theArgTypes,
                               Object... theArgs) {

        Invokable hookHandler = null;
        try {
            Method m = Checker.class.getDeclaredMethod(
                    theHookName, theArgTypes);
            m.setAccessible(true);
            hookHandler = Invokable.from(m);
        } catch (NoSuchMethodException nsmx) {
            //that's ok, not every rule has to have a handler
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "That's ok, not every rule has to have a handler. Method name is:{}. Exception:{}", theHookName,nsmx);
        }

        if (hookHandler != null) {
            try {
                hookHandler.invoke(this, theArgs);
            } catch (InvocationTargetException | IllegalAccessException itx) {
                errLogger.log(LogLevel.WARN, this.getClass().getName(), "Invocation failed for hook handler {} {}", theHookName, itx);
            } catch (Exception x) {
                errLogger.log(LogLevel.WARN, this.getClass().getName(), "Hook handler failed {} {}", theHookName, x);
            }
        }

        return hookHandler != null;
    }

    private void validationHook(String theTiming,
                                Object theTarget,
                                Rule theRule,
                                Validator.ValidationContext theContext) {

        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "looking up validation handler for {}, {} {}", theRule.getName(), theTiming, theContext.getPath());
        if (!invokeHook(theRule.getName() + "_" + theTiming + "_validation_handler",
                validationHookArgTypes,
                theTarget, theRule, theContext)) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "no validation handler for {}", theRule.getName() + "_" + theTiming);
        }
    }

    private void checks(String theName,
                        Object theTarget,
                        CheckContext theContext) {
        Map<Method, Object> handlers = checks.row(/*theName*/theContext.getPath(theName));
        if (handlers != null) {
            for (Map.Entry<Method, Object> handler : handlers.entrySet()) {
                try {
                    handler.getKey().invoke(handler.getValue(), new Object[]{theTarget, theContext});
                } catch (Exception x) {
                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "Check {} with {} failed {}", theName, handler.getKey(), x);
                }
            }
        } else {
            boolean hasHook = false;
            for (Class[] argTypes : checkHookArgTypes) {
                hasHook |= invokeHook("check_" + theName,
                        argTypes,
                        theTarget, theContext);
                //shouldn't we stop as soon as hasHook is true??
            }

            if (!hasHook) {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "no check handler for {}", theName);
            }
        }
    }

    private void catalogs(String theName,
                          Object theTarget,
                          CheckContext theContext) {

        Map<Method, Object> handlers = catalogs.row(/*theName*/theContext.getPath(theName));
        if (handlers != null) {
            for (Map.Entry<Method, Object> handler : handlers.entrySet()) {
                try {
                    handler.getKey().invoke(handler.getValue(), new Object[]{theTarget, theContext});
                } catch (Exception x) {
                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "Cataloging {} with {} failed {}", theName, handler.getKey(), x);
                }
            }
        }
    }

    private class TOSCAValidator extends Validator {

        //what were validating
        private Target target;

    /* Some of the TOSCA entries accept a 'short form/notation' instead of the canonical map representation.
     * kwalify cannot easily express these alternatives and as such we handle them here. In the pre-validation phase we detect the presence of a short notation 
and compute the canonical form and validate it. In the post-validation phase we
substitute the canonical form for the short form so that checking does not have to deal with it. 
     */

        private Map<String, Object> canonicals = new TreeMap<>();

        TOSCAValidator(Target theTarget, Object theSchema) {
            super(theSchema);
            this.target = theTarget;
        }

        public Target getTarget() {
            return this.target;
        }

        /* hook method called by Validator#validate()
         */
        @Override
        protected boolean preValidationHook(Object value, Rule rule, ValidationContext context) {

            validationHook("pre", value, rule, context);
            //short form handling
            String hint = rule.getShort();
            if (value != null &&
                    hint != null) {

                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Attempting canonical at {}, rule {}", context.getPath(), rule.getName());

                Object canonical = null;
                //if the canonical form requires a collection
                if (Types.isCollectionType(rule.getType())) {
                    //and the actual value isn't one
                    if (!(value instanceof Map || value instanceof List)) {
                        //used to use singleton map/list here (was good for catching errors)
                        //but there is the possibility if short forms within short forms so
                        //the created canonicals need to accomodate other values.
                        if (Types.isMapType(rule.getType())) {
                            canonical = new HashMap();
                            ((Map) canonical).put(hint, value);
                        } else {
                            //the hint is irrelevant here but we should impose a value when the target is a list
                            canonical = new LinkedList();
                            ((List) canonical).add(value);
                        }
                    } else {
                        //we can accomodate:
                        // map to list of map transformation
                        if (!Types.isMapType(rule.getType()) /* a seq */ &&
                                value instanceof Map) {
                            canonical = new LinkedList();
                            ((List) canonical).add(value);
                        } else {
                            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Grammar for rule {} (at {}) would require unsupported short form transformation: {} to {}", rule.getName(), context.getPath(), value.getClass(), rule.getType());
                            return false;
                        }
                    }

                    int errc = context.errorCount();
                    validateRule(canonical, rule, context);
                    if (errc != context.errorCount()) {
                        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Short notation for {} through {} at {} failed validation", rule.getName(), hint, context.getPath());
                    } else {
                        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Short notation for {} through {} at {} passed validation. Canonical form is {}", rule.getName(), hint, context.getPath(), canonical);
                        //replace the short notation with the canonicall one so we don't
                        //have to deal it again during checking
                        this.canonicals.put(context.getPath(), canonical);
                        return true;
                    }
                } else {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Grammar for rule {} (at {}) would require unsupported short form transformation: {} to {}", rule.getName(), context.getPath(), value.getClass(), rule.getType());
                }
            }

            //perform default validation process
            return false;
        }

        /*
         * Only gets invoked once the value was succesfully verified against the syntax indicated by the given rule.
         */
        @Override
        protected void postValidationHook(Object value,
                                          Rule rule,
                                          ValidationContext context) {
            validationHook("post", value, rule, context);
        }

    }

    /**
     * Maintains state across the checking process.
     */
    public class CheckContext {

        private Target target;
        private ArrayList<String> elems = new ArrayList<>(10);
        private ArrayList<Construct> constructs = new ArrayList<>(10);

        CheckContext(Target theTarget) {
            this.target = theTarget;
        }

        public CheckContext enter(String theName) {
            return enter(theName, null);
        }

        public CheckContext enter(String theName, Construct theConstruct) {
            this.elems.add(theName);
            this.constructs.add(theConstruct);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "entering check {} {}", theName, getPath());
            return this;
        }

        public CheckContext exit() {
            String path = getPath();
            String name = this.elems.remove(this.elems.size() - 1);
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "exiting check {} {}", name, path);
            this.constructs.remove(this.constructs.size() - 1);
            return this;
        }

        public String getPath() {
            return buildPath(null);
        }

        String getPath(String theNextElem) {
            return buildPath(theNextElem);
        }

        String buildPath(String theElem) {
            StringBuilder sb = new StringBuilder();
            for (String e : this.elems) {
                sb.append(e)
                        .append("/");
            }
            if (theElem != null) {
                sb.append(theElem)
                        .append("/");
            }

            return sb.substring(0, sb.length() - 1);
        }

        public String enclosingConstruct(Construct theConstruct) {
            for (int i = this.constructs.size() - 1; i > 0; i--) {
                Construct c = this.constructs.get(i);
                if (c != null && c.equals(theConstruct)) {
                    return this.elems.get(i);
                }
            }
            return null;
        }

        public CheckContext addError(String theMessage, Throwable theCause) {
            this.target.report(new TargetError("", getPath(), theMessage, theCause));
            return this;
        }

        public Checker checker() {
            return Checker.this;
        }

        public Catalog catalog() {
            return Checker.this.catalog;
        }

        public Target target() {
            return this.target;
        }

        public String toString() {
            return "CheckContext(" + this.target.getLocation() + "," + getPath() + ")";
        }
    }
    
    // -------------------------------------------------------------------------------------------------- //
    
	private String errorReport(List<Throwable> theErrors) {
		StringBuilder sb = new StringBuilder(theErrors.size() + " errors");
		for (Throwable x : theErrors) {
			sb.append("\n");
			if (x instanceof ValidationException) {
				ValidationException vx = (ValidationException) x;
				// .apend("at ")
				// .append(error.getLineNumber())
				// .append(" : ")
				sb.append("[").append(vx.getPath()).append("] ");
			} else if (x instanceof TargetError) {
				TargetError tx = (TargetError) x;
				sb.append("[").append(tx.getLocation()).append("] ");
			}
			sb.append(x.getMessage());
			if (x.getCause() != null) {
				sb.append("\n\tCaused by:\n").append(x.getCause());
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	protected void range_definition_post_validation_handler(Object theValue, Rule theRule,
			Validator.ValidationContext theContext) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "entering range_definition {}",
				theContext.getPath());

		assert "seq".equals(theRule.getType());
		List bounds = (List) theValue;

		if (bounds.size() != 2) {
			theContext.addError("Too many values in bounds specification", theRule, theValue, null);
			return;
		}

		try {
			Double.parseDouble(bounds.get(0).toString());
		} catch (NumberFormatException nfe) {
			theContext.addError("Lower bound not a number", theRule, theValue, null);
		}

		try {
			Double.parseDouble(bounds.get(1).toString());
		} catch (NumberFormatException nfe) {
			if (!"UNBOUNDED".equals(bounds.get(1).toString())) {
				theContext.addError("Upper bound not a number or 'UNBOUNDED'", theRule, theValue, null);
			}
		}

	}

	/*
	 * early processing (validation time) of the imports allows us to catalog
	 * their types before those declared in the main document.
	 */
	protected void imports_post_validation_handler(Object theValue, Rule theRule, Validator.ValidationContext theContext) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "entering imports {}", theContext.getPath());

        assert "seq".equals(theRule.getType());

		Target tgt = ((TOSCAValidator) theContext.getValidator()).getTarget();

		applyCanonicals(tgt.getTarget(), ((TOSCAValidator) theContext.getValidator()).canonicals, "/imports", true);

		for (ListIterator li = ((List) theValue).listIterator(); li.hasNext();) {

			Map.Entry importEntry = mapEntry(li.next());

			Map def = (Map) importEntry.getValue();
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Processing import {}", def);

			String tfile = (String) def.get("file");
			Target tgti = this.locator.resolve(tfile);
			if (tgti == null) {
				theContext.addError("Failure to resolve import '" + def + "', imported from " + tgt, theRule, null,
						null);
				continue;
			}
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Import {} located at {}", def,
					tgti.getLocation());

			if (this.catalog.addTarget(tgti, tgt)) {
				// we've never seen this import (location) before
				try {

					List<Target> tgtis = parseTarget(tgti);
					if (tgtis.isEmpty()) {
                        continue;
                    }

					if (tgtis.size() > 1) {
						theContext.addError(
								"Import '" + tgti + "', imported from " + tgt + ", contains multiple yaml documents",
								theRule, null, null);
						continue;
					}

					tgti = tgtis.get(0);

					if (tgt.getReport().hasErrors()) {
						theContext.addError("Failure parsing import '" + tgti + IMPORTED_FROM + tgt, theRule, null,
								null);
						continue;
					}

					validateTarget(tgti);
					if (tgt.getReport().hasErrors()) {
						theContext.addError("Failure validating import '" + tgti + IMPORTED_FROM + tgt, theRule,
								null, null);
						continue;
					}
				} catch (CheckerException cx) {
					theContext.addError("Failure validating import '" + tgti + IMPORTED_FROM + tgt, theRule, cx,
							null);
				}
			}

			// replace with the actual location (also because this is what they
			// get
			// index by .. bad, this exposed catalog inner workings)

			def.put("file", tgti.getLocation());
		}
	}

	protected void node_templates_post_validation_handler(Object theValue, Rule theRule,
			Validator.ValidationContext theContext) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "entering node_templates_post_validation_handler {}",
				theContext.getPath());
		assert "map".equals(theRule.getType());
		Map<String, Map> nodeTemplates = (Map<String, Map>) theValue;
		for (Iterator<Map.Entry<String, Map>> i = nodeTemplates.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, Map> node = i.next();
			try {
				catalog.addTemplate(((TOSCAValidator) theContext.getValidator()).getTarget(), Construct.Node,
						node.getKey(), node.getValue());
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Node template {} has been cataloged",
						node.getKey());
			} catch (CatalogException cx) {
				theContext.addError(cx.toString(), theRule, node, null);
			}
		}
	}

	protected void inputs_post_validation_handler(Object theValue, Rule theRule,
			Validator.ValidationContext theContext) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "entering inputs_post_validation_handler {}",
				theContext.getPath());
		assert theRule.getType().equals("map");

		// we'll repeat this test during checking but because we index inputs
		// early
		// we need it here too
		if (theValue == null) {
			return;
		}

		Map<String, Map> inputs = (Map<String, Map>) theValue;
		for (Iterator<Map.Entry<String, Map>> i = inputs.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, Map> input = i.next();
			try {
				catalog.addTemplate(((TOSCAValidator) theContext.getValidator()).getTarget(), Construct.Data,
						input.getKey(), input.getValue());
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Input {} has been cataloged",
						input.getKey());
			} catch (CatalogException cx) {
				theContext.addError(cx.toString(), theRule, input, null);
			}
		}
	}

	private void process(String theProcessorSpec) throws CheckerException {

		String[] spec = theProcessorSpec.split(" ");
		if (spec.length == 0) {
            throw new IllegalArgumentException("Incomplete processor specification");
        }

		Class processorClass;
		try {
			processorClass = Class.forName(spec[0]);
		} catch (ClassNotFoundException cnfx) {
			throw new CheckerException("Cannot find processor implementation", cnfx);
		}

		Processor proc;
		try {
			proc = (Processor) ConstructorUtils.invokeConstructor(processorClass,
					Arrays.copyOfRange(spec, 1, spec.length));
		} catch (Exception x) {
			throw new CheckerException("Cannot instantiate processor", x);
		}

		process(proc);
	}

	protected void check_artifact_definition(String theName, Map theDef, CheckContext theContext) {
		theContext.enter(theName, Construct.Artifact);

		try {
			if (!checkDefinition(theName, theDef, theContext)) {
				return;
			}
			// check artifact type
			if (!checkType(Construct.Artifact, theDef, theContext)) {
                return;
            }
		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_policy_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Policy);

		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			if (theDefinition.containsKey(PROPERTIES)) {
				check_properties((Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
				checkTypeConstructFacet(Construct.Policy, theName, theDefinition, Facet.properties, theContext);
			}

			// the targets can be known node types or group types
			List<String> targets = (List<String>) theDefinition.get("targets");
			if (targets != null) {
				if (checkDefinition("targets", targets, theContext)) {
					for (String target : targets) {
						if (!(this.catalog.hasType(Construct.Node, target)
								|| this.catalog.hasType(Construct.Group, target))) {
							theContext.addError(
									"The 'targets' entry must contain a reference to a node type or group type, '"
											+ target + "' is none of those",
									null);
						}
					}
				}
			}

		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_group_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Group);

		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			if (theDefinition.containsKey(PROPERTIES)) {
				check_properties((Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
				checkTypeConstructFacet(Construct.Group, theName, theDefinition, Facet.properties, theContext);
			}

			if (theDefinition.containsKey("targets")) {
				checkTypeReference(Construct.Node, theContext,
						((List<String>) theDefinition.get("targets")).toArray(EMPTY_STRING_ARRAY));
			}

			// interfaces
			Map<String, Map> interfaces = (Map<String, Map>) theDefinition.get("interfaces");
			if (interfaces != null) {
				try {
					theContext.enter("interfaces");
					for (Iterator<Map.Entry<String, Map>> i = interfaces.entrySet().iterator(); i.hasNext();) {
						Map.Entry<String, Map> e = i.next();
						check_type_interface_definition(e.getKey(), e.getValue(), theContext);
					}
				} finally {
					theContext.exit();
				}
			}

		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_node_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Node);

		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			if (theDefinition.containsKey(PROPERTIES)) {
				check_properties((Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
				checkTypeConstructFacet(Construct.Node, theName, theDefinition, Facet.properties, theContext);
			}

			if (theDefinition.containsKey("attributes")) {
				check_properties((Map<String, Map>) theDefinition.get("attributes"), theContext);
				checkTypeConstructFacet(Construct.Node, theName, theDefinition, Facet.attributes, theContext);
			}

			// requirements
			if (theDefinition.containsKey("requirements")) {
				check_requirements((List<Map>) theDefinition.get("requirements"), theContext);
			}

			// capabilities
			if (theDefinition.containsKey(CAPABILITIES)) {
				check_capabilities((Map<String, Map>) theDefinition.get(CAPABILITIES), theContext);
			}

			// interfaces:
			Map<String, Map> interfaces = (Map<String, Map>) theDefinition.get("interfaces");
			if (interfaces != null) {
				try {
					theContext.enter("interfaces");
					for (Iterator<Map.Entry<String, Map>> i = interfaces.entrySet().iterator(); i.hasNext();) {
						Map.Entry<String, Map> e = i.next();
						check_type_interface_definition(e.getKey(), e.getValue(), theContext);
					}
				} finally {
					theContext.exit();
				}
			}

			// artifacts

		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_interface_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Interface);
		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			// not much else here: a list of operation_definitions, each with
			// its
			// implementation and inputs

			// check that common inputs are re-defined in a compatible manner

			// check that the interface operations are overwritten in a
			// compatible manner
			// for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet()

		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_artifact_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Artifact);
		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_relationship_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Relationship);
		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			if (theDefinition.containsKey(PROPERTIES)) {
				check_properties((Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
				checkTypeConstructFacet(Construct.Relationship, theName, theDefinition, Facet.properties, theContext);
			}

			if (theDefinition.containsKey("attributes")) {
				check_properties((Map<String, Map>) theDefinition.get("attributes"), theContext);
				checkTypeConstructFacet(Construct.Relationship, theName, theDefinition, Facet.attributes, theContext);
			}

			Map<String, Map> interfaces = (Map<String, Map>) theDefinition.get("interfaces");
			if (interfaces != null) {
				theContext.enter("interfaces");
				for (Iterator<Map.Entry<String, Map>> i = interfaces.entrySet().iterator(); i.hasNext();) {
					Map.Entry<String, Map> e = i.next();
					check_type_interface_definition(e.getKey(), e.getValue(), theContext);
				}
				theContext.exit();
			}

			if (theDefinition.containsKey(VALID_TARGET_TYPES)) {
				checkTypeReference(Construct.Capability, theContext,
						((List<String>) theDefinition.get(VALID_TARGET_TYPES)).toArray(EMPTY_STRING_ARRAY));
			}
		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_capability_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Capability);

		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			if (theDefinition.containsKey(PROPERTIES)) {
				check_properties((Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
				checkTypeConstructFacet(Construct.Capability, theName, theDefinition, Facet.properties, theContext);
			}

			if (theDefinition.containsKey("attributes")) {
				check_attributes((Map<String, Map>) theDefinition.get("attributes"), theContext);
				checkTypeConstructFacet(Construct.Capability, theName, theDefinition, Facet.attributes, theContext);
			}

			// valid_source_types: see capability_type_definition
			// unclear: how is the valid_source_types list definition eveolving
			// across
			// the type hierarchy: additive, overwriting, ??
			if (theDefinition.containsKey("valid_source_types")) {
				checkTypeReference(Construct.Node, theContext,
						((List<String>) theDefinition.get("valid_source_types")).toArray(EMPTY_STRING_ARRAY));
			}
		} finally {
			theContext.exit();
		}
	}

	/* */
	protected void check_data_type_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName, Construct.Data);
		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}

			if (theDefinition.containsKey(PROPERTIES)) {
				check_properties((Map<String, Map>) theDefinition.get(PROPERTIES), theContext);
				checkTypeConstructFacet(Construct.Data, theName, theDefinition, Facet.properties, theContext);
			}
		} finally {
			theContext.exit();
		}
	}

	/*
	 * top level rule, we collected the whole information set. this is where
	 * checking starts
	 */
	protected void check_service_template_definition(Map<String, Object> theDef, CheckContext theContext) {
		theContext.enter("");

		if (theDef == null) {
			theContext.addError("Empty template", null);
			return;
		}

		// !!! imports need to be processed first now that catalogging takes
		// place at check time!!

		// first catalog whatever it is there to be cataloged so that the checks
		// can perform cross-checking
		for (Iterator<Map.Entry<String, Object>> ri = theDef.entrySet().iterator(); ri.hasNext();) {
			Map.Entry<String, Object> e = ri.next();
			catalogs(e.getKey(), e.getValue(), theContext);
		}

		for (Iterator<Map.Entry<String, Object>> ri = theDef.entrySet().iterator(); ri.hasNext();) {
			Map.Entry<String, Object> e = ri.next();
			checks(e.getKey(), e.getValue(), theContext);
		}
		theContext.exit();
	}

	protected void check_attribute_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName);
		try {
			if (!checkDefinition(theName, theDefinition, theContext)) {
				return;
			}
			if (!checkDataType(theDefinition, theContext)) {
				return;
			}
		} finally {
			theContext.exit();
		}
	}

	public void check_attributes(Map<String, Map> theDefinitions, CheckContext theContext) {
		theContext.enter("attributes");
		try {
			if (!checkDefinition("attributes", theDefinitions, theContext)) {
                return;
            }

			for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext();) {
				Map.Entry<String, Map> e = i.next();
				check_attribute_definition(e.getKey(), e.getValue(), theContext);
			}
		} finally {
			theContext.exit();
		}
	}

	protected void check_property_definition(String theName, Map theDefinition, CheckContext theContext) {
		theContext.enter(theName);
		if (!checkDefinition(theName, theDefinition, theContext)) {
			return;
		}
		// check the type
		if (!checkDataType(theDefinition, theContext)) {
			return;
		}
		// check default value is compatible with type
		Object defaultValue = theDefinition.get("default");
		if (defaultValue != null) {
			checkDataValuation(defaultValue, theDefinition, theContext);
		}

		theContext.exit();
	}

	public void check_properties(Map<String, Map> theDefinitions, CheckContext theContext) {
		theContext.enter(PROPERTIES);
		try {
			if (!checkDefinition(PROPERTIES, theDefinitions, theContext)) {
                return;
            }

			for (Iterator<Map.Entry<String, Map>> i = theDefinitions.entrySet().iterator(); i.hasNext();) {
				Map.Entry<String, Map> e = i.next();
				check_property_definition(e.getKey(), e.getValue(), theContext);
			}
		} finally {
			theContext.exit();
		}
	}

}


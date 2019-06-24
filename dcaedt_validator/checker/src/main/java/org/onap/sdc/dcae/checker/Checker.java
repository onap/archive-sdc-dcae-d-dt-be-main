package org.onap.sdc.dcae.checker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.io.File;
import java.io.Reader;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

import com.google.common.reflect.Invokable;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.checker.common.*;
import org.onap.sdc.dcae.checker.validation.TOSCAValidator;
import org.yaml.snakeyaml.Yaml;


import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

import kwalify.Validator;
import kwalify.Rule;
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

import static org.onap.sdc.dcae.checker.common.ConstCommon.*;
import static org.onap.sdc.dcae.checker.common.ConstCommon.INTERFACE_TYPES;

/*
 * To consider: model consistency checking happens now along with validation
 * (is implemented as part of the validation hooks). It might be better to
 * separate the 2 stages and perform all the consistency checking once 
 * validation is completed.
 */
public class Checker implements IChecker {

    private CheckCommon checkCommon;
    private TypeCommon typeCommon;
    private ArtifactCommon artifactCommon;
    private CapabilityCommon capabilityCommon;
    private FacetCommon facetCommon;
    private GroupCommon groupCommon;
    private InputsOutputsCommon inputsOutputsCommon;
    private InterfaceCommon interfaceCommon;
    private PropertiesCommon propertiesCommon;
    private RelationshipCommon relationshipCommon;
    private NodeCommon nodeCommon;
    private PolicyCommon policyCommon;
    private RequirementCommon requirementCommon;
    private AttributesCommon attributesCommon;

    private Target target = null; //what we're validating at the moment

    private Map<String, Target> grammars = new HashMap<>(); //grammars for the different tosca versions

    private Catalog catalog;
    private TargetLocator locator = new CommonLocator();

    private Table<String, Method, Object> checks = HashBasedTable.create();
    private Table<String, Method, Object> catalogs = HashBasedTable.create();

    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private static Catalog commonsCatalogInstance = null;

    private static Class[] validationHookArgTypes =
            new Class[]{Object.class, Rule.class, Validator.ValidationContext.class};



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



    public Checker() throws CheckerException {
        initCommons();

        loadGrammars();
        loadAnnotations();
    }

    private void initCommons() {
        NodeCommon.init(this);
        InterfaceCommon.init(this);
        checkCommon = CheckCommon.getInstance();
        typeCommon = TypeCommon.getInstance();
        artifactCommon = ArtifactCommon.getInstance();
        capabilityCommon = CapabilityCommon.getInstance();
        facetCommon = FacetCommon.getInstance();
        groupCommon = GroupCommon.getInstance();
        inputsOutputsCommon = InputsOutputsCommon.getInstance();
        interfaceCommon = InterfaceCommon.getInstance();
        propertiesCommon = PropertiesCommon.getInstance();
        relationshipCommon = RelationshipCommon.getInstance();
        nodeCommon = NodeCommon.getInstance();
        policyCommon = PolicyCommon.getInstance();
        requirementCommon = RequirementCommon.getInstance();
        attributesCommon = AttributesCommon.getInstance();
    }

    @FunctionalInterface
    interface Function<A, B, C, D> {
        void function(String key, Map value, Checker.CheckContext theContext, Catalog catalog);

    }

    @FunctionalInterface
    interface FunctionWithoutCatalog<A, B, C> {
        void function(String key, Map value, Checker.CheckContext theContext);

    }
    @FunctionalInterface
    interface FunctionWithTarget<A, B, C, D, E> {
        void function(String key, Map value, Checker.CheckContext theContext, Catalog catalog, Target target);
    }

    private void abstractCheck(Function function, Map<String, Map> stringMapMap, Checker.CheckContext theContext, String type) {
        theContext.enter(type);
        try {
            if (!checkCommon.checkDefinition(type, stringMapMap, theContext)) {
                return;
            }

            stringMapMap.forEach((key, value) -> function.function(key, value, theContext, catalog));
        } finally {
            theContext.exit();
        }
    }

    private void abstractCheck(FunctionWithoutCatalog function, Map<String, Map> stringMapMap, Checker.CheckContext theContext, String type) {
        theContext.enter(type);
        try {
            if (!checkCommon.checkDefinition(type, stringMapMap, theContext)) {
                return;
            }

            stringMapMap.forEach((key, value) -> function.function(key, value, theContext));
        } finally {
            theContext.exit();
        }
    }

    private void abstractCheck(FunctionWithTarget function, Map<String, Map> stringMapMap, Checker.CheckContext theContext, String type) {
        theContext.enter(type);
        try {
            if (!checkCommon.checkDefinition(type, stringMapMap, theContext)) {
                return;
            }

            stringMapMap.forEach((key, value) -> function.function(key, value, theContext, catalog, target));
        } finally {
            theContext.exit();
        }
    }


    public static void main(String[] theArgs) {
        if (theArgs.length == 0) {
            errLogger.log(LogLevel.ERROR, Checker.class.getName(), "checker resource_to_validate [processor]*");
            return;
        }

        try {
            Catalog cat = Checker.check(new File(theArgs[0]));

            cat.targets().forEach(t -> errLogger.log(LogLevel.ERROR, Checker.class.getName(), "{}\n{}\n{}", t.getLocation(), cat.importString(t), t.getReport()));

            cat.sortedTargets().forEach(t -> errLogger.log(LogLevel.ERROR, Checker.class.getName(), t.toString()));

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
        checkHandlers.forEach(checkHandler -> checks.put(checkHandler.getAnnotation(Checks.class).path(),
                checkHandler,
                handlers.computeIfAbsent(checkHandler.getDeclaringClass(),
                        type -> {
                            try {
                                return (getClass() == type) ? this
                                        : type.newInstance();
                            } catch (Exception x) {
                                throw new RuntimeException(x);
                            }
                        })));

        Set<Method> catalogHandlers = reflections.getMethodsAnnotatedWith(Catalogs.class);
        catalogHandlers.forEach(catalogHandler -> catalogs.put(catalogHandler.getAnnotation(Catalogs.class).path(),
                catalogHandler,
                handlers.computeIfAbsent(catalogHandler.getDeclaringClass(),
                        type -> {
                            try {
                                return (getClass() == type) ? this
                                        : type.newInstance();
                            } catch (Exception x) {
                                throw new RuntimeException(x);
                            }
                        })));
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

        Reader source;
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
            /*
            !!We're changing the target below, i.e. we're changing the target implementation hence caching implementation will suffer!!
            */
            IntStream.range(0, yamlRoots.size()).forEach(i -> {
                Target newTarget = new Target(theTarget.getName(),
                        fragmentTargetURI(theTarget.getLocation(), String.valueOf(i)));
                newTarget.setTarget(yamlRoots.get(i));
                targets.add(newTarget);
            });
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
            validator = new TOSCAValidator(theTarget, grammar.getTarget(), this);
        } catch (SchemaException sx) {
            throw new CheckerException("Grammar error at: " + sx.getPath(), sx);
        }

        theTarget.getReport().addAll(
                validator.validate(theTarget.getTarget()));

        if (!theTarget.getReport().hasErrors()) {
            applyCanonicals(theTarget.getTarget(), validator.getCanonicals());
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
        theDef.forEach((key, value) -> catalogs(key, value, theContext));

        theDef.forEach((key, value) -> checks(key, value, theContext));
        theContext.exit();
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

        for (Object importEntry : theImports) {
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

    /*
     * Additional generics checks to be performed on any definition: construct,
     * construct types, etc ..
     */


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
        Arrays.stream(elems).forEach(elem -> {
            if (spacePattern.matcher(elem).find()) {
                path.append("[@name='")
                        .append(elem)
                        .append("']");
            } else {
                path.append("/")
                        .append(elem);
            }
        });
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
            Arrays.stream(Data.CoreType.class.getEnumConstants()).forEach(type -> catalog.addType(Construct.Data, type.toString(), Collections.emptyMap()));
        }
        return catalog;
    }

    private void checks(String theName,
                        Object theTarget,
                        CheckContext theContext) {
        Map<Method, Object> handlers = checks.row(/*theName*/theContext.getPath(theName));
        if (handlers != null) {
            handlers.entrySet().forEach(handler -> {
                try {
                    handler.getKey().invoke(handler.getValue(), new Object[]{theTarget, theContext});
                } catch (Exception x) {
                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "Check {} with {} failed {}", theName, handler.getKey(), x);
                }
            });
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
            handlers.forEach((key, value) -> {
                try {
                    key.invoke(value, theTarget, theContext);
                } catch (Exception x) {
                    errLogger.log(LogLevel.WARN, this.getClass().getName(), "Cataloging {} with {} failed {}", theName, key, x);
                }
            });
        }
    }

    public boolean checkDefinition(String workflows, Map theDefinition, CheckContext theContext) {
        return  checkCommon.checkDefinition(workflows, theDefinition, theContext);
    }

    public void checkProperties(Map<String, Map> inputs, CheckContext theContext) {
        propertiesCommon.checkProperties(inputs, theContext, catalog);
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
        theErrors.forEach(x -> {
            sb.append("\n");
            if (x instanceof ValidationException) {
                ValidationException vx = (ValidationException) x;
                sb.append("[").append(vx.getPath()).append("] ");
            } else if (x instanceof TargetError) {
                TargetError tx = (TargetError) x;
                sb.append("[").append(tx.getLocation()).append("] ");
            }
            sb.append(x.getMessage());
            if (x.getCause() != null) {
                sb.append("\n\tCaused by:\n").append(x.getCause());
            }
        });
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

		applyCanonicals(tgt.getTarget(), ((TOSCAValidator) theContext.getValidator()).getCanonicals(), "/imports", true);

        for (Object o : ((List) theValue)) {

            Map.Entry importEntry = mapEntry(o);

            Map def = (Map) importEntry.getValue();
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Processing import {}", def);

            String tfile = (String) def.get("file");
            Target tgti = this.locator.resolve(tfile);
            if (tgti == null) {
                theContext.addError("Failure to resolve import '" + def + "', " + IMPORTED_FROM + " " + tgt, theRule, null,
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
                                "Import '" + tgti + "', " + IMPORTED_FROM + " " + tgt + ", contains multiple yaml documents",
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
		assert MAP.equals(theRule.getType());
		Map<String, Map> nodeTemplates = (Map<String, Map>) theValue;
        nodeTemplates.entrySet().forEach(node -> {
            try {
                catalog.addTemplate(((TOSCAValidator) theContext.getValidator()).getTarget(), Construct.Node,
                        node.getKey(), node.getValue());
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Node template {} has been cataloged",
                        node.getKey());
            } catch (CatalogException cx) {
                theContext.addError(cx.toString(), theRule, node, null);
            }
        });
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

    @Catalogs(path = "/artifact_types")
    protected void catalog_artifact_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(ARTIFACT_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Artifact, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/artifact_types")
    protected void check_artifact_types(
            Map<String, Map> theDefinition, Checker.CheckContext theContext) {
        abstractCheck(artifactCommon::checkArtifactTypeDefinition, theDefinition, theContext, ARTIFACT_TYPES);
    }

    @Catalogs(path = "/capability_types")
    protected void catalog_capability_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(ConstCommon.CAPABILITY_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Capability, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/capability_types")
    protected void check_capability_types(
            Map<String, Map> theTypes, Checker.CheckContext theContext) {
        abstractCheck(capabilityCommon::checkCapabilityTypeDefinition, theTypes, theContext, CAPABILITY_TYPES);
    }

    @Catalogs(path = "/data_types")
    protected void catalog_data_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(DATA_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Data, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/data_types")
    protected void check_data_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        abstractCheck(checkCommon::checkDataTypeDefinition, theDefinitions, theContext, DATA_TYPES);

    }


    @Catalogs(path = "/group_types")
    protected void catalog_group_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(GROUP_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Group, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/group_types")
    protected void check_group_types(
            Map<String, Map> theDefinition, Checker.CheckContext theContext) {
        abstractCheck(groupCommon::checkGroupTypeDefinition, theDefinition, theContext, GROUP_TYPES);
    }

    @Catalogs(path = "/interface_types")
    protected void catalog_interface_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(INTERFACE_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Interface, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    @Checks(path = "/interface_types")
    protected void check_interface_types(
            Map<String, Map> theDefinition, Checker.CheckContext theContext) {
        abstractCheck(interfaceCommon::checkInterfaceTypeDefinition, theDefinition, theContext, INTERFACE_TYPES);
    }

    @Catalogs(path = "/node_types")
    protected void catalog_node_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(NODE_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Node, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/node_types")
    protected void check_node_types(
            Map<String, Map> theDefinition, Checker.CheckContext theContext) {
        abstractCheck(nodeCommon::checkNodeTypeDefinition, theDefinition, theContext, NODE_TYPES);
    }

    @Catalogs(path = "/policy_types")
    protected void catalog_policy_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(POLICY_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Policy, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/policy_types")
    protected void check_policy_types(
            Map<String, Map> theDefinition, Checker.CheckContext theContext) {
        abstractCheck(policyCommon::checkPolicyTypeDefinition, theDefinition, theContext, POLICY_TYPES);
    }

    @Catalogs(path = "/relationship_types")
    protected void catalog_relationship_types(
            Map<String, Map> theDefinitions, Checker.CheckContext theContext) {
        theContext.enter(RELATIONSHIP_TYPES);
        try {
            typeCommon.catalogTypes(Construct.Relationship, theDefinitions, theContext, catalog);
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/relationship_types")
    protected void check_relationship_types(
            Map<String, Map> theDefinition, Checker.CheckContext theContext) {
        abstractCheck(relationshipCommon::checkRelationshipTypeDefinition, theDefinition, theContext, RELATIONSHIP_TYPES);
    }

    @Checks(path = "/topology_template/groups")
    protected void check_groups(Map<String, Map> theGroups,
                             Checker.CheckContext theContext) {
        abstractCheck(groupCommon::checkGroupDefinition, theGroups, theContext, GROUPS);
    }

    @Checks(path = "/topology_template/policies")
    protected void check_policies(List<Map<String, Map>> thePolicies,
                               Checker.CheckContext theContext) {
        theContext.enter(POLICIES);

        try {
            if (!checkCommon.checkDefinition(POLICIES, thePolicies, theContext)) {
                return;
            }

            thePolicies.forEach(policy -> {
                assert policy.size() == 1;
                Map.Entry<String, Map> e = policy.entrySet().iterator().next();
                policyCommon.checkPolicyDefinition(e.getKey(), e.getValue(), theContext, catalog, target);
            });
        } finally {
            theContext.exit();
        }
    }

    /* */
    @Checks(path = "/topology_template/substitution_mappings")
    protected void check_substitution_mappings(Map<String, Object> theSub,
                                            Checker.CheckContext theContext) {
        theContext.enter("substitution_mappings");
        try {
            //type is mandatory
            String type = (String) theSub.get("node_type");
            if (!typeCommon.checkTypeReference(Construct.Node, theContext, catalog, type)) {
                theContext.addError("Unknown node type: " + type + "", null);
                return; //not much to go on with
            }

            Map<String, List> capabilities = (Map<String, List>) theSub.get(ConstCommon.CAPABILITIES);
            if (null != capabilities) {
                //the key must be a capability of the type
                //the value is a 2 element list: first is a local node,
                //second is the name of one of its capabilities
                capabilities.entrySet().forEach(ce -> {
                    if (null == facetCommon.findTypeFacetByName(Construct.Node, type,
                            Facet.capabilities, ce.getKey(), catalog)) {
                        theContext.addError("Unknown node type capability: " + ce.getKey() + ", type " + type, null);
                    }
                    if (!checkValidationOnCatalog(ce, "capability", theContext, Construct.Node)) {
                        return;
                    }
                    String targetNode = (String) ce.getValue().get(0);
                    Map<String, Object> targetNodeDef = (Map<String, Object>)
                            catalog.getTemplate(theContext.target(), Construct.Node, targetNode);
                    String targetCapability = (String) ce.getValue().get(1);
                    String targetNodeType = (String) targetNodeDef.get("type");
                    if (null == facetCommon.findTypeFacetByName(Construct.Node, targetNodeType,
                            Facet.capabilities, targetCapability, catalog)) {
                        theContext.addError("Invalid capability mapping capability: " + targetCapability + ". No such capability found for node template " + targetNode + ", of type " + targetNodeType, null);
                    }
                });
            }

            Map<String, List> requirements = (Map<String, List>) theSub.get(ConstCommon.REQUIREMENTS);
            if (null != requirements) {
                //the key must be a requirement of the type
                requirements.entrySet().forEach(re -> {
                    if (null == nodeCommon.findNodeTypeRequirementByName(type, re.getKey(), catalog)) {
                        theContext.addError("Unknown node type requirement: " + re.getKey() + ", type " + type, null);
                    }
                    if (!checkValidationOnCatalog(re, "requirement", theContext, Construct.Node)) {
                        return;
                    }
                    String targetNode = (String) re.getValue().get(0);
                    Map<String, Object> targetNodeDef = (Map<String, Object>)
                            catalog.getTemplate(theContext.target(), Construct.Node, targetNode);
                    String targetRequirement = (String) re.getValue().get(1);
                    String targetNodeType = (String) targetNodeDef.get("type");
                    if (null == nodeCommon.findNodeTypeRequirementByName(targetNodeType, targetRequirement, catalog)) {
                        theContext.addError("Invalid requirement mapping requirement: " + targetRequirement + ". No such requirement found for node template " + targetNode + ", of type " + targetNodeType, null);
                    }
                });
            }
        } finally {
            theContext.exit();
        }
    }

    private boolean checkValidationOnCatalog(Map.Entry<String, List> target, String name, Checker.CheckContext theContext, Construct construct) {
        List targetList = target.getValue();
        if (targetList.size() != 2) {
            theContext.addError("Invalid " + name + " mapping: " + target + ", expecting 2 elements", null);
            return false;
        }

        String targetNode = (String) targetList.get(0);

        Map<String, Object> targetNodeDef = (Map<String, Object>)
                catalog.getTemplate(theContext.target(), construct, targetNode);
        if (null == targetNodeDef) {
            theContext.addError("Invalid " + name + " mapping node template: " + targetNode, null);
            return false;
        }
        return true;
    }

    @Override
    @Checks(path = "/topology_template/artifacts")
    public void check_template_artifacts_definition(
            Map<String, Object> theDefinition,
            Checker.CheckContext theContext) {
        theContext.enter(ARTIFACTS);
        theContext.exit();
    }

    @Checks(path = "/topology_template/relationship_templates")
    protected void check_relationship_templates(Map theTemplates,
                                                Checker.CheckContext theContext) {
        abstractCheck(relationshipCommon::checkRelationshipTemplateDefinition, theTemplates, theContext, RELATIONSHIP_TEMPLATES);
    }

    @Checks(path = "topology_template/outputs")
    protected void check_outputs(Map<String, Map> theOutputs,
                              Checker.CheckContext theContext) {
        abstractCheck(inputsOutputsCommon::checkOutputDefinition, theOutputs, theContext, OUTPUTS);
    }

    /* */
    @Checks(path = "/topology_template/node_templates")
    protected void check_node_templates(Map<String, Map> theTemplates,
                                     Checker.CheckContext theContext) {
        abstractCheck(nodeCommon::checkNodeTemplateDefinition, theTemplates, theContext, NODE_TEMPLATES);
    }

    /* */
    @Override
    @Checks(path = "/topology_template/inputs")
    public void check_inputs(Map<String, Map> theInputs,
                             Checker.CheckContext theContext) {
        abstractCheck(inputsOutputsCommon::checkInputDefinition, theInputs, theContext, INPUTS);
    }

    @Override
    public void validationHook(String theTiming,
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
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), getClass().getName(), "That's ok, not every rule has to have a handler. Method name =", theHookName);
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

    public void inputs_post_validation_handler(Object theValue, Rule theRule,
                                               Validator.ValidationContext theContext) {
        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "entering inputs_post_validation_handler {}",
                theContext.getPath());
        assert MAP.equals(theRule.getType());

        // we'll repeat this test during checking but because we index inputs
        // early
        // we need it here too
        if (theValue == null) {
            return;
        }

        Map<String, Map> inputs = (Map<String, Map>) theValue;
        inputs.entrySet().forEach(input -> {
            try {
                catalog.addTemplate(((TOSCAValidator) theContext.getValidator()).getTarget(), Construct.Data,
                        input.getKey(), input.getValue());
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Input {} has been cataloged",
                        input.getKey());
            } catch (CatalogException cx) {
                theContext.addError(cx.toString(), theRule, input, null);
            }
        });
    }

    /* */
    public void check_node_type_definition(String theName, Map theDefinition, Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Node);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            checkProperties(theName, theDefinition, theContext, PROPERTIES, Construct.Node, Facet.properties);
            checkProperties(theName, theDefinition, theContext, ATTRIBUTES, Construct.Node, Facet.attributes);
            // requirements
            checkRequirements(theDefinition, theContext, requirementCommon);
            // capabilities
            checkCapabilities(theDefinition, theContext, capabilityCommon);
            // interfaces:
            checkInterfaces(theDefinition, theContext);

            // artifacts

        } finally {
            theContext.exit();
        }
    }

    private void checkCapabilities(Map theDefinition, CheckContext theContext, CapabilityCommon capabilityCommon) {
        if (theDefinition.containsKey(CAPABILITIES)) {
            capabilityCommon.check_capabilities((Map<String, Map>) theDefinition.get(CAPABILITIES), theContext, catalog);
        }
    }

    private void checkRequirements(Map theDefinition, CheckContext theContext, RequirementCommon requirementCommon) {
        if (theDefinition.containsKey(REQUIREMENTS)) {
            requirementCommon.check_requirements((List<Map>) theDefinition.get(REQUIREMENTS), theContext, catalog);
        }
    }

    private void checkProperties(String theName, Map theDefinition, CheckContext theContext, String definition, Construct node, Facet facet) {
        if (theDefinition.containsKey(definition)) {
            propertiesCommon.check_properties((Map<String, Map>) theDefinition.get(definition), theContext, catalog);
            facetCommon.checkTypeConstructFacet(node, theName, theDefinition, facet, theContext, catalog);
        }
    }

    /* */
    public void check_data_type_definition(String theName, Map theDefinition, Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Data);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            checkProperties(theName, theDefinition, theContext, PROPERTIES, Construct.Data, Facet.properties);
        } finally {
            theContext.exit();
        }
    }

    /* */
    public void check_capability_type_definition(String theName, Map theDefinition, Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Capability);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            checkProperties(theName, theDefinition, theContext, PROPERTIES, Construct.Capability, Facet.properties);

            if (theDefinition.containsKey(ATTRIBUTES)) {
                attributesCommon.check_attributes((Map<String, Map>) theDefinition.get(ATTRIBUTES), theContext, catalog);
                facetCommon.checkTypeConstructFacet(Construct.Capability, theName, theDefinition, Facet.attributes, theContext, catalog);
            }

            // valid_source_types: see capability_type_definition
            // unclear: how is the valid_source_types list definition eveolving
            // across
            // the type hierarchy: additive, overwriting, ??
            if (theDefinition.containsKey("valid_source_types")) {
                typeCommon.checkTypeReference(Construct.Node, theContext, catalog,
                        ((List<String>) theDefinition.get("valid_source_types")).toArray(EMPTY_STRING_ARRAY));
            }
        } finally {
            theContext.exit();
        }
    }


    /* */
    public void check_group_type_definition(String theName, Map theDefinition, Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Group);

        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
                return;
            }

            checkProperties(theName, theDefinition, theContext, PROPERTIES, Construct.Group, Facet.properties);

            if (theDefinition.containsKey(TARGETS)) {
                typeCommon.checkTypeReference(Construct.Node, theContext, catalog,
                        ((List<String>) theDefinition.get(TARGETS)).toArray(EMPTY_STRING_ARRAY));
            }

            // interfaces
            checkInterfaces(theDefinition, theContext);

        } finally {
            theContext.exit();
        }
    }

    private void checkInterfaces(Map theDefinition, CheckContext theContext) {
        Map<String, Map> interfaces = (Map<String, Map>) theDefinition.get(INTERFACES);
        if (interfaces != null) {
            try {
                theContext.enter(INTERFACES);
                interfaces.forEach((key, value) -> interfaceCommon.check_type_interface_definition(key, value, theContext, catalog));
            } finally {
                theContext.exit();
            }
        }
    }

    /* */
    public void check_interface_type_definition(String theName, Map theDefinition, Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Interface);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext)) {
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
}


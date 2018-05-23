package org.onap.sdc.dcae.checker;

import java.util.Iterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;

import java.util.stream.Collectors;

import java.net.URI;

import com.google.common.base.Predicate;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.Enums.LogLevel;

/*
 * Oddball: tracking inputs as data templates could be seen as rather
 * odd but we see them as instances of data types, in the same way node 
 * templates are instances of node types.
 */
public class Catalog {

    private static final String DERIVED_FROM = "derived_from";
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();


    /* tracks imports, i.e.targets */
    private LinkedHashMap<URI, Target> targets =
            new LinkedHashMap<>();
    /* tracks dependencies between targets, i.e. the 'adjency' matrix defined by
     * the 'import' relationship */
    private Table<Target,Target,Boolean> imports = HashBasedTable.create();


    /* Type hierarchies are stored as maps from a type name to its definition
    * Not the best but easy to follow hierarchies towards their root ..
    */
   private EnumMap<Construct, Map<String,Map>> types =
                            new EnumMap<>(Construct.class);
     /* track templates: we track templates (tye instances) first per target then per contruct.
    * This allows us to share the catalog among multiple templates sharign the same type set
    */
   private Map<Target, EnumMap<Construct, Map<String,Map>>> templates =
                            new HashMap<>();

    private Catalog parent;

    public Catalog(Catalog theParent) {
        this.parent = theParent;
        /* there are no requirement types, they are the same as capability types */
        types.put(Construct.Data, new LinkedHashMap<>());
        types.put(Construct.Capability, new LinkedHashMap<>());
        types.put(Construct.Relationship, new LinkedHashMap<>());
        types.put(Construct.Artifact, new LinkedHashMap<>());
        types.put(Construct.Interface, new LinkedHashMap<>());
        types.put(Construct.Node, new LinkedHashMap<>());
        types.put(Construct.Group, new LinkedHashMap<>());
        types.put(Construct.Policy, new LinkedHashMap<>());
     
    }

    public Catalog() {
        this(null);
    }

    public boolean addType(Construct theConstruct, String theName, Map theDef) {
        if (hasType(theConstruct, theName)) {
            return false;
        }
        getConstructTypes(theConstruct).put(theName, theDef);
        return true;
  }

    public Map getTypeDefinition(Construct theConstruct, String theName) {
        Map<String, Map> constructTypes = getConstructTypes(theConstruct);
        Map typeDef = constructTypes.get(theName);
        if (typeDef == null && this.parent != null) {
            return this.parent.getTypeDefinition(theConstruct, theName);
        }
        return typeDef;
    }

  public boolean hasType(Construct theConstruct, String theName) {
        Map<String, Map> constructTypes = getConstructTypes(theConstruct);
        boolean res = constructTypes.containsKey(theName);
        if (!res && this.parent != null) {
            res = this.parent.hasType(theConstruct, theName);
        }
        return res;
    }

    protected Map<String, Map> getConstructTypes(Construct theConstruct) {
        Map<String, Map> constructTypes = this.types.get(theConstruct);
        if (null == constructTypes) {
            throw new RuntimeException("Something worse is cooking here!",
                            new CatalogException("No types for construct " + theConstruct));
        }
        return constructTypes;
    }

    private Iterator<Map.Entry<String,Map>>
                                            typesIterator(Construct theConstruct) {
        List<Map.Entry<String,Map>> constructTypes =
                                                new ArrayList<>(
                                                    this.types.get(theConstruct).entrySet());
        Collections.reverse(constructTypes);
        return (this.parent == null)
                            ? constructTypes.iterator()
                            : Iterators.concat(constructTypes.iterator(),
                                                                 this.parent.typesIterator(theConstruct));
    }


    // this will iterate through the type hierarchy for the given type, included.
    public Iterator<Map.Entry<String,Map>>
                                        hierarchy(Construct theConstruct, final String theName) {
        return Iterators.filter(typesIterator(theConstruct),
                             new Predicate<Map.Entry<String,Map>>() {
                               Object next = theName;
                               public boolean apply(Map.Entry<String,Map> theEntry) {
                                 if (next != null && next.equals(theEntry.getKey())) {
                                   next = theEntry.getValue().get(DERIVED_FROM);
                                   return true;
                                 } else {
                                     return false;
                                 }
                               }
                             });
   }
   
     public boolean isDerivedFrom(Construct theConstruct, String theType, String theBaseType) {

        Iterator<Map.Entry<String,Map>> hierachyIterator =
                                                                                    hierarchy(theConstruct, theType);
        while (hierachyIterator.hasNext())	{
            Map.Entry<String,Map> typeDef = hierachyIterator.next();

            if (typeDef.getKey().equals(theBaseType)) {
                return true;
            }
        }
        return  false;
     }

    /* We go over the type hierarchy and retain only an iterator over the
     * elements of the given facet for each type in the hierarchy.
     * We concatenate these iterators and filter out duplicates.
     * TODO: cannot just filter out duplicates - a redefinition can refine the one in the base construct so we
     * should merge them!
     */
    public Iterator<Map.Entry> facets(Construct theConstruct, final Facet theFacet, final String theName) {
        return
            Iterators.filter(
                Iterators.concat(
                    Iterators.transform(
                        hierarchy(theConstruct, theName),
                            (Function<Map.Entry<String, Map>, Iterator<Map.Entry>>) theEntry -> {
                                Map m = (Map)theEntry.getValue().get(theFacet.name());
                                return m == null
                                                    ? Collections.emptyIterator()
                                                    : m.entrySet().iterator();
                            }
                    )
                ),
        new Predicate<Map.Entry>() {
                    Set insts = new HashSet();
                    public boolean apply(Map.Entry theEntry) {
                        return !insts.contains(theEntry.getKey());
                    }
                }
            );
    }

    //no need to specify a construct, only nodes can have requirements
    public Iterator<Map.Entry> requirements(final String theName) {
        return
            Iterators.concat(
                Iterators.transform(
                    hierarchy(Construct.Node, theName),
                        theEntry -> {
                            List<Map> l = (List<Map>)theEntry.getValue().get("requirements");
                            return l == null
                                                ? Collections.emptyIterator()
                                                : Iterators.concat(
                                                        Iterators.transform(
                                                            l.iterator(),
                                                                (Function<Map, Iterator<Map.Entry>>) theEntry1 -> theEntry1.entrySet().iterator()
                                                        )
                                                    );
                            }
                )
                );
    }

    /* Example: find the definition of property 'port' of the node type
     * tosca.nodes.Database (properties being a facet of the node construct)
     *
     * Note: the definition of a facet is cumulative, i.e. more specialized
     * definitions contribute (by overwriting) to the
     */
    public Map getFacetDefinition(Construct theConstruct,
                                                                String theConstructTypeName,
                                                                Facet theFacet,
                                                                String theName) {
        Map def = null;
        Iterator<Map.Entry<String,Map>> ti = hierarchy(theConstruct, theConstructTypeName);
        while (ti.hasNext()) {
            //this is where requirements would yield a List ..
            Map<String,Map> fset = (Map<String,Map>)ti.next().getValue().get(theFacet.name());
            if (fset != null) {
                def = def == null ? fset.get(theName)
                                                    : mergeDefinitions(def, fset.get(theName));
            }
        }
        return def;
    }

    public Map getRequirementDefinition(Construct theConstruct,
                                                                            String theConstructTypeName,
                                                                            String theName) {
        Iterator<Map.Entry<String,Map>> ti = hierarchy(theConstruct, theConstructTypeName);
        while (ti.hasNext()) {
            //this is where requirements yield a List ..
            List<Map> reqs = (List<Map>)ti.next().getValue().get("requirements");

            if(reqs!=null) {
                for (Map req: reqs) {
                    Map.Entry reqe = (Map.Entry)req.entrySet().iterator().next();
                    if (theName.equals(reqe.getKey())) {
                        return (Map)reqe.getValue();
                    }
                }
            } else {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Avoiding adding requirment block since it doesn't exists on the template....");
            }
        }
        return null;
    }

  /* */
  private EnumMap<Construct,Map<String,Map>> getTemplates(Target theTarget) {
    EnumMap<Construct, Map<String,Map>> targetTemplates = templates.get(theTarget);
        if (targetTemplates == null) {
            targetTemplates = new EnumMap<>(Construct.class);
            targetTemplates.put(Construct.Data, new LinkedHashMap<>());
            targetTemplates.put(Construct.Relationship, new LinkedHashMap<>());
            targetTemplates.put(Construct.Node, new LinkedHashMap<>());
            targetTemplates.put(Construct.Group, new LinkedHashMap<>());
            targetTemplates.put(Construct.Policy, new LinkedHashMap<>());

            templates.put(theTarget, targetTemplates);
        }
        return targetTemplates;
    }

    public Map<String,Map> getTargetTemplates(Target theTarget, Construct theConstruct) {
        return getTemplates(theTarget).get(theConstruct);
    }

    public void addTemplate(Target theTarget, Construct theConstruct, String theName, Map theDef)
                                                                                                    throws CatalogException {
    Map<String, Map> constructTemplates = getTargetTemplates(theTarget, theConstruct);
        if (null == constructTemplates) {
      throw new CatalogException("No such thing as " + theConstruct + " templates");
        }
    if (constructTemplates.containsKey(theName)) {
      throw new CatalogException(theConstruct + " template '" + theName + "' re-declaration");
    }
    constructTemplates.put(theName, theDef);
  }

  public boolean hasTemplate(Target theTarget, Construct theConstruct, String theName) {
    Map<String, Map> constructTemplates = getTargetTemplates(theTarget, theConstruct);
    return constructTemplates != null && 
            constructTemplates.containsKey(theName);
  }

  public Map getTemplate(Target theTarget, Construct theConstruct, String theName) {
    Map<String, Map> constructTemplates = getTargetTemplates(theTarget, theConstruct);
    if (constructTemplates != null) {
        return constructTemplates.get(theName);
    } else {
        return null;
    }
  }

    public static Map mergeDefinitions(Map theAggregate, Map theIncrement) {
        if (theIncrement == null) {
            return theAggregate;
        }

        for(Map.Entry e: (Set<Map.Entry>)theIncrement.entrySet()) {
            theAggregate.putIfAbsent(e.getKey(), e.getValue());
        }
        return theAggregate;
    }


    /*
   * theParent contains an 'include/import' statement pointing to the Target
   */
    public boolean addTarget(Target theTarget, Target theParent) {
        boolean cataloged = targets.containsKey(theTarget.getLocation());

        if(!cataloged) {
             targets.put(theTarget.getLocation(), theTarget);
        }

        if (theParent != null) {
            imports.put(theParent, theTarget, Boolean.TRUE);
        }

        return !cataloged;
    }

    public Target getTarget(URI theLocation) {
        return targets.get(theLocation);
    }

    public Collection<Target> targets() {
        return targets.values();
    }

    /* Targets that no other targets depend on */
    public Collection<Target> topTargets() {
        return targets.values()
                        .stream()
                            .filter(t -> !imports.containsColumn(t))
                            .collect(Collectors.toList());

    }

    public String importString(Target theTarget) {
        return importString(theTarget, "  ");
    }

    private String importString(Target theTarget, String thePrefix) {
        StringBuilder sb = new StringBuilder("");
        Map<Target,Boolean> parents = imports.column(theTarget);
        if (parents != null) {
            for (Target p: parents.keySet()) {
                sb.append(thePrefix)
                    .append("from ")
                  .append(p.getLocation())
                    .append("\n")
                    .append(importString(p, thePrefix + "  "));
            }
            //we only keep the positive relationships
        }
        return sb.toString();
    }

    /* */
    private class TargetComparator implements Comparator<Target> {

        /* @return 1 if there is a dependency path from TargetOne to TargetTwo, -1 otherwise  */
        public int compare(Target theTargetOne, Target theTargetTwo) {
            if (hasPath(theTargetTwo, theTargetOne)) {
                return -1;
            }

            if (hasPath(theTargetOne, theTargetTwo)) {
                return 1;
            }

            return 0;
        }

        boolean hasPath(Target theStart, Target theEnd) {
            Map<Target,Boolean> deps = imports.row(theStart);
            if (deps.containsKey(theEnd)) {
                return true;
            }
            for (Target dep: deps.keySet()) {
                if (hasPath(dep, theEnd)) {
                    return true;
                }
            }
            return false;
        }
    }

    public Collection<Target> sortedTargets() {
        List keys = new ArrayList(this.targets.values());
        Collections.sort(keys, new TargetComparator());
        return keys;
    }

    public static void main(String[] theArgs) throws Exception {

        Catalog cat = new Catalog();

        Target a = new Target("a", new URI("a")),
                b = new Target("b", new URI("b")),
                c = new Target("c", new URI("c")),
                d = new Target("d", new URI("d"));

        cat.addTarget(a, null);
        cat.addTarget(b, null);
        cat.addTarget(c, null);
        cat.addTarget(d, null);

        cat.addTarget(b, c);
        cat.addTarget(a, c);
        cat.addTarget(c, d);
        cat.addTarget(a, b);

        for (Target t: cat.sortedTargets()) {
            debugLogger.log(LogLevel.DEBUG, Catalog.class.getName(), t.toString());
        }

        Catalog root = new Catalog();
        root.addType(Construct.Node, "_a", Collections.emptyMap());
        root.addType(Construct.Node, "__a", Collections.singletonMap(DERIVED_FROM, "_a"));
        root.addType(Construct.Node, "___a", Collections.singletonMap(DERIVED_FROM, "_a"));

        Catalog base = new Catalog(root);
        base.addType(Construct.Node, "_b", Collections.singletonMap(DERIVED_FROM, "__a"));
        base.addType(Construct.Node, "__b", Collections.singletonMap(DERIVED_FROM, "_b"));
        base.addType(Construct.Node, "__b_", Collections.singletonMap(DERIVED_FROM, "_a"));

        if (theArgs.length > 0) {
            Iterator<Map.Entry<String, Map>> ti =
                                                                    base.hierarchy(Construct.Node, theArgs[0]);
            while (ti.hasNext()) {
                debugLogger.log(LogLevel.DEBUG, Catalog.class.getName(), "> {}", ti.next().getKey());
            }
        }
    }
}

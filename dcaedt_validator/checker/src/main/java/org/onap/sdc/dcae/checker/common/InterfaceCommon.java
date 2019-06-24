package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.checker.*;

import java.util.Iterator;
import java.util.Map;

import static org.onap.sdc.dcae.checker.common.ConstCommon.INPUTS;
import static org.onap.sdc.dcae.checker.common.ConstCommon.INTERFACES;
import static org.onap.sdc.dcae.checker.common.ConstCommon.WAS_DEFINED_FOR_THE_NODE_TYPE;

public class InterfaceCommon extends BaseCommon {
    private static InterfaceCommon instance;

    public synchronized static void init(IChecker checker) {
        if (instance == null)
        {
            instance = new InterfaceCommon();
            instance.setChecker(checker);
        }
    }

    public synchronized static InterfaceCommon getInstance() {
        if (instance == null)
        {
            errLogger.log(LogLevel.ERROR, InterfaceCommon.class.getSimpleName(),"Need to call init before");
        }
        return instance;
    }

    private InterfaceCommon() {}

    private IChecker checker;

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
    public void check_type_interface_definition(
            String theName, Map theDef, Checker.CheckContext theContext, Catalog catalog) {
        TypeCommon typeCommon = TypeCommon.getInstance();
        theContext.enter(theName);
        try {
            if (!CheckCommon.getInstance().checkDefinition(theName, theDef, theContext)) {
                return;
            }

            if (!typeCommon.checkType(Construct.Interface, theDef, theContext, catalog)) {
                return;
            }

            if (theDef.containsKey(INPUTS)) {
                checker.check_inputs((Map<String, Map>) theDef.get(INPUTS), theContext);
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkTemplateInterfacesDefinition(
            Map<String, Map> theInterfaces,
            Checker.CheckContext theContext,
            Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext.enter(INTERFACES);
        try {
            if (!CheckCommon.getInstance().checkDefinition(INTERFACES, theInterfaces, theContext)) {
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
                Map interfaceDef = facetCommon.findTypeFacetByName(Construct.Node, nodeType,
                        Facet.interfaces, interfaceName, catalog);

                if (interfaceDef == null) {
                    /* this is subject to augmentation: this could be a warning but not an error */
                    theContext.addError("No interface " + interfaceName + WAS_DEFINED_FOR_THE_NODE_TYPE + nodeType, null);
                    continue;
                }

                checkTemplateInterfaceDefinition(
                        interfaceName, ie.getValue(), interfaceDef, theContext, catalog);
            }
        } finally {
            theContext.exit();
        }
    }

    public void checkTemplateInterfaceDefinition(
            String theInterfaceName,
            Map theAssignment,
            Map theDefinition,
            Checker.CheckContext theContext,
            Catalog catalog) {
        FacetCommon facetCommon = FacetCommon.getInstance();
        theContext.enter(theInterfaceName, Construct.Interface);
        try {
            //check the assignment of the common inputs
            facetCommon.checkFacet(Construct.Interface,
                    theAssignment,
                    (String) theDefinition.get("type"),
                    Facet.inputs,
                    theContext,
                    catalog);
        } finally {
            theContext.exit();
        }
    }

    public void checkMapTypeInterfaceDefinition(Checker.CheckContext theContext, Map<String, Map> interfaces, Catalog catalog) {
        if (interfaces != null) {
            try {
                theContext.enter(INTERFACES);
                for (Iterator<Map.Entry<String, Map>> i =
                     interfaces.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<String, Map> e = i.next();
                    check_type_interface_definition(
                            e.getKey(), e.getValue(), theContext, catalog);
                }
            } finally {
                theContext.exit();
            }
        }
    }

    public void checkInterfaceTypeDefinition(String theName,
                                              Map theDefinition,
                                              Checker.CheckContext theContext) {
        theContext.enter(theName, Construct.Interface);
        try {
            CheckCommon.getInstance().checkDefinition(theName, theDefinition, theContext);
        } finally {
            theContext.exit();
        }
    }

    void setChecker(IChecker checker) {
        this.checker = checker;
    }
}

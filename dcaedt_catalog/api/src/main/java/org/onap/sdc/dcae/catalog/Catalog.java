package org.onap.sdc.dcae.catalog;

import java.net.URI;

import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONObject;
import org.onap.sdc.dcae.catalog.commons.Action;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Futures;
import org.onap.sdc.dcae.catalog.commons.Proxies;


import org.json.JSONArray;

public interface Catalog {


    URI getUri();

    <T> T proxy(JSONObject theData, Class<T> theType);


    /* Base class for all Catalog objects. */
    interface Element<T extends Element<T>> {

        default Class<T> selfClass() {
            return (Class<T>)getClass().getInterfaces()[0];
        }

        Catalog catalog();

        String id();

        /**
         * Direct access to the underlying JSON object.
         * Warning: Modifications to the JSON object are reflected in the Element.
         */
        JSONObject data();

        /* Allows for typed deep exploration of the backing JSON data structure
         * @arg theName name of a JSON entry ; It must map another JSONObject.
         * @arg theType the expected wrapping catalog artifact type
         * @return the JSON entry wrapped in the specified type
         */
        default <E extends Element<E>> E element(String theName, Class<E> theType) {
            JSONObject elemData = data().optJSONObject(theName);
            if (elemData == null) {
                return null;
            }
            else {
                return catalog().proxy(elemData, theType);
            }
        }

        /* Similar to {@link #element(String,Class)} but for collection wrapping. */
        default <E extends Elements> E elements(String theName, Class<E> theType) {
            JSONArray elemsData = data().optJSONArray(theName);
            if (elemsData == null) {
                return null;
            }
            else {
                Class etype = Proxies.typeArgument(theType);
                Elements elems;
                try {
                    elems = theType.newInstance();
                }
                catch (ReflectiveOperationException rox) {
                    throw new RuntimeException("Failed to instantiate " + theType, rox);
                }

                try{
                    for (Iterator i = elemsData.iterator(); i.hasNext();) {
                        JSONObject elemData = (JSONObject)i.next();
                        elems.add(catalog().proxy(elemData,	etype));
                    }
                }
                catch(Exception e){
                    throw new RuntimeException("Failed to fetch json data ", e);
                }
                return (E)elems;
            }
        }
    }

    /* Base class for all collections of elements. */
    class Elements<T extends Element>
                                                extends LinkedList<T> {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (Element el: this) {
                sb.append(el.selfClass().getSimpleName())
                    .append("(")
                    .append(el.data())
                    .append("),");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /*
     * We need this contraption in order to store a mix of Folders and CatalogItem
     * instances (Elements in self is not good because it is defined around a
     * type variable so we cannot use reflection to determine the type at runtime
     * - generics are resolved compile time)
     */
    class Mixels extends Elements<Element> {}

    interface Item<T extends Item<T>> extends Element<T> {
        String name();
        String description();
    }

    /*
     * Collection of catalog items.
     */
    class Items extends Elements<Item> {}

    interface Folder extends Element<Folder> {

        String name();

        String description();

        String itemId();

        default Future<Items> items() {
            Items i = elements("items", Items.class);
            if (i != null) {
                return Futures.succeededFuture(i);
            }
            else {
                return Futures.advance(catalog().folder(itemId())
                                .withItems()
                                .execute(),
                        folder -> folder.elements("items", Items.class));
            }
        }
    }

    class Folders extends Elements<Folder> {}

    //no predefined properties here
    interface Annotation extends Element<Annotation> {}

  class Annotations extends Elements<Annotation> {
  }

    /**
     * A TOSCA teamplate.
     * When a deep loading method is used to obtain a Template its collection
     * of inputs and nodes will be immediately available (and 'cached' within
     * the backing JSON object). It can be retrieved through a call to
     * {@link Element#elements(String,Class)} as in:
     *	elements("inputs", Inputs.class)
     * or
     *  elements("nodes", Nodes.class)
     *
     * The same result will be obtained through one of the methods of the
     * navigation interface. in this case
     * the result does not become part of the backing JSONObject.
     */
    interface Template extends Element<Template> {
        String name();

        String version();

        String description();
    }

    /**
     * Collection of {@link Catalog.Template template} instances.
     */
    class Templates extends Elements<Template> {
    }


    /**
     * A TOSCA type declaration.
     */
    interface Type extends Element<Type> {
        String name();
    }

    /**
     * Collection of {@link Catalog.Type type} instances.
     */
    class Types extends Elements<Type> {
    }


    interface TemplateAction extends Action<Template> {

        TemplateAction withInputs();

        TemplateAction withOutputs();

        TemplateAction withNodes();

        TemplateAction withNodeProperties();

        TemplateAction withNodeRequirements();

        TemplateAction withNodePropertiesAssignments();

        TemplateAction withNodeCapabilities();

        TemplateAction withNodeCapabilityProperties();

        TemplateAction withNodeCapabilityPropertyAssignments();

        TemplateAction withPolicies();

        TemplateAction withPolicyProperties();

        TemplateAction withPolicyPropertiesAssignments();

        @Override
         Future<Template> execute();
    }

    interface TypeAction extends Action<Type> {

        TypeAction withHierarchy();

        TypeAction withRequirements();

        TypeAction withCapabilities();

        @Override
        Future<Type> execute();
    }

    interface FolderAction extends Action<Folder> {

        FolderAction withItems();

        FolderAction withItemAnnotations();

        FolderAction withItemModels();

        FolderAction withParts();

        FolderAction withPartAnnotations();

        @Override
        Future<Folder> execute();
    }

    interface ItemAction<T extends Item> extends Action<T> {

        ItemAction<T> withModels();

        @Override
        Future<T> execute();

    }

    Future<Folders> rootsByLabel(String theLabel);

    FolderAction folder(String theFolderId);

    <T extends Item> ItemAction<T> item(String theItemId);

    TemplateAction template(String theTemplateId);

    TypeAction type(String theNamespace, String theTypeName);
}

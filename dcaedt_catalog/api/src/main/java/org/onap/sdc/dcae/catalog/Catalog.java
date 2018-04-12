package org.onap.sdc.dcae.catalog;

import java.net.URI;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.EnumSet;

import org.json.JSONObject;
import org.onap.sdc.dcae.catalog.commons.Action;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.Futures;
import org.onap.sdc.dcae.catalog.commons.Proxies;


import org.json.JSONArray;

/*
 *
 */
public interface Catalog {


	public abstract URI getUri();

	public abstract String namespace();

	public abstract boolean same(Catalog theCatalog);

	public abstract <T> T proxy(JSONObject theData, Class<T> theType);
		
	
	/* Base class for all Catalog objects.
	 */
	public static interface Element<T extends Element<T>> {
	
		/**
		 * provide a typed 'self' reference
		 */
		public default T self() { return (T)this; }

		/**
		 */
		public default Class<T> selfClass() {
			return (Class<T>)getClass().getInterfaces()[0];
		}

		/* */
		public Catalog catalog();

		/**
		 */
		public String id();

		/**
		 * Direct access to the underlying JSON object.
		 * Warning: Modifications to the JSON object are reflected in the Element.
		 */
		public JSONObject data();

		/**
		 * Provides the labels of the artifacts (we use labels to type/classify the 
		 * neo4j artifacts, nodes and edges.
		 * Currently not all queries retrieve the labels.
		 */
		public String[] labels();

		/* Allows for typed deep exploration of the backing JSON data structure 
		 * <pre>
		 * {@code
		 * element("type", Type.class);
		 * }
		 * </pre>
		 *
		 * @arg theName name of a JSON entry ; It must map another JSONObject.
		 * @arg theType the expected wrapping catalog artifact type
		 * @return the JSON entry wrapped in the specified type
		 */
		public default <E extends Element<E>> E element(String theName, Class<E> theType) {
			JSONObject elemData = data().optJSONObject(theName);
			if (elemData == null)
				return null;
			else
				return catalog().proxy(elemData, theType);
		}
	
		/* Similar to {@link #element(String,Class)} but for collection wrapping.
		 * Example:
		 * <pre>
		 * {@code
		 * element("nodes", Nodes.class);
		 * }
		 * </pre>
		 */
		public default <E extends Elements> E elements(String theName, Class<E> theType) {
																															//throws ReflectiveOperationException {
			JSONArray elemsData = data().optJSONArray(theName);
			if (elemsData == null) {
				return null;
			}
			else {
				Class etype = Proxies.typeArgument(theType);
				Elements elems = null;
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

		/*
		 */
		public default boolean same(Element theElem) {
			return this.catalog().same(theElem.catalog()) &&
						 this.id().equals(theElem.id());
		}
	}

	/*
	 * Base class for all collections of elements.
	 */
	public static class Elements<T extends Element> 
												extends LinkedList<T> {

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
	public static class Mixels extends Elements<Element> {
	}

	/*
	 */
	public static interface Item<T extends Item<T>> extends Element<T> {

		public String name();

		public String description();

		/* catalog item native identifier */
		public String itemId();

		/* similar to @ItemAction#withModels
		 */
		default public Future<Templates> models() {
			Templates t = elements("models", Templates.class);
			if (t != null)
				return Futures.succeededFuture(t);
			else
				return Futures.advance(catalog().item(itemId())
																					.withModels()
																					.execute(),
															 item -> (Templates)item.elements("models", Templates.class));
		}
		
		/* similar to @ItemAction#withAnnotations
		 */
		default public Future<Annotations> annotations() {
			Annotations a = elements("annotations", Annotations.class);
			if (a != null)
				return Futures.succeededFuture(a);
			else
				return Futures.advance(catalog().item(itemId())
																					.withAnnotations()
																					.execute(),
															 item -> (Annotations)item.elements("annotations", Annotations.class));
		} 
	}

	/*
	 * Collection of catalog items.
	 */
	public static class Items extends Elements<Item> {
	}

	/*
	 */
  public static interface Folder extends Element<Folder> {

		public String name();

		public String description();
		
		public String itemId();

		 /* the namespace is immutable */
		public default String namespace() {
			return catalog().namespace();
		}

		/*
		 */
		default public Future<Items> items() {
			Items i = elements("items", Items.class);
			if (i != null)
				return Futures.succeededFuture(i);
			else
				return Futures.advance(catalog().folder(itemId())
																					.withItems()
																					.execute(),
															 folder -> (Items)folder.elements("items", Items.class));
		}

		/*
		 */
		default public Future<Folders> parts() {
			Folders f = elements("parts", Folders.class);
			if (f != null)
				return Futures.succeededFuture(f);
			else
				return Futures.advance(catalog().folder(itemId())
																					.withParts()
																					.execute(),
															 folder -> (Folders)folder.elements("parts", Folders.class));
		}

		/*
		 */
		public Future<Folders> partof(); 
	
	}


	public static class Folders extends Elements<Folder> {
	}

	//no predefined properties here
  public static interface Annotation extends Element<Annotation> {

    public default String namespace() {
      return catalog().namespace();
    }
  }

  public static class Annotations extends Elements<Annotation> {
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
	 * navigation interface, {@link #inputs()} or {@link #nodes()}; in this case
	 * the result does not become part of the backing JSONObject.
	 */
	public static interface Template extends Element<Template> {

		public String name();

		public String version();

		public String description();

	}
	
	/**
	 * Collection of {@link Catalog.Template template} instances.
	 */
	public static class Templates extends Elements<Template> {
	}


	/**
	 * A TOSCA type declaration.
	 */
	public interface Type extends Element<Type> {
		
		public String name();

		/**
		 * Allows navigation to the parent {@link Catalog.Type type}, if any.
		 */
		public Future<Type> derivedfrom();
			
	}
	
	/**
	 * Collection of {@link Catalog.Type type} instances.
	 */
	public static class Types extends Elements<Type> {
	}
	

	public static interface TemplateAction extends Action<Template> {

		public TemplateAction withInputs(); 
		
		public TemplateAction withOutputs(); 

		public TemplateAction withNodes(); 
	
		public TemplateAction withNodeProperties(); 
	
		public TemplateAction withNodeRequirements(); 
		
		public TemplateAction withNodePropertiesAssignments(); 

		public TemplateAction withNodeCapabilities(); 

		public TemplateAction withNodeCapabilityProperties(); 
		
		public TemplateAction withNodeCapabilityPropertyAssignments();
		
		public TemplateAction withPolicies();

		public TemplateAction withPolicyProperties(); 
		
		public TemplateAction withPolicyPropertiesAssignments(); 

		@Override
		public Future<Template> execute();

	}
	
	/*
	 */
	public static interface TypeAction extends Action<Type> {
		
		public TypeAction withHierarchy(); 
	
		public TypeAction withRequirements();
		
		public TypeAction withCapabilities(); 
	
		@Override
		public Future<Type> execute();

	}

	/*
	 */
	public static interface FolderAction extends Action<Folder> {

		public FolderAction withAnnotations();

		public FolderAction withAnnotations(String theSelector); 

	  public FolderAction withItems();

		public FolderAction withItemAnnotations();

		public FolderAction withItemAnnotations(String theSelector);

		public FolderAction withItemModels(); 

		public FolderAction withParts(); 
		
		public FolderAction withPartAnnotations();

		public FolderAction withPartAnnotations(String theSelector);

		@Override
		public Future<Folder> execute();
	}

	/*
	 */
	public static interface ItemAction<T extends Item> extends Action<T> {

		public ItemAction<T> withModels();

		public ItemAction<T> withAnnotations();
		
		@Override
		public Future<T> execute();

	}

	/**
	 */
	public abstract Future<Folders> roots();

	/**
	 */
	public abstract Future<Folders> rootsByLabel(String theLabel);

	/**
   */
	public abstract Future<Mixels> lookup(JSONObject theSelector); 
	
	public abstract Future<Mixels> lookup(String theAnnotation, JSONObject theSelector); 
	
	/**
   */
	public abstract FolderAction folder(String theFolderId);

	/**
	 */
	public abstract <T extends Item> ItemAction<T> item(String theItemId);

	/**
   */
	public abstract TemplateAction template(String theTemplateId); 

	/**
   */
	public abstract TypeAction type(String theNamespace, String theTypeName);



}

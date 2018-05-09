/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package org.onap.sdc.dcae.catalog.engine;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.onap.sdc.dcae.catalog.Catalog.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONObject;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.FutureHandler;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.json.JSONArray;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * All requests body:
 *  {
 *		"id": optional request uuid,
 *		"timestamp": optional request timestamp,
 *		"catalog": optional catalog uri,
 *		"timeout": optional timeout - default 0 no time limit
 *  }
 *
 * All responses body:
 * 	{ "data": {},
 *    "error": {}
 *  }
 *
 * If a non-2xx reponse is provided and error occured at catalog engine processing level.
 * If error has occured in data retrieval then the response error object is not empty.
 *
 * Available uris
 *   /catalog
 *		/elements	: roots of the catalog; request body is optional but can specify a label under 'startingLabel'
 *								response contains items under 'data/elements'
 *		/{itemId}/elements : catalog descendants of the given item, possibly a mix of folders and items
 *								response contains items under 'data/elements'
 *		/lookup.by.name : lookup catalog entries by name.
									The request body must contain a 'selector' entry with a 'name' criteria
 *								response contains items under 'data/elements'
 *                Example: '{"id":"5d0c1cf4-11aa-11e6-a148-3e1d05defe78","selector":{"name":"Firewall"}}'
 *		/lookup.by.annotation
									The request body must contain a 'annotation' entry and it can have a 'selector' entry
 *								with a multiple annotation property criteria
 *								response contains items under 'data/elements'
 *		/lookup.by.model.property.value :
 *								The request must contain a "selector" entry as a JSONObject containing the selection criteria
 *								(property name with values) and desired output properties (null values). Example:
 *									"selector":{"att-part-number":"L-CSR-50M-APP-3Y",
 *															"management-option":"ATT",
 *															"vnf-type":null,
 *															"vendor-model":null}
 *								response contains items under 'data/elements'
 *		/referents : provides generic recommendations
 *								response contains items under 'data/elements'
 *		/{itemId}/referents : provides recommendations for the given item
 *								response contains items under 'data/elements'
 *		/{itemId}/model : retrieves the TOSCA model for the item with the given id
 *								response under 'data/model'
 *
 */


@RestController
//@RequestMapping(value="/catalog",method=RequestMethod.POST)
@CrossOrigin(origins="*")
//@ConfigurationProperties(prefix="catalogController")
public class CatalogController {

	private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();


	@Autowired
	private SystemProperties systemProperties;


	private boolean	enableCORS = false;
	private URI 		defaultCatalog;
	private static Map<URI, Catalog> catalogs = new HashMap<URI, Catalog>();


	public void setDefaultCatalog(URI theUri) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "set default catalog at {}", theUri);
		this.defaultCatalog = theUri;
	}

	public void setEnableCORS(boolean doEnable) {
		this.enableCORS = doEnable;
	}

//	@RequestMapping(value="/elements",method={RequestMethod.POST, RequestMethod.GET}, produces = "application/json")
//	public DeferredResult<CatalogResponse> items(@RequestBody(required=false) ItemsRequest theRequest) {
//
//		final ItemsRequest request = (theRequest == null) ? ItemsRequest.EMPTY_REQUEST : theRequest;
//
//		Catalog catalog = getCatalog(request.getCatalog());
//		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());
//
//		catalog.rootsByLabel(request.getStartingLabel())
//		//catalog.roots()
//			.setHandler(
//				new CatalogHandler<Folders>(request, result) {
//					public CatalogResponse handleData(Folders theFolders) {
//						JSONArray ja = new JSONArray();
//						if (theFolders != null) {
//							for (Folder folder : theFolders) {
//								ja.put(patchData(catalog, folder.data()));
//							}
//						}
//						CatalogResponse response = new CatalogResponse(this.request);
//						response.data()
//										.put("elements", ja);
//						return response;
//					}
//			});
//		return result;
//	}
//
//	@RequestMapping(value="/{theItemId}/elements",method={RequestMethod.POST,RequestMethod.GET}, produces = "application/json")
//	public DeferredResult<CatalogResponse> items(@RequestBody(required=false) ItemsRequest theRequest, @PathVariable String theItemId) {
//
//		final ItemsRequest request = (theRequest == null) ? ItemsRequest.EMPTY_REQUEST : theRequest;
//
//		Catalog catalog = getCatalog(request.getCatalog());
//		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());
//
//		catalog
////			.fetchFolderByItemId(theItemId)
//			.folder(theItemId)
//			.withParts()
//			.withPartAnnotations()
//			.withItems()
//			.withItemAnnotations()
//			.withItemModels()
//			.execute()
//			.setHandler(
//				new CatalogHandler<Folder>(request, result) {
//					public CatalogResponse handleData(Folder theFolder) {
//						CatalogResponse response = new CatalogResponse(this.request);
//						if (theFolder == null) {
//							return response;
//						}
//
//						try {
//							Elements folders = theFolder.elements("parts",Folders.class);
//							if (folders != null) {
//								for (Object folder: folders) {
//									patchData(catalog, ((Element)folder).data());
//									//lots of ephemere proxies created here ..
//									Elements annotations =
//											((Element)folder).elements("annotations", Annotations.class);
//									if (annotations != null) {
//										for (Object a: annotations) {
//											patchData(catalog, ((Annotation)a).data());
//										}
//									}
//								}
//							}
//							Elements items = theFolder.elements("items",Items.class);
//							if (items != null) {
//								for (Object i: items) {
//									patchData(catalog, ((Element)i).data());
//									//lots of ephemere proxies created here ..
//									Elements annotations =
//											((Element)i).elements("annotations", Annotations.class);
//									if (annotations != null) {
//										for (Object a: annotations){
//											patchData(catalog, ((Annotation)a).data());
//										}
//									}
//								}
//							}
//						}
//						catch(Exception x) {
//x.printStackTrace();
//							return new CatalogError(this.request, "", x);
//						}
//
//						response.data()
//										.put("element", theFolder.data());
//						return response;
//					}
//				});
//
//		return result;
//	}
//
//	@RequestMapping(value="/lookup.by.name",method=RequestMethod.POST, produces = "application/json")
//  public DeferredResult<CatalogResponse> elementsByName(@RequestBody ElementsLookup theRequest) {
//
//		Catalog catalog = getCatalog(theRequest.getCatalog());
//		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(theRequest.getTimeout());
//
//		catalog
//			.lookup(new JSONObject(theRequest.getSelector()))
//			.setHandler(
//				new CatalogHandler<Mixels>(theRequest, result) {
//					public CatalogResponse handleData(Mixels theElems) {
//						JSONArray ja = new JSONArray();
//						if (theElems != null) {
//							for (Object elem : theElems) {
//								ja.put(patchData(catalog, ((Element)elem).data()));
//							}
//						}
//						CatalogResponse response = new CatalogResponse(theRequest);
//						response.data()
//										.put("elements", ja);
//						return response;
//					}
//				});
//
//		return result;
//	}
//
//	@RequestMapping(value="/lookup.by.annotation",method=RequestMethod.POST, produces = "application/json")
//  public DeferredResult<CatalogResponse> elementsByAnnotation(@RequestBody ElementsLookup theRequest) {
//
//		Catalog catalog = getCatalog(theRequest.getCatalog());
//		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(theRequest.getTimeout());
//
//		catalog
//			.lookup(theRequest.getAnnotation(),
//						 	new JSONObject(theRequest.getSelector()))
//			.setHandler(
//				new CatalogHandler<Mixels>(theRequest, result) {
//					public CatalogResponse handleData(Mixels theElems) {
//						JSONArray ja = new JSONArray();
//						if (theElems != null) {
//							for (Object elem : theElems) {
//								ja.put(patchData(catalog, ((Element)elem).data()));
//							}
//						}
//						CatalogResponse response = new CatalogResponse(this.request);
//						response.data()
//										.put("elements", ja);
//						return response;
//					}
//				});
//
//		return result;
//	}

	/**
   * NeoCatalog specific
   *//*
	@RequestMapping(value="/lookup.by.model.property.value",method=RequestMethod.POST, produces = "application/json")
  public DeferredResult<CatalogResponse> elementsByModelPropertyValue(@RequestBody ElementsLookup theRequest) {

		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(theRequest.getTimeout());

		NeoCatalog catalog = asNeo(getCatalog(theRequest.getCatalog()));
		if (catalog == null) {
			result.setErrorResult(
				new CatalogError(
					theRequest,"The selected catalog is not capable of handling this request (lookup.by.model.property.value)"));
			return result;
		}

		catalog
			.lookupItemsByToscaNodePropertyValue(theRequest.getJSONSelector())
			.setHandler(
				new CatalogHandler<Items>(theRequest, result) {
					public CatalogResponse handleData(Items theItems) {
						JSONArray ja = new JSONArray();
						if (theItems != null) {
							for (Item item : theItems) {
								ja.put(patchData(catalog, item.data()));
							}
						}
						CatalogResponse response = new CatalogResponse(this.request);
						response.data()
										.put("elements", ja);
						return response;
					}
				});

		return result;
	}
*/
	/**
   * This follows the current convention that each item will have a single model
		2 stage
   */
//	@RequestMapping(value="/{theItemId}/model",method={RequestMethod.POST,RequestMethod.GET}, produces = "application/json")
//  //public DeferredResult<CatalogResponse> model(@RequestBody ElementRequest theRequest) {
//  public DeferredResult<CatalogResponse> model(@RequestBody(required=false) ElementRequest theRequest, @PathVariable String theItemId) {
//		final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;
//
//		Catalog catalog = getCatalog(request.getCatalog());
//		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());
//
//		catalog
////			.fetchItemByItemId(/*theRequest.getProductId()*/theItemId)
//			.item(theItemId)
//			.withModels()
//			.execute()
//			.setHandler(
//				new CatalogHandler<Item>(request, result) {
//					public CatalogResponse handleData(Item theItem) {
//						if (theItem == null) {
//							return new CatalogError(this.request, "No such item");
//						}
//						Templates models = null;
//						try {
//							models = (Templates)theItem.elements("models", Templates.class);
//						}
//						catch (Exception x) {
//							return new CatalogError(this.request, "Failed to decode templates from result", x);
//						}
//
//						if (models == null || models.size() == 0) {
//							return new CatalogError(this.request, "Item has no models");
//						}
//						if (models.size() > 1) {
//							return new CatalogError(this.request, "Item has more than one model !?");
//						}
//			try{
//						catalog.template(models.get(0).id())
//							.withInputs()
//							.withOutputs()
//							.withNodes()
//							.withNodeProperties()
//							.withNodePropertiesAssignments()
//							.withNodeRequirements()
//							.withNodeCapabilities()
//							.withNodeCapabilityProperties()
//							.withNodeCapabilityPropertyAssignments()
//							.withPolicies()
//						  .withPolicyProperties()
//						  .withPolicyPropertiesAssignments()
//							.execute()
//							.setHandler(
//								new CatalogHandler<Template>(this.request, this.result) {
//									public CatalogResponse handleData(Template theTemplate) {
//										CatalogResponse response = new CatalogResponse(this.request);
//										if (theTemplate != null) {
//											response.data()
//												.put("model", patchData(catalog, theTemplate.data()));
//										}
//										return response;
//									}
//								});
//				}
//				catch (Exception x) {
//					x.printStackTrace();
//				}
//						return null;
//					}
//				});
//
//		return result;
//	}

//	@RequestMapping(value="/{theItemId}/type/{theTypeName}",method={RequestMethod.POST,RequestMethod.GET}, produces = "application/json")
//  public DeferredResult<CatalogResponse> model(@RequestBody(required=false) ElementRequest theRequest, @PathVariable String theItemId, @PathVariable String theTypeName) {
//		final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;
//
//		Catalog catalog = getCatalog(request.getCatalog());
//		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());
//
//		catalog.type(theItemId, theTypeName)
//			.withHierarchy()
//			.withCapabilities()
//			.withRequirements()
//			.execute()
//			.setHandler(
//					new CatalogHandler<Type>(request, result) {
//									public CatalogResponse handleData(Type theType) {
//										CatalogResponse response = new CatalogResponse(this.request);
//										if (theType != null) {
//											response.data()
//												.put("type", patchData(catalog, theType.data()));
//										}
//										return response;
//									}
//								});
//
//		return result;
//	}

/*
	@RequestMapping(value="/referents",method=RequestMethod.POST, produces = "application/json")
  public DeferredResult<CatalogResponse> referents(@RequestBody(required=false) ElementRequest theRequest) {
		final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;
		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());

		NeoCatalog catalog = asNeo(getCatalog(theRequest.getCatalog()));
		if (catalog == null) {
			result.setErrorResult(
				new CatalogError(
					theRequest,"The selected catalog is not capable of handling this request (referents)"));
			return result;
		}

		catalog
			.defaultRecommendations()
			.setHandler(
				new CatalogHandler<Mixels>(request, result) {
					public CatalogResponse handleData(Mixels theElems) {
						JSONArray ja = new JSONArray();
						if (theElems != null) {
							for (Element elem : theElems) {
								ja.put(patchData(catalog, elem.data()));
							}
						}
						CatalogResponse response = new CatalogResponse(this.request);
						response.data()
										.put("elements", ja);
						return response;
					}
				});

		return result;
	}
*/

/*	@RequestMapping(value="/{theItemId}/referents",method=RequestMethod.POST, produces = "application/json")
  public DeferredResult<CatalogResponse> referents(@RequestBody(required=false) ElementRequest theRequest, @PathVariable String theItemId) {
		final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;
		DeferredResult<CatalogResponse> result = new DeferredResult<CatalogResponse>(request.getTimeout());

		NeoCatalog catalog = asNeo(getCatalog(theRequest.getCatalog()));
		if (catalog == null) {
			result.setErrorResult(
				new CatalogError(
					theRequest,"The selected catalog is not capable of handling this request (item referents)"));
			return result;
		}

		catalog
			.recommendationsForItemId(theItemId)
			.setHandler(
				new CatalogHandler<Mixels>(request, result) {
					public CatalogResponse handleData(Mixels theElems) {
						JSONArray ja = new JSONArray();
						if (theElems != null) {
							for (Element elem : theElems) {
								ja.put(patchData(catalog, elem.data()));
							}
						}
						CatalogResponse response = new CatalogResponse(this.request);
						response.data()
										.put("elements", ja);
						return response;
					}
				});

		return result;
	}
*/
	@PostConstruct
	public void initCatalog() {
		// Dump some info and construct our configuration objects
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "initCatalog");

		this.defaultCatalog = URI.create(systemProperties.getProperties().getProperty(DcaeBeConstants.Config.ASDC_CATALOG_URL));
		// Initialize default catalog connection
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "default catalog at {}", this.defaultCatalog);
		getCatalog(null);

			// Done
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "CatalogEngine started");
	}

	@PreDestroy
	public void cleanupCatalog() {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "destroyCatalog");
	}

	public Catalog getCatalog(URI theCatalogUri) {
		//TODO: Thread safety! Check catalog is alive!
		if (theCatalogUri == null)
			theCatalogUri = this.defaultCatalog;

		Catalog cat = catalogs.get(theCatalogUri);
		if (cat == null && theCatalogUri != null) {
			String scheme = theCatalogUri.getScheme();
			URI catalogUri = null;
			try {
				catalogUri = new URI(theCatalogUri.getSchemeSpecificPart() + "#" + theCatalogUri.getFragment());
			}
			catch (URISyntaxException urisx) {
				throw new IllegalArgumentException("Invalid catalog reference '" + theCatalogUri.getSchemeSpecificPart() + "'");
			}
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Build catalog for {}", catalogUri);

			if ("asdc".equals(scheme)) {
				cat = new ASDCCatalog(catalogUri);
			}
			else {
				return null;
			}

			catalogs.put(theCatalogUri, cat);
		}
		return cat;
	}

/*	private NeoCatalog asNeo(Catalog theCatalog) {
		try {
			return (NeoCatalog)theCatalog;
		}
		catch (ClassCastException ccx) {
			return null;
		}
	}*/

	public JSONObject patchData(Catalog theCat, JSONObject theData) {
		theData.put("catalog", theCat.getUri());
		theData.put("catalogId", theData.optLong("id"));
		theData.put("id", theData.optLong("itemId"));
		return theData;
	}

	public abstract class CatalogHandler<T> implements FutureHandler<T> {

		protected DeferredResult result;
		protected CatalogRequest request;

		public CatalogHandler(CatalogRequest theRequest, DeferredResult theResult) {
			this.request = theRequest;
			this.result = theResult;
		}

		public abstract CatalogResponse handleData(T theData);

		//@Override
		public void handle(Future<T> theEvent) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "handle");

			if (this.result.isSetOrExpired()) {
				debugLogger.log(LogLevel.WARN, this.getClass().getName(), "handle, Data is late");
				return;
			}

			if (theEvent.failed()) {
				this.result.setErrorResult(new CatalogError(this.request, "Catalog API failed", theEvent.cause()));
			}
			else {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "handle, got: {}", theEvent.result());
				CatalogResponse response = handleData(theEvent.result());
				//a null result allows the handler to pass the processing onto some other async processing stage
				if (response != null) {
					if (!this.result.setResult(response)) {
						this.result.setErrorResult(new CatalogError(this.request, "Catalog API call succesful but late"));
					}
				}
			}
		}
	}
}

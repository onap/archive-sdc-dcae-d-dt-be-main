package org.onap.sdc.dcae.composition.impl;

import org.json.JSONArray;
import org.json.JSONException;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Component
public class CompositionCatalogBusinessLogic {

	@Autowired
	private CatalogController catalogController;

	protected OnapLoggerError errLogger = OnapLoggerError.getInstance();

	public DeferredResult<CatalogResponse> getItems(ItemsRequest theRequest) {

		final ItemsRequest request = (theRequest == null) ? ItemsRequest.EMPTY_REQUEST : theRequest;

		Catalog catalog = catalogController.getCatalog(request.getCatalog());
		DeferredResult<CatalogResponse> result = new DeferredResult<>(request.getTimeout());

		catalog.rootsByLabel(request.getStartingLabel()).setHandler(catalogController.new CatalogHandler<Catalog.Folders>(request, result) {
			public CatalogResponse handleData(Catalog.Folders theFolders) {
				JSONArray ja = new JSONArray();
				if (theFolders != null) {
					for (Catalog.Folder folder : theFolders) {
						ja.put(catalogController.patchData(catalog, folder.data()));
					}
				}
				CatalogResponse response = new CatalogResponse(this.request);
				try {
					response.data().put("elements", ja);
				} catch (JSONException e) {
					errLogger.log(LogLevel.ERROR, this.getClass().getName(), "JSONException putting json elements to response {}", e);
				}
				return response;
			}
		});
		return result;
	}

	public DeferredResult<CatalogResponse> getItemById(ItemsRequest theRequest, String theItemId) {

		final ItemsRequest request = (theRequest == null) ? ItemsRequest.EMPTY_REQUEST : theRequest;

		Catalog catalog = catalogController.getCatalog(request.getCatalog());
		DeferredResult<CatalogResponse> result = new DeferredResult<>(request.getTimeout());

		catalog.folder(theItemId).withParts().withPartAnnotations().withItems().withItemAnnotations().withItemModels().execute().setHandler(new FolderHandler(catalog, request, result));
		return result;
	}

	public DeferredResult getModelById(ElementRequest theRequest, String theItemId) {
		final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;

		Catalog catalog = catalogController.getCatalog(request.getCatalog());
		DeferredResult<CatalogResponse> result = new DeferredResult<>(request.getTimeout());

//		try {
			catalog.item(theItemId).withModels().execute().setHandler(new ItemHandler(catalog, request, result));
//		} catch (IllegalArgumentException e) {
//			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Error fetching catalog model with id {}. Message: {}", theItemId, e);
//			result.setErrorResult(new CatalogError(request, "Catalog API failed", e));
//		}
		return result;
	}

	public DeferredResult<CatalogResponse> getTypeInfo(ElementRequest theRequest, String theItemId, String theTypeName) {
		final ElementRequest request = (theRequest == null) ? ElementRequest.EMPTY_REQUEST : theRequest;

		Catalog catalog = catalogController.getCatalog(request.getCatalog());
		DeferredResult<CatalogResponse> result = new DeferredResult<>(request.getTimeout());

		catalog.type(theItemId, theTypeName).withHierarchy().withCapabilities().withRequirements().execute().setHandler(catalogController.new CatalogHandler<Catalog.Type>(request, result) {
			public CatalogResponse handleData(Catalog.Type theType) {
				CatalogResponse response = new CatalogResponse(this.request);
				if (theType != null) {
					try {
						response.data().put("type", catalogController.patchData(catalog, theType.data()));
					} catch (JSONException e) {
						errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Exception processing catalog {}", e);
					}
				}
				return response;
			}
		});
		return result;
	}

	/// Nested Catalog Data Handlers ///

	private class FolderHandler extends CatalogController.CatalogHandler<Catalog.Folder> {

		private Catalog catalog;

		private FolderHandler(Catalog catalog, ItemsRequest request, DeferredResult result) {
			catalogController.super(request, result);
			this.catalog = catalog;
		}

		private void patchCatalogData(Catalog.Elements folders, Catalog catalog) {
			if (folders != null) {
				folders.forEach(folder -> {
					catalogController.patchData(catalog, ((Catalog.Element) folder).data());
					// lots of ephemere proxies created here ..
					Catalog.Elements annotations = ((Catalog.Element) folder).elements("annotations", Catalog.Annotations.class);
					if (annotations != null) {
						annotations.forEach(a -> catalogController.patchData(catalog, ((Catalog.Annotation) a).data()));
					}
				});
			}
		}

		public CatalogResponse handleData(Catalog.Folder theFolder) {
			CatalogResponse response = new CatalogResponse(this.request);
			if (theFolder == null) {
				return response;
			}
			try {
				Catalog.Elements folders = theFolder.elements("parts", Catalog.Folders.class);
				patchCatalogData(folders, catalog);
				Catalog.Elements items = theFolder.elements("items", Catalog.Items.class);
				patchCatalogData(items, catalog);
			} catch (Exception x) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Exception processing catalog {}", x);
				return new CatalogError(this.request, "", x);
			}
			try {
				response.data().put("element", theFolder.data());
			} catch (JSONException e) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "JSONException putting element to response {}", e);
			}
			return response;
		}
	}

	private class ItemHandler extends CatalogController.CatalogHandler<Catalog.Item> {

		private Catalog catalog;

		private ItemHandler(Catalog catalog, ElementRequest request, DeferredResult result) {
			catalogController.super(request, result);
			this.catalog = catalog;
		}

		public CatalogResponse handleData(Catalog.Item theItem) {
			if (theItem == null) {
				return new CatalogError(this.request, "No such item");
			}
			Catalog.Templates models;
			try {
				models = (Catalog.Templates) theItem.elements("models", Catalog.Templates.class);
			} catch (Exception x) {
				return new CatalogError(this.request, "Failed to decode templates from result", x);
			}
			if (models == null || models.isEmpty()) {
				return new CatalogError(this.request, "Item has no models");
			}
			if (models.size() > 1) {
				return new CatalogError(this.request, "Item has more than one model !?");
			}
			try {
				catalog.template(models.get(0).id()).withInputs().withOutputs().withNodes().withNodeProperties().withNodePropertiesAssignments().withNodeRequirements().withNodeCapabilities().withNodeCapabilityProperties()
						.withNodeCapabilityPropertyAssignments().withPolicies().withPolicyProperties().withPolicyPropertiesAssignments().execute().setHandler(new TemplateHandler(this.catalog, this.request, this.result));
			} catch (Exception e) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "Exception caught during Catalog Item Handler: {}", e);
			}
			return null;
		}
	}

	private class TemplateHandler extends CatalogController.CatalogHandler<Catalog.Template> {

		private Catalog catalog;

		private TemplateHandler(Catalog catalog, CatalogRequest request, DeferredResult result) {
			catalogController.super(request, result);
			this.catalog = catalog;
		}

		public CatalogResponse handleData(Catalog.Template theTemplate) {
			CatalogResponse response = new CatalogResponse(this.request);
			if (theTemplate != null) {
				try {
					response.data().put("model", catalogController.patchData(catalog, theTemplate.data()));
				} catch (JSONException e) {
					errLogger.log(LogLevel.ERROR, this.getClass().getName(), "JSONException putting model to response {}", e);
				}
			}
			return response;
		}
	}
}







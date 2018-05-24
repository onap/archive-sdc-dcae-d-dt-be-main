package org.onap.sdc.dcae.composition.impl;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog;
import org.onap.sdc.dcae.catalog.engine.CatalogController;
import org.onap.sdc.dcae.catalog.engine.CatalogError;
import org.onap.sdc.dcae.catalog.engine.CatalogResponse;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CompositionCatalogBusinessLogicTest {

	@Mock
	private CatalogController catalogController;

	private ASDCCatalog asdcCatalog = new ASDCCatalog(URI.create("https://mockUri:8888#mock"));

	@InjectMocks
	private CompositionCatalogBusinessLogic compositionCatalogBusinessLogic = new CompositionCatalogBusinessLogic();

	@Before
	public void init() throws JSONException {
		MockitoAnnotations.initMocks(this);
		when(catalogController.getCatalog(any())).thenReturn(asdcCatalog);
	}

	@Test
	public void getItemsTest() {
		compositionCatalogBusinessLogic.getItems(null).getResult();
		verify(catalogController, times(7)).patchData(any(), any());
	}

	@Test
	public void getItemByIdNoSuchFolderFailureTest() {
		DeferredResult<CatalogResponse> result = compositionCatalogBusinessLogic.getItemById(null, "No Such Category");
		verify(catalogController).getCatalog(any());
		verify(catalogController, times(0)).patchData(any(), any());
		CatalogError error = (CatalogError)result.getResult();
		assertEquals("{\"exception\":\"java.lang.RuntimeException: No such folder No Such Category\",\"message\":\"Catalog API failed\"}", error.getError());
	}

	@Test
	public void getModelByIdInvalidUuidFailureTest() {
		try {
			compositionCatalogBusinessLogic.getModelById(null, "Invalid-UUID");
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid UUID string: Invalid-UUID", e.getMessage());
			verify(catalogController).getCatalog(any());
			verify(catalogController, times(0)).patchData(any(), any());
		}
	}

	@Test
	public void getTypeInfoModelNotLoadedFailureTest() {
		// this is pretty awful. you cannot call 'getTypeInfo' unless it is preceded by a 'getModel' call of the containing model, so that the 'catalogs' item is populated by the container model id.
		String uuid = UUID.randomUUID().toString();
		DeferredResult<CatalogResponse> result = compositionCatalogBusinessLogic.getTypeInfo(null, uuid, "tosca.nodes.Root");
		verify(catalogController).getCatalog(any());
		verify(catalogController, times(0)).patchData(any(), any());
		CatalogError error = (CatalogError)result.getResult();
		assertEquals("{\"exception\":\"java.lang.Exception: No catalog available for resource " + uuid + ". You might want to fetch the model first.\",\"message\":\"Catalog API failed\"}", error.getError());
	}
}
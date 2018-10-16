package org.onap.sdc.dcae.composition.impl;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog;
import org.onap.sdc.dcae.catalog.engine.CatalogController;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.canvas.DcaeComponentCatalog;
import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.errormng.ResponseFormat;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CompositionCatalogBusinessLogicTest {

	private final String REQUEST_ID = "123456";
	private ASDCCatalog asdcCatalog = new ASDCCatalog();

	@Mock
	private CatalogController catalogController;

	@Mock
	private ISdcClient sdcRestClient;

	@InjectMocks
	private CompositionCatalogBusinessLogic compositionCatalogBusinessLogic;

	@Before
	public void init() throws JSONException {
		MockitoAnnotations.initMocks(this);
		when(catalogController.getCatalog()).thenReturn(asdcCatalog);
		new ErrorConfigurationLoader(System.getProperty("user.dir")+"/src/main/webapp/WEB-INF");
		mockCatalog();
	}

	@Test
	public void getCatalogTest() {
		DcaeComponentCatalog catalog = compositionCatalogBusinessLogic.getCatalog(REQUEST_ID);
		assertEquals(1, catalog.getElements().size());
		assertEquals(1, catalog.getElements().get(0).getItems().size());
	}


	@Test
	public void getModelByIdInvalidUuidFailureTest() {
		ResponseEntity result = compositionCatalogBusinessLogic.getModelById(REQUEST_ID, "invalidId");
		assertEquals("Invalid UUID string: invalidId", ((ResponseFormat)result.getBody()).getNotes());
	}

	@Test
	public void getTypeInfoModelNotLoadedFailureTest() {
		String uuid = UUID.randomUUID().toString();
		// this is pretty awful. you cannot call 'getTypeInfo' unless it is preceded by a 'getModel' call of the containing model, so that the 'catalogs' item is populated by the container model id.
		ResponseEntity result = compositionCatalogBusinessLogic.getTypeInfo(uuid, "tosca.nodes.Root");
		assertEquals("No catalog available for resource " + uuid + ". You might want to fetch the model first.", ((ResponseFormat)result.getBody()).getNotes());
	}

	private void mockCatalog() {
		String subcategory1 = "subcategory1";
		String subcategory2 = "subcategory2";
		List<Resource> resources = Arrays.asList(buildVf(subcategory1, DcaeBeConstants.LifecycleStateEnum.CERTIFIED.name()), buildVf(subcategory1, DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name()), buildVf(subcategory2, DcaeBeConstants.LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name()));
		when(sdcRestClient.getResources(anyString(), anyString(), eq(null), anyString())).thenReturn(resources);
	}

	private Resource buildVf(String subcategory, String lifecycleState) {
		Resource vf = new Resource();
		vf.setLifecycleState(lifecycleState);
		vf.setSubCategory(subcategory);
		return vf;
	}
}
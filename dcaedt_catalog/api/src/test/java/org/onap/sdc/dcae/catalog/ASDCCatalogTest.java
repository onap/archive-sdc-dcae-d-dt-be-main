package org.onap.sdc.dcae.catalog;

import static org.assertj.core.api.Assertions.*;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog.CatalogFolderAction;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog.Resource;

import static org.mockito.Mockito.*;


public class ASDCCatalogTest {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	private static CatalogFolderAction getTarget() {
		ASDCCatalog catalog = mock(ASDCCatalog.class);
		when(catalog.folder("test")).thenCallRealMethod();
		CatalogFolderAction target = catalog.folder("test");
		return target;
	}
	
	@Test
	public void filterLatestVersion_null_throwIllegalArgumentException() {
		// arrange
		CatalogFolderAction target = getTarget();
		// assert
		thrown.expect(IllegalArgumentException.class);
		// act
		target.filterLatestVersion(null);
	}
	
	@Test
	public void filterLatestVersion_emptyItemsList_emptyItemsList() throws URISyntaxException {
		// arrange
		CatalogFolderAction target = getTarget();
		// act
		Collection<Resource> result = target.filterLatestVersion(new ArrayList<>());
		// assert
		assertThat(result).isEmpty();
	}
	
	@Test
	public void filterLatestVersion_itemWithTwoVersions_itemWithLatestVersion() {
		// arrange
		CatalogFolderAction target = getTarget();
		
		UUID invariantUUID = UUID.randomUUID();
		Resource r1v1 = mock(Resource.class);
		Resource r1v2 = mock(Resource.class);
		when(r1v1.invariantUUID()).thenReturn(invariantUUID);
		when(r1v2.invariantUUID()).thenReturn(invariantUUID);
		when(r1v1.version()).thenReturn("1.0");
		when(r1v2.version()).thenReturn("2.0");
		ArrayList<Resource> listItemWithTwoVersions = new ArrayList<Resource>(Arrays.asList(r1v1, r1v2));
		// act
		Collection<Resource> result = target.filterLatestVersion(listItemWithTwoVersions);
		// assert
		assertThat(result).containsExactly(r1v2);
	}
	
	@Test
	public void filterLatestVersion_2distinctItems_2distinctItems() {
		// arrange
		CatalogFolderAction target = getTarget();
		
		Resource r1 = mock(Resource.class);
		Resource r2 = mock(Resource.class);
		when(r1.invariantUUID()).thenReturn(UUID.randomUUID());
		when(r2.invariantUUID()).thenReturn(UUID.randomUUID());
		ArrayList<Resource> listOfTwoDistinctItems = new ArrayList<Resource>(Arrays.asList(r1, r2));
		// act
		Collection<Resource> result = target.filterLatestVersion(listOfTwoDistinctItems);
		// assert
		assertThat(result).containsExactlyInAnyOrder(r1, r2);
	}
	
}

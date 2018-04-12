package org.onap.sdc.dcae.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.onap.sdc.dcae.composition.restmodels.DcaeMinimizedService;
import org.onap.sdc.dcae.composition.controller.ServicesController;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum;
import org.testng.annotations.Test;

public class GetServicesTest {
	

	@Test
	public void parseAndFliterServicesByUser_nullServices_TBD() {
//		fail("TODO Auto-generated method stub");
	}
	

	@Test
	public void parseAndFliterServicesByUser_emptyList_emptyList() {
		// arrange
		ServicesController target = new ServicesController();
		String user_id = "test";
		String lastUpdaterUserId = "test";
		List<LinkedHashMap<String, String>> services = new ArrayList<LinkedHashMap<String, String>>();
		// act
		List<DcaeMinimizedService> result = target.parseAndFilterServicesByUser(lastUpdaterUserId, services, user_id);
		// assert
		assertThat(result).isEqualTo(new ArrayList<DcaeMinimizedService>());
	}
	

	@Test
	public void parseAndFliterServicesByUser_singleServicesAsMap_singleServiceParsed() {
		// arrange
		String user_id = "test";
		String lastUpdaterUserId = user_id;
		String uuid = "a";
		String invariantUUID = "1";
		String lifecycleState = LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name();
		String version = "0.1";
		String serviceName = "TestService";

		ServicesController target = new ServicesController();
		LinkedHashMap<String, String> service = createServiceAsMap(lastUpdaterUserId, uuid, invariantUUID,
				lifecycleState, version, serviceName);
		List<LinkedHashMap<String, String>> services = new ArrayList<LinkedHashMap<String, String>>(
				Arrays.asList(service));

		DcaeMinimizedService expected = new DcaeMinimizedService(uuid, serviceName, lastUpdaterUserId, lifecycleState,
				version, invariantUUID);
		// act
		List<DcaeMinimizedService> result = target.parseAndFilterServicesByUser(lastUpdaterUserId, services, user_id);
		// assert
		assertThat(result).usingRecursiveFieldByFieldElementComparator().contains(expected);
	}
	

	@Test
	public void parseAndFliterServicesByUser_unsortedServices_sortedServices() {
		// arrange
		String user_id = "test";
		String lastUpdaterUserId = user_id;
		String uuid = "a";
		String lifecycleState = LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name();
		String version = "0.1";

		List<LinkedHashMap<String, String>> unsortedServices = Arrays.asList("d", "a", "c", "b").stream()
				.map(x -> createServiceAsMap(lastUpdaterUserId, uuid, UUID.randomUUID().toString(), lifecycleState, version, x))
				.collect(Collectors.toList());

		ServicesController target = new ServicesController();

		// act
		List<DcaeMinimizedService> result = target.parseAndFilterServicesByUser(lastUpdaterUserId, unsortedServices,
				user_id);
		// assert
		assertThat(result).extracting("name").containsExactly("a","b","c","d");
	}
	
	
	@Test
	public void parseAndFliterServicesByUser_allOptionsForLastUpdaterAndIsCheckout_allOptionsButIsCheckoutAndNotLastUpdater() {
		// ------------user == last_updater
		// -----------------True----False--
		// isCheckout----------------------
		// --------True------V--------X----
		// --------False-----V--------V----
		// --------------------------------
//		fail("TODO Auto-generated method stub");
	}
	
	
	@Test
	public void parseAndFliterServicesByUser_singleServiceWithMultiVersions_singleServiceWithLatestVersion() {
		// arrange
		String user_id = "test";
		String lastUpdaterUserId = user_id;
		String uuid = "a";
		String invariantUUID = "1";
		String lifecycleState = LifecycleStateEnum.NOT_CERTIFIED_CHECKOUT.name();
		String serviceName = "TestService";
		
		List<LinkedHashMap<String, String>> singleServiceWithMultiVersions = Arrays.asList("1.0", "0.3", "11.0", "2.0", "1.8").stream()
				.map(x -> createServiceAsMap(lastUpdaterUserId, uuid, invariantUUID, lifecycleState, x, serviceName))
				.collect(Collectors.toList());
		
		ServicesController target = new ServicesController();

		// act
		List<DcaeMinimizedService> result = target.parseAndFilterServicesByUser(lastUpdaterUserId, singleServiceWithMultiVersions, user_id);
		
		// assert
		assertThat(result).extracting("version").containsExactly("11.0");
	}
	

	private static LinkedHashMap<String, String> createServiceAsMap(String lastUpdaterUserId, String uuid,
			String invariantUUID, String lifecycleState, String version, String serviceName) {
		
		LinkedHashMap<String, String> service = new LinkedHashMap<String, String>() {
			{
				put("invariantUUID", invariantUUID);
				put("uuid", uuid);
				put("name", serviceName);
				put("lastUpdaterUserId", lastUpdaterUserId);
				put("lifecycleState", lifecycleState);
				put("version", version);
			}
		};
		
		return service;
	}

}

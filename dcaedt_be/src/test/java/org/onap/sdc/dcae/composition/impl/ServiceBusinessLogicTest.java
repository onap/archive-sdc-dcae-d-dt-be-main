package org.onap.sdc.dcae.composition.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.dcae.client.ISdcClient;
import org.onap.sdc.dcae.composition.restmodels.DcaeMinimizedService;
import org.onap.sdc.dcae.composition.impl.ServiceBusinessLogic;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ActionDeserializer;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseAction;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.BaseCondition;
import org.onap.sdc.dcae.composition.restmodels.ruleeditor.ConditionDeserializer;
import org.onap.sdc.dcae.composition.restmodels.sdc.Artifact;
import org.onap.sdc.dcae.composition.restmodels.sdc.ResourceInstance;
import org.onap.sdc.dcae.composition.restmodels.sdc.ServiceDetailed;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants.LifecycleStateEnum;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.testng.annotations.Test;

public class ServiceBusinessLogicTest {

	private String userId = "me";
	private String requestId = "1";
	private String monitoringComponentName = "monitoringComponentName";
	private String serviceUuid = "serviceUuid";
	private String vfiName = "vfiName";

	private static Gson gson = new GsonBuilder()
			.registerTypeAdapter(BaseAction.class, new ActionDeserializer())
			.registerTypeAdapter(BaseCondition.class, new ConditionDeserializer()).create();

	ServiceBusinessLogic target = new ServiceBusinessLogic();


	@Test
	public void parseAndFliterServicesByUser_nullServices_TBD() {
//		fail("TODO Auto-generated method stub");
	}
	

	@Test
	public void parseAndFliterServicesByUser_emptyList_emptyList() {
		// arrange
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
	private void mockGetService()
	{
		ServiceDetailed serviceDetailed = new ServiceDetailed();
		ResourceInstance resourceInstance = new ResourceInstance();
		Artifact artifact = new Artifact();
		artifact.setArtifactName("." + monitoringComponentName + "." + DcaeBeConstants.Composition.fileNames.EVENT_PROC_BP_YAML);
		resourceInstance.setArtifacts(Collections.singletonList(artifact));
		resourceInstance.setResourceInstanceName(vfiName);
		serviceDetailed.setResources(Collections.singletonList(resourceInstance));
		when(target.getSdcRestClient().getService(serviceUuid, requestId)).thenReturn(serviceDetailed);
	}
}

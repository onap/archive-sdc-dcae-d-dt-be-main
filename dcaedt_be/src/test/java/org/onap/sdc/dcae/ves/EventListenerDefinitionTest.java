package org.onap.sdc.dcae.ves;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onap.sdc.dcae.VesStructureLoaderMock;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventListenerDefinitionTest {
	VesStructureLoaderMock loader = new VesStructureLoaderMock(false);

	@Test
	public void resolveRefTypesSimpleTest() throws Exception {
		EventListenerDefinition eventListenerDefinition = loader.getEventListeners().get("4.1");
		assertTrue(eventListenerDefinition.propertiesContainReference());
		eventListenerDefinition.resolveRefTypes();
		assertFalse(eventListenerDefinition.propertiesContainReference());
	}

	@Test
	public void resolveRefTypesSimpleUnresolvableTest() throws Exception {
		EventListenerDefinition eventListenerDefinition = loader.getEventListeners().get("Unresolvable");
		assertTrue(eventListenerDefinition.propertiesContainReference());
		String resolverError = eventListenerDefinition.resolveRefTypes();
		assertTrue(eventListenerDefinition.propertiesContainReference());
		assertEquals("the following definitions containing unresolvable references: [\"otherFields\",\"stateChangeFields\",\"syslogFields\",\"thresholdCrossingAlertFields\"]",resolverError);
	}

	@Test
	public void validateSuccessTest() throws Exception {
		EventListenerDefinition eventListenerDefinition = loader.getEventListeners().get("4.1");
		assertTrue(StringUtils.isBlank(eventListenerDefinition.validate()));
	}

	@Test
	public void validateTypesFailureTest() throws Exception {
		EventListenerDefinition eventListenerDefinition = loader.getEventListeners().get("InvalidType");
		String error = eventListenerDefinition.validate();
		assertEquals("invalid type declaration: invalid", error);
	}

	@Test
	public void validateRequiredFailureTest() throws Exception {
		EventListenerDefinition eventListenerDefinition = loader.getEventListeners().get("InvalidRequiredEntry");
		String error = eventListenerDefinition.validate();
		assertEquals("invalid required entry: codecIdentifier(invalid)", error);
	}

	@Test
	public void validateEventPropertyFailureTest() throws Exception {
		EventListenerDefinition eventListenerDefinition = loader.getEventListeners().get("NoEventProperty");
		String error = eventListenerDefinition.validate();
		assertEquals("schema not containing property: event", error);
	}



}
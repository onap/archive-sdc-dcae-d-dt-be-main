package org.onap.sdc.dcae.checker;

/*
 * What exactly is allowed to go in here is a subject of meditation :) I would have said 'elements with a type' but
 * that will no cover Requirement and Workflow, or topology template top elements but won't cover others ..
 * 
 * Properties/Attributes/Inputs/Outputs are just Data constructs under a particular name.
 */
public enum Construct {
    Data,
		Requirement,
    Capability,
    Relationship,
    Artifact,
    Interface,
    Node,
		Group,
		Policy,
		Workflow
}



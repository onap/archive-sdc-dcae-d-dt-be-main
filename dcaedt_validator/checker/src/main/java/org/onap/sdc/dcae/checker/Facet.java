package org.onap.sdc.dcae.checker;

/*
 * Oddballs:
 *  - requirements (a requirement does not have a type - i.e. is not based
 *  on a Construct) and can target a node, a capability or both .. When present 
 *  as a facet of another Construct it is also the only one represented as a
 *  sequence so it will need special handling anyway.
 */
public enum Facet {
    
	inputs(Construct.Data),
	outputs(Construct.Data),
	properties(Construct.Data),
	attributes(Construct.Data),
 	capabilities(Construct.Capability),
 	//requirements(Construct.Capability),//??
 	artifacts(Construct.Artifact),
	interfaces(Construct.Interface);
	/*
  Node
	Relationship
	they can be considered as facets of the topology template ...
	*/
    
	private Construct construct;

	private Facet(Construct theConstruct) {
		this.construct  = theConstruct;
	}

	public Construct construct() {
		return this.construct;
	}
}



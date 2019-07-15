/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.sdc.dcae.checker;

import java.util.Map;

import org.onap.sdc.dcae.checker.annotations.Checks;

import java.util.List;
import java.util.Iterator;

@Checks
public class Workflows {

	@Checks(path="/topology_template/workflows")
	public void check_workflows(Map theDefinition, Checker.CheckContext theContext) {

		theContext.enter("workflows");
		
		try {
			if(!theContext.checker().checkDefinition("workflows", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_workflow_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}


	public void check_workflow_definition(String theName, Map theDef, Checker.CheckContext theContext) {
		
		theContext.enter("workflow", Construct.Workflow);

		if (theDef.containsKey("inputs")) {
    	theContext
				.checker()
					.checkProperties((Map<String,Map>)theDef.get("inputs"), theContext);
    }

		if (theDef.containsKey("preconditions")) {
			check_workflow_preconditions_definition((List<Map>)theDef.get("preconditions"), theContext);
		}

		if (theDef.containsKey("steps")) {
			check_workflow_steps_definition((Map<String, Map>)theDef.get("steps"), theContext);
		}

		theContext.exit();
	}
  	  

	public void check_workflow_steps_definition(Map theSteps, Checker.CheckContext theContext) {
		
		theContext.enter("steps");

		try {
			for (Iterator<Map.Entry<String,Map>> i = theSteps.entrySet().iterator(); i.hasNext(); ) {
  	  	Map.Entry<String,Map> e = i.next();
				check_workflow_step_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}

	}

	public void check_workflow_step_definition(String theName, Map theDef, Checker.CheckContext theContext) {

		theContext.enter(theName);
		try {
			//requireed entry, must be a node or group template
			String 		target = (String)theDef.get("target");
			Construct targetConstruct = null;

			if (theContext.catalog().hasTemplate(theContext.target(), Construct.Group, target)) {
				targetConstruct = Construct.Group;
			}
			else if (theContext.catalog().hasTemplate(theContext.target(), Construct.Node, target)) {
				targetConstruct = Construct.Node;
			}
			else {
				theContext.addError("The 'target' entry must contain a reference to a node template or group template, '" + target + "' is none of those", null);
			}

			String targetRelationship = (String)theDef.get("target_relationship");
			if (targetConstruct.equals(Construct.Node)) {
				if (targetRelationship != null) {
					//must be a requirement of the target Node
				}
			}

			
		}
		finally {
			theContext.exit();
		}
	}

	public void check_workflow_preconditions_definition(List<Map> thePreconditions, Checker.CheckContext theContext) {
		
		theContext.enter("preconditions");

		try {
			for (Map precondition: thePreconditions) {
				check_workflow_precondition_definition(precondition, theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}

	public void check_workflow_precondition_definition(Map theDef, Checker.CheckContext theContext) {
	}

}

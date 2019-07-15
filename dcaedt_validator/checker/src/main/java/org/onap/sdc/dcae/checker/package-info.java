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

/**
 * The checker provides an api/tool for the verification of TOSCA yaml files
 * as specified in the OASIS specification found at:
 * http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/TOSCA-Simple-Profile-YAML-v1.0.pdf
 *
 * It provides a three stage processing of a tosca yaml file:
 *   - yaml verification: is the document a valid yaml document as per yaml.org/spec. In particular we're using the snakeyaml library for parsing the yaml document to a nested structure of java objects.
 *   - tosca yaml grammar validation: is the document a valid tosca yaml
 *   document, as per the the TOSCA simple profile for yaml. We use a modified
 *   version of the kwalify library for this task. The grammar for TOSCA yaml
 *   is itself a yaml document (found in the package in 
 *   resources/tosca-schema.yaml). There are certain limitations on how far 
 *   this grammar can go.  
 *   - consistency verification: we check the type hierarchies for all TOSCA
 *   constructs (data types, capability types, node types, etc), the definition
 *   of all facets of a construct (properties, attributes, etc) across the type
 *   hierachies, the conformity of construct templates (node templates, ..) with
 *   their types, data valuations(input assignements, constants, function calls).
 *
 * Each stage is blocking, i.e. a stage will be performed only if the previous
 * one completed successfully.
 *
 * The verification is done across all the imported documents. The common TOSCA 
 * types are by default made available to all documents being processed (the
 * specification is in resources/tosca-common-types.yaml). Networking related
 * types can be made available by importing resources/tosca-network-types.yaml
 * while the tosca nfv profile definitions are available at
 * resources/tosca-nfv-types.yaml. 
 *
 * Besides snakeyaml and kwalify this package also has dependencies on Google's
 * guava library and Apache's jxpath.
 *
 * The three java interfaces exposed by the package are the Checker, Target
 * and Report. A Target represents a document processed by the Checker. While
 * the Checker starts with a top Target, through import statements it can end up
 * processing a number of Targets. The results of processing a Target are made
 * available through a Report which currently is nothing more that a list of
 * recorded errors.
 *
 * <div>
 * {@code
 *   Checker checker = new Checker();
 *	 checker.check("tests/example.yaml");
 *
 *	 for (Target t: checker.targets())
 *     System.out.println(t.getLocation() + "\n" + t.getReport());
 * }
 * </div>
 *
 * The errors are recorded as instances of Exception, mostly due to the fact
 * snakeyaml and kwalify do report errors as exceptions. As such there are 3
 * basic types of errros to be expected in a report: YAMLException (from
 * snakeyaml, related to parsing), ValidationException (from kwalify, tosca
 * grammar validation), TargetException (from the checker itself). This might
 * change as we're looking to unify the way errors are reported. A Report 
 * object has a user friendly toString function.
 *
 * A CheckerException thrown during the checking process is an indication of a
 * malfunction in the checker itself.
 *
 * The checker handles targets as URIs. The resolution of a target consists in
 * going from a string representing some path/uri to the absolute URI. URIs can
 * be of any java recognizable schema: file, http, etc. A TargetResolver (not
 * currently exposed through the API) attempts in order:
 *   - if the String is an absolute URI, keep it as such
 *   - if the String is a relative URI attempt to resolve it as relative to
 * know search paths (pre-configured absolute URIs: current directory and the
 * root of the main target's URI). The option of adding custom search paths will
 * be added.
 *   - attempt to resolve as a classpath resource (a jar:file: URI) 
 * 
 * At this time there are no options for the checker (please provide
 * requirements to be considered).
 *
 *
 *
 * Other:
 *   - the checker performs during tosca grammar validation a 'normalization'
 *   process as the tosca yaml profile allows for short forms in the 
 *   specification of a number of its constructs (see spec). The checker changes
 *   the actual structure of the parsed document such that only normalized
 *   (complete) forms of specification are present before the checking phase.
 *   (the kwalify library was extended in order to be able to specify these
 *   short forms in the grammar itself and process/tolerate them at validation
 *   time).
 *
 *   - the checker contains an internal catalog where the types and templates
 *   of different constructs are aggregated and indexed across all targets in
 *   order to facilitate the checking phase. Catalogs can be 'linked' and the
 *   resolution process delegated (the checker maintains a basic catalog with
 *   the core and common types and there is always a second catalog maintaining
 *   the information related to the current targets).
 *   The catalog is currently not exposed by the library.
 *
 *   - imports processing: the import statements present in a target are first 
 *   'detected' during tosca yaml grammar validation phase. At that stage all
 *   imports are (recursively) parsed and validated (first 2 phases). Checking
 *   off all imports (recursively) is done during stage 3.
 *    
 */
package org.onap.sdc.dcae.checker;

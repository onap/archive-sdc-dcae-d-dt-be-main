/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package org.onap.sdc.dcae.catalog.engine;

import org.json.JSONObject;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.dcae.catalog.Catalog;
import org.onap.sdc.dcae.catalog.asdc.ASDCCatalog;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.FutureHandler;
import org.onap.sdc.dcae.composition.util.DcaeBeConstants;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * All requests body:
 *  {
 *		"id": optional request uuid,
 *		"timestamp": optional request timestamp,
 *		"catalog": optional catalog uri,
 *		"timeout": optional timeout - default 0 no time limit
 *  }
 *
 * All responses body:
 * 	{ "data": {},
 *    "error": {}
 *  }
 *
 * If a non-2xx reponse is provided and error occured at catalog engine processing level.
 * If error has occured in data retrieval then the response error object is not empty. 
 *
 * Available uris
 *   /catalog
 *		/elements	: roots of the catalog; request body is optional but can specify a label under 'startingLabel'
 *								response contains items under 'data/elements'
 *		/{itemId}/elements : catalog descendants of the given item, possibly a mix of folders and items  
 *								response contains items under 'data/elements'
 *		/lookup.by.name : lookup catalog entries by name.
									The request body must contain a 'selector' entry with a 'name' criteria
 *								response contains items under 'data/elements'
 *                Example: '{"id":"5d0c1cf4-11aa-11e6-a148-3e1d05defe78","selector":{"name":"Firewall"}}'
 *		/lookup.by.annotation
									The request body must contain a 'annotation' entry and it can have a 'selector' entry 
 *								with a multiple annotation property criteria
 *								response contains items under 'data/elements'
 *		/lookup.by.model.property.value : 
 *								The request must contain a "selector" entry as a JSONObject containing the selection criteria
 *								(property name with values) and desired output properties (null values). Example:
 *									"selector":{"att-part-number":"L-CSR-50M-APP-3Y",
 *															"management-option":"ATT",
 *															"vnf-type":null,
 *															"vendor-model":null}
 *								response contains items under 'data/elements'
 *		/referents : provides generic recommendations
 *								response contains items under 'data/elements'
 *		/{itemId}/referents : provides recommendations for the given item
 *								response contains items under 'data/elements'
 *		/{itemId}/model : retrieves the TOSCA model for the item with the given id
 *								response under 'data/model'
 *
 */


@RestController
@CrossOrigin(origins="*")
public class CatalogController {

	private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();


	@Autowired
	private SystemProperties systemProperties;

	private URI defaultCatalog;
	private static Map<URI, Catalog> catalogs = new HashMap<>();


	public void setDefaultCatalog(URI theUri) {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "set default catalog at {}", theUri);
		this.defaultCatalog = theUri;
	}

	@PostConstruct
	public void initCatalog() {
		// Dump some info and construct our configuration objects
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "initCatalog");
			
		this.defaultCatalog = URI.create(systemProperties.getProperties().getProperty(DcaeBeConstants.Config.ASDC_CATALOG_URL));
		// Initialize default catalog connection
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "default catalog at {}", this.defaultCatalog);
		getCatalog(null);

			// Done
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "CatalogEngine started");
	}

	@PreDestroy
	public void cleanupCatalog() {
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "destroyCatalog");
	}

	public Catalog getCatalog(URI theCatalogUri) {
		//TODO: Thread safety! Check catalog is alive!
		if (theCatalogUri == null) {
			theCatalogUri = this.defaultCatalog;
		}

		Catalog cat = catalogs.get(theCatalogUri);
		if (cat == null && theCatalogUri != null) {
			String scheme = theCatalogUri.getScheme();
			URI catalogUri;
			try {
				catalogUri = new URI(theCatalogUri.getSchemeSpecificPart() + "#" + theCatalogUri.getFragment());
			}
			catch (URISyntaxException urisx) {
				throw new IllegalArgumentException("Invalid catalog reference '" + theCatalogUri.getSchemeSpecificPart() + "'");
			}
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Build catalog for {}", catalogUri);

			if ("asdc".equals(scheme)) {
				cat = new ASDCCatalog(catalogUri);
			}
			else {
				return null;
			}

			catalogs.put(theCatalogUri, cat);
		}
		return cat;
	}

	public JSONObject patchData(Catalog theCat, JSONObject theData) {
		theData.put("catalog", theCat.getUri());
		theData.put("catalogId", theData.optLong("id"));
		theData.put("id", theData.optLong("itemId"));
		return theData;
	}

	public abstract class CatalogHandler<T> implements FutureHandler<T> {

		protected DeferredResult result;
		protected CatalogRequest request;

		public CatalogHandler(CatalogRequest theRequest, DeferredResult theResult) {
			this.request = theRequest;
			this.result = theResult;
		}

		public abstract CatalogResponse handleData(T theData);

		//@Override
		public void handle(Future<T> theEvent) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "handle");

			if (this.result.isSetOrExpired()) {
				debugLogger.log(LogLevel.WARN, this.getClass().getName(), "handle, Data is late");
				return;
			}

			if (theEvent.failed()) {
				this.result.setErrorResult(new CatalogError(this.request, "Catalog API failed", theEvent.cause()));
			}
			else {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "handle, got: {}", theEvent.result());
				CatalogResponse response = handleData(theEvent.result());
				//a null result allows the accumulatorHandler to pass the processing onto some other async processing stage
				if (response != null) {
					if (!this.result.setResult(response)) {
						this.result.setErrorResult(new CatalogError(this.request, "Catalog API call successful but late"));
					}
				}
			}
		}
	}
}	

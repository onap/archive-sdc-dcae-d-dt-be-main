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

package org.onap.sdc.dcae.catalog.commons;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

import org.onap.sdc.dcae.catalog.commons.MapBuilder;

import java.util.function.BiFunction;

public class MapBuilder<K,V> {

	private Map<K,V> map;

	public MapBuilder() {
		this.map = new HashMap<K,V>();
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public MapBuilder<K,V> put(K theKey, V theValue) {
		this.map.put(theKey, theValue);
		return this;
	}

	public MapBuilder<K,V> putOpt(K theKey, V theValue) {
		if (theValue != null) { 
			this.map.put(theKey, theValue);
		}
		return this;
	}

	public MapBuilder<K,V> put(final Map.Entry<? extends K, ? extends V> theEntry) {
		this.map.put(theEntry.getKey(), theEntry.getValue());
		return this;
	}
	
	public MapBuilder<K,V> putOpt(final Map.Entry<? extends K, ? extends V> theEntry) {
		if (theEntry != null) {
			this.map.put(theEntry.getKey(), theEntry.getValue());
		}
		return this;
	}
    
	public MapBuilder<K,V> putAll(final Iterable<? extends Map.Entry<? extends K, ? extends V>> theEntries) {
		for (final Map.Entry<? extends K, ? extends V> e : theEntries) {
			this.map.put(e.getKey(), e.getValue());
		}
		return this;
	}
	
	/* If theEntries contains multiple entries with the same key then the key gets a suffix in order to make it unique
     .. */
//	public MapBuilder forceAll(final Iterable<? extends Map.Entry<? extends K, ? extends V>> theEntries,
		public MapBuilder<K,V> forceAll(final Iterable<? extends Map.Entry<K, V>> theEntries,
														 Function<Map.Entry<K, V> , K> rekeyFunction) {
		for (final Map.Entry<? extends K, ? extends V> e : theEntries) {
			K key = e.getKey();
			if (this.map.containsKey(key))
				key = rekeyFunction.apply((Map.Entry<K,V>)e);
			this.map.put(key, e.getValue());
		}
		return this;
	}

	public MapBuilder<K,V> putAll(final Map<? extends K, ? extends V> theMap) {
		this.map.putAll(theMap);
		return this;
	}  

	public Map<K,V> build() {
		return this.map;
	}

	public Map<K,V> buildOpt() {
		return this.map.isEmpty() ? null : this.map;
	}
}

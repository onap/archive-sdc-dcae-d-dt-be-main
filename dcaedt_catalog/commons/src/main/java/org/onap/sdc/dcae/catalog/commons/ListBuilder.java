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

import java.util.Arrays;
import java.util.List;

import org.onap.sdc.dcae.catalog.commons.ListBuilder;

import java.util.LinkedList;

public class ListBuilder<T> {

	private List<T> list;

	public ListBuilder() {
		this.list = new LinkedList<T>();
	}

	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	public ListBuilder add(T theValue) {
		this.list.add(theValue);
		return this;
	}

	public ListBuilder addAll(final Iterable<? extends T> theValues) {
		for (final T val : theValues) {
			this.list.add(val);
		}
		return this;
	}

	public ListBuilder addAll(final List<? extends T> theList) {
		this.list.addAll(theList);
		return this;
	}  

	public ListBuilder addAll(final T[] theArray) {
		for (T t: theArray) this.list.add(t);
		return this;
	}
  
	public List build() {
		return this.list;
	}

	public List buildOpt() {
		return this.list.isEmpty() ? null : this.list;
	}

	public static <V> List<V> asList(V[] theArray) {
		return Arrays.asList(theArray);
	}
	
	public static <V> List<V> asListOpt(V[] theArray) {
		return (theArray != null && theArray.length > 0) ? Arrays.asList(theArray) : null;
	}
}

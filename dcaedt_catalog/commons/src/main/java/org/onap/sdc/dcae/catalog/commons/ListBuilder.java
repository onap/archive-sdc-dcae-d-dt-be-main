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

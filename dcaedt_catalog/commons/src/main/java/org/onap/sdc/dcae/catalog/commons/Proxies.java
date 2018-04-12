package org.onap.sdc.dcae.catalog.commons;

import java.util.Map;
import java.lang.reflect.ParameterizedType;

import org.json.JSONObject;

import org.onap.sdc.dcae.catalog.commons.ProxyBuilder;


public class Proxies {

	private Proxies() {
	}


	private static ProxyBuilder builder = new ProxyBuilder();

	public static <T> T build(Map theData, Class<T> theType) {
		return builder.build(new JSONObject(theData), theType);
	}
	
	public static <T> T build(Map theData, Map theContextData, Class<T> theType) {
		return builder.build(new JSONObject(theData), theContextData, theType);
	}

	public static <T> T build(JSONObject theData, Class<T> theType) {
		return builder.build(theData, theType);
	}

	public static <T> Class<T> typeArgument(Class theType) {
		return (Class<T>)	
						((ParameterizedType)theType.getGenericSuperclass()).
							getActualTypeArguments()[0];
	}
  
}

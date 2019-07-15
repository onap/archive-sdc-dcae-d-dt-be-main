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
 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;
import java.nio.charset.Charset;
 
/**
 */ 
public class JSONHttpMessageConverter extends AbstractHttpMessageConverter<Object> { 
 
	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8"); 

	/** */ 
	public JSONHttpMessageConverter() { 
  	super(new MediaType("application", "json", DEFAULT_CHARSET)); 
 	} 
 /* 
 	@Override 
 	public boolean canRead(Class<?> theClazz, MediaType theMediaType) { 
  	return canRead(theMediaType); 
 	} 
 
 	@Override 
	public boolean canWrite(Class<?> theClazz, MediaType theMediaType) { 
  	return canWrite(theMediaType); 
 	} 
 */
 	@Override 
 	protected boolean supports(Class<?> theClazz) {
		return theClazz.equals(JSONObject.class) ||
					 theClazz.equals(JSONArray.class);
 	} 
 
	@Override 
 	protected Object readInternal(Class<?> theClazz, HttpInputMessage theInputMessage) 
  																								 throws IOException, HttpMessageNotReadableException { 
   
  	Reader json = new InputStreamReader(theInputMessage.getBody(), getCharset(theInputMessage.getHeaders())); 
   
		try {
			if (theClazz.equals(JSONObject.class))
				return new JSONObject(new JSONTokener(json));
			if (theClazz.equals(JSONArray.class))
				return new JSONArray(new JSONTokener(json));
 			
			throw new HttpMessageNotReadableException("Could not process input, cannot handle " + theClazz); 
		}
		catch (JSONException jsonx) { 
 			throw new HttpMessageNotReadableException("Could not read JSON: " + jsonx.getMessage(), jsonx); 
 		} 
	} 
 
	@Override 
 	protected void writeInternal(Object theObject, HttpOutputMessage theOutputMessage) 
  																								 throws IOException, HttpMessageNotWritableException { 
   
  	Writer writer = new OutputStreamWriter(theOutputMessage.getBody(), getCharset(theOutputMessage.getHeaders())); 
 
		try {
			if (theObject instanceof JSONObject) {
				((JSONObject)theObject).write(writer);
			}
			else if (theObject instanceof JSONArray) {
				((JSONArray)theObject).write(writer);
			}

			writer.close();
  	}
		catch(JSONException jsonx) { 
   		throw new HttpMessageNotWritableException("Could not write JSON: " + jsonx.getMessage(), jsonx); 
  	}  
	} 
  
	private Charset getCharset(HttpHeaders theHeaders) { 
  	if (theHeaders != null &&
				theHeaders.getContentType() != null &&
				theHeaders.getContentType().getCharset() != null) {
   		return theHeaders.getContentType().getCharset();
  	} 
  	return DEFAULT_CHARSET; 
	} 
 
}

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

package org.onap.sdc.dcae.errormng;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class BaseException extends HttpClientErrorException {

    private static final Gson gson = new Gson();

    protected final transient RequestError requestError;

    public BaseException(HttpClientErrorException theError) {
        super(theError.getStatusCode());
        String body = theError.getResponseBodyAsString();
        if (body != null) {
            requestError = extractRequestError(body);
        } else {
            requestError = null;
        }
    }

    public BaseException(HttpStatus status, RequestError re){
        super(status);
        requestError = re;
    }

    public RequestError getRequestError() {
        return requestError;
    }

    private RequestError extractRequestError(String error) {
        ResponseFormat responseFormat = gson.fromJson(error, ResponseFormat.class);
        return responseFormat.getRequestError();
    }

    @JsonIgnore
    public String getMessageId() {
        return requestError.getMessageId();
    }

    @JsonIgnore
    public String[] getVariables() {
        return requestError.getVariables();
    }

    @JsonIgnore
    public String getText(){
        return requestError.getText();
    }

    @Override
    @JsonIgnore
    public String getMessage() {
        return requestError.getFormattedMessage();
    }

}

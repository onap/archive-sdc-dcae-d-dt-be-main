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

package org.onap.sdc.dcae.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.InputStream;

public class SDCResponseErrorHandlerTest {
    @InjectMocks
    private SDCResponseErrorHandler classUnderTest;
    private ClientHttpResponse clientHttpResponse;
    private HttpStatus httpStatus;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        clientHttpResponse = new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return httpStatus;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return httpStatus.value();
            }

            @Override
            public String getStatusText() throws IOException {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                return null;
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
    }

    @Test(expected = HttpClientErrorException.class)
    public void handleError_haveError_throwsClientException() throws IOException {
        httpStatus = HttpStatus.EXPECTATION_FAILED;
        classUnderTest.handleError(clientHttpResponse);
    }

    @Test(expected = HttpServerErrorException.class)
    public void handleError_haveError_throwsServerException() throws IOException {
        httpStatus = HttpStatus.BAD_GATEWAY;
        classUnderTest.handleError(clientHttpResponse);
    }

    @Test
    public void hasError_haveClientError_returnTrue() throws IOException {
        httpStatus = HttpStatus.EXPECTATION_FAILED;
        boolean result = classUnderTest.hasError(clientHttpResponse);
        Assert.assertTrue(result);
    }

    @Test
    public void hasError_haveServerError_returnTrue() throws IOException {
        httpStatus = HttpStatus.BAD_GATEWAY;
        boolean result = classUnderTest.hasError(clientHttpResponse);
        Assert.assertTrue(result);
    }

    @Test
    public void hasError_200OK_returnFalse() throws IOException {
        httpStatus = HttpStatus.OK;
        boolean result = classUnderTest.hasError(clientHttpResponse);
        Assert.assertFalse(result);
    }

}

package org.onap.sdc.dcae.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.InputStream;

@RunWith(MockitoJUnitRunner.class)
public class SDCResponseErrorHandlerTest {
    @InjectMocks
    private SDCResponseErrorHandler classUnderTest;
    private ClientHttpResponse clientHttpResponse;
    private HttpStatus httpStatus;

    @Before
    public void setup() {
        clientHttpResponse = new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return httpStatus;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return 0;
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
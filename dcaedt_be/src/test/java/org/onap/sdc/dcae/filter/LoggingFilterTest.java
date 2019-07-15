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

package org.onap.sdc.dcae.filter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoggingFilterTest {
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse httpResponse;
    @Mock private ServletResponse response;
    @Mock private FilterChain filterChain;

    private FilterConfig filterConfig = new FilterConfig() {
        @Override
        public String getFilterName() {
            return null;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return null;
        }
    };

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void loggingFilterInstanciationLifeCycle() {
        LoggingFilter loggingFilter = new LoggingFilter();

        loggingFilter.init(filterConfig);

        loggingFilter.destroy();
    }

    @Test
    public void doFilter_healthCheck_noReadingHeader(){
        LoggingFilter loggingFilter = new LoggingFilter();
        boolean exceptionThrown = false;

        when(request.getServletPath()).thenReturn("/healthCheck");
        try {
            loggingFilter.doFilter(request, response, filterChain);
        }
        catch (Exception e){
            exceptionThrown = true;
        }
        verify(request,never()).getHeader("X-ECOMP-RequestID");
        assertEquals(exceptionThrown, false);
    }

    @Test
    public void doFilter_notHealthCheck_noReadingHeader(){
        LoggingFilter loggingFilter = new LoggingFilter();
        boolean exceptionThrown = false;

        when(request.getServletPath()).thenReturn("/notHealthCheck");
        try {
            loggingFilter.doFilter(request, response, filterChain);
        }
        catch (Exception e){
            exceptionThrown = true;
        }
        verify(request).getHeader("X-ECOMP-RequestID");
        assertEquals(exceptionThrown, false);
    }
    @Test
    public void doFilter_InternalServerError(){
        LoggingFilter loggingFilter = new LoggingFilter();
        boolean exceptionThrown = false;

        when(request.getServletPath()).thenReturn("/notHealthCheck");
        try {

            when(httpResponse.getStatus()).thenReturn(500);
            when(request.getHeader("user-agent")).thenReturn("test");

            loggingFilter.doFilter(request, httpResponse, filterChain);
        }
        catch (Exception e){
            exceptionThrown = true;
        }
        verify(request).getHeader("X-ECOMP-RequestID");
        assertEquals(exceptionThrown, false);
    }


}

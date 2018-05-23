package org.onap.sdc.dcae.filter;

import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.Enums.OnapLoggerErrorCode;
import org.onap.sdc.common.onaplog.OnapMDCWrapper;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Locale;

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

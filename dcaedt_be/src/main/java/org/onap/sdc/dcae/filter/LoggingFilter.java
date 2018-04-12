package org.onap.sdc.dcae.filter;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_ENTITY_TOO_LARGE;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_GONE;
import static java.net.HttpURLConnection.HTTP_LENGTH_REQUIRED;
import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_PAYMENT_REQUIRED;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.net.HttpURLConnection.HTTP_PROXY_AUTH;
import static java.net.HttpURLConnection.HTTP_REQ_TOO_LONG;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.onap.sdc.common.onaplog.OnapLoggerAudit;
import org.onap.sdc.common.onaplog.OnapMDCWrapper;
import org.onap.sdc.common.onaplog.Enums.OnapLoggerErrorCode;
import org.onap.sdc.common.onaplog.Enums.LogLevel;

public class LoggingFilter implements Filter {
	
	private static final String serviceName = "DCAE-D-BE";
	
	private OnapMDCWrapper commonLoggerArgs = OnapMDCWrapper.getInstance();
	private OnapLoggerAudit auditLogger = OnapLoggerAudit.getInstance();
	
	public LoggingFilter() {
		super();
	}


	@Override
	public void destroy() {}

	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		boolean shouldLogRequest = true;
		
		try {
			if (request instanceof HttpServletRequest) {
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				if (httpRequest.getServletPath().equals("/healthCheck")) {
					shouldLogRequest = false;
				}
				
				if (shouldLogRequest) {
					beforeHandle(httpRequest);
				}
			}
		} catch (Exception e) {
			// TODO: log problem with extracting parameters or writing to log
		}
		
		filterChain.doFilter(request, response); // handle request
		
		try {
			if (response instanceof HttpServletResponse && shouldLogRequest) {
				afterHandle((HttpServletResponse) response);
			}
		} catch (Exception e) {
			// TODO: log problem with extracting parameters or writing to log
		}
	}
	
	
	private void beforeHandle(HttpServletRequest request) {
		
		String requestId = getRequestId(request);
		request.setAttribute("requestId", requestId); // making requestId available for the API controllers
		commonLoggerArgs
			.clear()
			.startTimer()
			.setRemoteHost(request.getRemoteAddr())
			.setServiceName(serviceName)
			.setPartnerName(getPartnerName(request.getHeader("USER_ID"), request.getHeader("user-agent")))
			.setKeyRequestId(requestId)
			.setAutoServerIPAddress(request.getLocalAddr())
			.setOptCustomField1(request.getProtocol())
			.setOptCustomField2(request.getMethod())
			.setOptCustomField3(request.getServletPath());
		
	}


	private static String getRequestId(HttpServletRequest request) {
		String requestId = request.getHeader("X-ECOMP-RequestID");
		return isNullOrEmpty(requestId)
			? UUID.randomUUID().toString()
			: requestId;
	}

	
	private void afterHandle(HttpServletResponse response) {
		String responseDesc = EnglishReasonPhraseCatalog.INSTANCE.getReason(response.getStatus(), Locale.ENGLISH);
		commonLoggerArgs
			.stopTimer()
			.setResponseCode(getLoggingErrorCode(response.getStatus()).getErrorCode())
			.setResponseDesc(responseDesc)
			.setOptCustomField4(Integer.toString(response.getStatus()));
		
		auditLogger
			.setStatusCode(Integer.toString(response.getStatus()))
			.log(LogLevel.INFO, this.getClass().getName(), responseDesc);
	}


	private OnapLoggerErrorCode getLoggingErrorCode(int httpResponseCode) {
		if (isSuccessError(httpResponseCode)) {
			return OnapLoggerErrorCode.SUCCESS;
		}
		else if (isSchemaError(httpResponseCode)) {
			return OnapLoggerErrorCode.SCHEMA_ERROR;
		}
		else if (isDataError(httpResponseCode)) {
			return OnapLoggerErrorCode.DATA_ERROR;
		}
		else if (isPermissionsError(httpResponseCode)) {
			return OnapLoggerErrorCode.PERMISSION_ERROR;
		}
		else if (isTimeoutOrAvailabilityError(httpResponseCode)) {
			return OnapLoggerErrorCode.AVAILABILITY_TIMEOUTS_ERROR;
		}
		else if (isBusinessProcessError(httpResponseCode)) {
			return OnapLoggerErrorCode.BUSINESS_PROCESS_ERROR;
		}
		else {
			return OnapLoggerErrorCode.UNKNOWN_ERROR;
		}
	}


	private boolean isTimeoutOrAvailabilityError(int httpResponseCode) {

        switch (httpResponseCode) {
            case HTTP_BAD_REQUEST:
            case HTTP_UNAUTHORIZED:
            case HTTP_NOT_FOUND:
            case HTTP_CLIENT_TIMEOUT:
            case HTTP_GONE:
                return true;
        }

        return false;
    }

    private boolean isPermissionsError(int httpResponseCode) {

        switch (httpResponseCode) {
            case HTTP_PAYMENT_REQUIRED:
            case HTTP_FORBIDDEN:
            case HTTP_BAD_METHOD:
            case HTTP_PROXY_AUTH:
                return true;
        }

        return false;
    }

    private boolean isDataError(int httpResponseCode) {

        switch (httpResponseCode) {
            case HTTP_NOT_ACCEPTABLE:
            case HTTP_LENGTH_REQUIRED:
            case HTTP_PRECON_FAILED:
            case HTTP_REQ_TOO_LONG:
            case HTTP_ENTITY_TOO_LARGE:
            case HTTP_UNSUPPORTED_TYPE:
                return true;
        }

        return false;
    }

    private boolean isSchemaError(int httpResponseCode) {

        switch (httpResponseCode) {
            case HTTP_CONFLICT:
                return true;
        }

        return false;
    }

    private boolean isSuccessError(int httpResponseCode) {
        return httpResponseCode < 399;
    }

    private boolean isBusinessProcessError(int httpResponseCode) {
        return httpResponseCode > 499;
    }
    
    private String getPartnerName(String userId, String userAgent) {
    	return (isNullOrEmpty(userId))
    			? getClientApplication(userAgent)
				: userId;
    }
    
    private String getClientApplication(String userAgent) {
    	if (userAgent != null && userAgent.length() > 0) {
            if (userAgent.toLowerCase().contains("firefox")) {
                return "fireFox_FE";
            }

            if (userAgent.toLowerCase().contains("msie")) {
                return "explorer_FE";
            }

            if (userAgent.toLowerCase().contains("chrome")) {
                return "chrome_FE";
            }

            return userAgent;
        }
        return "";
	}


	private static boolean isNullOrEmpty(String str) {
        return (str == null || str.isEmpty());
    }


	@Override
	public void init(FilterConfig config) throws ServletException {}
}

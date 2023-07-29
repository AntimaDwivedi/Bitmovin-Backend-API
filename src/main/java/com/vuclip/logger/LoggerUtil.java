package com.vuclip.logger;

import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class LoggerUtil {

    private LoggerUtil() {
    }

    public static String X_SESSION_ID = "x-session-id";

    public static void registerContext(HttpServletRequest request) {
        if ((request.getHeader("x-session-id") != null) && (request.getHeader("x-session-id") != "")) {
            MDC.put("xRequestId", request.getHeader("x-request-id"));
            MDC.put("xSessionId", request.getHeader("x-session-id"));
        } else {
            X_SESSION_ID = UUID.randomUUID().toString();
            MDC.put("xRequestId", request.getHeader("x-request-id"));
            MDC.put("xSessionId", X_SESSION_ID);
        }
    }


    public static void deregisterContext() {
        MDC.clear();
    }
}

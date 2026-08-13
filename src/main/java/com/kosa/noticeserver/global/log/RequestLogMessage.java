package com.kosa.noticeserver.global.log;

import jakarta.servlet.http.HttpServletRequest;

public record RequestLogMessage(
        String httpMethod,
        String requestUri
) {
    public static RequestLogMessage createInstance(HttpServletRequest request) {
        return new RequestLogMessage(
                request.getMethod(),
                request.getRequestURI()
        );
    }
}

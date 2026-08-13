package com.kosa.noticeserver.global.log;

import jakarta.servlet.http.HttpServletResponse;

public record ResponseLogMessage(
        int httpStatus
) {
    public static ResponseLogMessage createInstance(HttpServletResponse response) {
        return new ResponseLogMessage(response.getStatus());
    }
}

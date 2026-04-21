package com.gorani.ecodrive.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InternalApiTokenValidator {

    @Value("${app.pay.internal-token:}")
    private String internalToken;

    public void validate(String requestToken) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "내부 API 토큰 설정 누락");
        }
        if (requestToken == null || requestToken.isBlank() || !internalToken.equals(requestToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내부 API 토큰 불일치");
        }
    }
}

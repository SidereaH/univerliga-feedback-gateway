package com.univerliga.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univerliga.gateway.config.GatewayProperties;
import com.univerliga.gateway.dto.AuthDtos;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class AuthService {
    private static final String GRANT_TYPE_PASSWORD = "password";

    private final RestClient restClient;
    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthService(GatewayProperties properties, ObjectMapper objectMapper) {
        this(RestClient.create(), properties, objectMapper);
    }

    AuthService(RestClient restClient, GatewayProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AuthDtos.TokenResponse token(AuthDtos.TokenRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE_PASSWORD);
        form.add("client_id", properties.auth().clientId());
        form.add("client_secret", properties.auth().clientSecret());
        form.add("username", request.username());
        form.add("password", request.password());

        try {
            AuthDtos.TokenResponse tokenResponse = restClient.post()
                .uri(properties.auth().tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AuthDtos.TokenResponse.class);
            if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
                throw new ApiException("UPSTREAM_AUTH_ERROR", "Authentication provider returned invalid token response",
                    HttpStatus.BAD_GATEWAY);
            }
            return tokenResponse;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()
                || ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw new ApiException("UNAUTHORIZED", "Invalid username or password", HttpStatus.UNAUTHORIZED, details(ex));
            }
            throw new ApiException("UPSTREAM_AUTH_ERROR", "Authentication provider error", HttpStatus.BAD_GATEWAY, details(ex));
        }
    }

    private List<ApiErrorDetail> details(RestClientResponseException ex) {
        String providerError = extractProviderError(ex.getResponseBodyAsString());
        if (providerError == null || providerError.isBlank()) {
            providerError = ex.getStatusText();
        }
        return List.of(new ApiErrorDetail("authProvider", providerError));
    }

    private String extractProviderError(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode description = root.get("error_description");
            if (description != null && description.isTextual()) {
                return description.asText();
            }
            JsonNode code = root.get("error");
            if (code != null && code.isTextual()) {
                return code.asText();
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}

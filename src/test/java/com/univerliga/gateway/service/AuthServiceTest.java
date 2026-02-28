package com.univerliga.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univerliga.gateway.config.GatewayProperties;
import com.univerliga.gateway.dto.AuthDtos;
import com.univerliga.gateway.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthServiceTest {

    @Test
    void tokenReturnsParsedResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://keycloak:8080/realms/univerliga/protocol/openid-connect/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(allOf(
                containsString("grant_type=password"),
                containsString("client_id=univerliga-gateway"),
                containsString("client_secret=gateway-secret"),
                containsString("username=employee"),
                containsString("password=employee")
            )))
            .andRespond(withSuccess("""
                {
                  "access_token":"access-token",
                  "expires_in":300,
                  "refresh_expires_in":1800,
                  "refresh_token":"refresh-token",
                  "token_type":"Bearer",
                  "scope":"profile email"
                }
                """, MediaType.APPLICATION_JSON));

        AuthService service = new AuthService(builder.build(), properties(), new ObjectMapper());

        AuthDtos.TokenResponse response = service.token(new AuthDtos.TokenRequest("employee", "employee"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(300, response.expiresIn());
        server.verify();
    }

    @Test
    void invalidCredentialsMappedToUnauthorized() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://keycloak:8080/realms/univerliga/protocol/openid-connect/token"))
            .andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                    {
                      "error":"invalid_grant",
                      "error_description":"Invalid user credentials"
                    }
                    """));

        AuthService service = new AuthService(builder.build(), properties(), new ObjectMapper());

        ApiException ex = assertThrows(ApiException.class,
            () -> service.token(new AuthDtos.TokenRequest("employee", "wrong")));

        assertEquals("UNAUTHORIZED", ex.getCode());
        assertEquals(401, ex.getStatus().value());
        assertEquals("authProvider", ex.getDetails().getFirst().field());
        server.verify();
    }

    private GatewayProperties properties() {
        return new GatewayProperties(
            "mock",
            "v1",
            new GatewayProperties.Clients(
                new GatewayProperties.ServiceEndpoint("http://crm:8080"),
                new GatewayProperties.ServiceEndpoint("http://feedback:8080"),
                new GatewayProperties.ServiceEndpoint("http://reporting:8080"),
                new GatewayProperties.ServiceEndpoint("http://analytics:8080")
            ),
            new GatewayProperties.Auth(
                "http://keycloak:8080/realms/univerliga/protocol/openid-connect/token",
                "univerliga-gateway",
                "gateway-secret"
            )
        );
    }
}

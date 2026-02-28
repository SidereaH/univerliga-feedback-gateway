package com.univerliga.gateway.integration;

import com.univerliga.gateway.dto.AuthDtos;
import com.univerliga.gateway.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.univerliga.gateway.testutil.TestAuth.jwtFor;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void unauthenticatedApiReturns401WithStandardError() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void apiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    @Test
    void tokenEndpointIsPublic() throws Exception {
        when(authService.token(any())).thenReturn(new AuthDtos.TokenResponse(
            "access-token", 300, 1800, "refresh-token", "Bearer", "profile email"));

        mockMvc.perform(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "employee",
                      "password": "employee"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void meEndpointReturnsEnvelopeAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                .header("X-Request-Id", "req-123")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-Id", "req-123"))
            .andExpect(jsonPath("$.data.personId").value("p_employee"))
            .andExpect(jsonPath("$.meta.requestId").value("req-123"))
            .andExpect(jsonPath("$.meta.version").value("v1"));
    }

    @Test
    void employeeCannotCreateFeedbackForTaskWithoutParticipation() throws Exception {
        String body = """
            {
              "targetPersonId": "p_5",
              "contextType": "TASK",
              "contextRef": "task_2",
              "rating": 4,
              "sentiment": "POSITIVE",
              "tagIds": ["sub_comm_good"],
              "comment": "x"
            }
            """;

        mockMvc.perform(post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void backwardCompatibleTaskIdStillAccepted() throws Exception {
        String body = """
            {
              "targetPersonId": "p_4",
              "taskId": "task_1",
              "rating": 5,
              "tagIds": ["sub_comm_good"],
              "comment": "legacy"
            }
            """;
        mockMvc.perform(post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contextType").value("TASK"))
            .andExpect(jsonPath("$.data.contextRef").value("task_1"));
    }

    @Test
    void duplicateReviewReturnsConflict() throws Exception {
        String body = """
            {
              "targetPersonId": "p_7",
              "contextType": "EPISODE",
              "contextRef": "episode_2026_3",
              "rating": 4,
              "tagIds": ["sub_comm_good"],
              "comment": "dup test"
            }
            """;
        mockMvc.perform(post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_REVIEW"));
    }

    @Test
    void adminCanReadRawFeedbackWithAuthor() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/raw?page=1&size=1")
                .with(jwtFor("admin", "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].authorPersonId", notNullValue()));
    }

    @Test
    void hrCanReadRawFeedbackWithAuthor() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/raw?page=1&size=1")
                .with(jwtFor("hr", "ROLE_HR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].authorPersonId", notNullValue()));
    }

    @Test
    void employeeCannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-03-31")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void managerCanReadReportsWithCoverage() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-03-31")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.kpis.responses", greaterThan(0)))
            .andExpect(jsonPath("$.data.kpis.coverage.totalTargetsInScope", greaterThan(0)));
    }

    @Test
    void managerCanReadTopTagsInsights() throws Exception {
        mockMvc.perform(get("/api/v1/reports/insights/top-tags")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-03-31")
                .param("limit", "5")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.topPositive.length()", greaterThan(0)))
            .andExpect(jsonPath("$.data.topNegative.length()", greaterThan(0)));
    }

    @Test
    void managerCanReadCompositeDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/reports/dashboard")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-03-31")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.kpis.responses", greaterThan(0)))
            .andExpect(jsonPath("$.data.insights.topTags.topPositive.length()", greaterThan(0)));
    }

    @Test
    void systemVersionIsAvailableForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/system/version")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("univerliga-gateway"))
            .andExpect(jsonPath("$.data.mode").value("mock"));
    }
}

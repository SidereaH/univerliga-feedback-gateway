package com.univerliga.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.univerliga.gateway.testutil.TestAuth.jwtFor;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
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
    void employeeSeesOnlySelfInPeopleList() throws Exception {
        mockMvc.perform(get("/api/v1/crm/people")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value("p_employee"));
    }

    @Test
    void employeeCannotReadAnotherPersonCard() throws Exception {
        mockMvc.perform(get("/api/v1/crm/people/p_4")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void adminSeesKeycloakUserIdInPersonCard() throws Exception {
        mockMvc.perform(get("/api/v1/crm/people/p_manager")
                .with(jwtFor("admin", "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.keycloakUserId").value("kc_manager"));
    }

    @Test
    void createPersonValidationUsesStandardErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/crm/people")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(jwtFor("admin", "ROLE_ADMIN")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details.length()", greaterThan(0)));
    }

    @Test
    void employeeCannotOpenTaskOutsideParticipation() throws Exception {
        mockMvc.perform(get("/api/v1/crm/tasks/task_2")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void employeeCanOpenParticipantTask() throws Exception {
        mockMvc.perform(get("/api/v1/crm/tasks/task_1")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value("task_1"));
    }

    @Test
    void employeeCannotCreateFeedbackForTaskWithoutParticipation() throws Exception {
        String body = """
            {
              "taskId": "task_2",
              "targetPersonId": "p_5",
              "categoryId": "cat_1",
              "subcategoryId": "sub_1",
              "rating": 4,
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
    void adminCanReadRawFeedbackWithAuthor() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/raw?page=1&size=1")
                .with(jwtFor("admin", "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].authorPersonId", notNullValue()));
    }

    @Test
    void employeeCannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-01-31")
                .with(jwtFor("employee", "ROLE_EMPLOYEE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void managerCanReadReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-01-31")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.kpis.responses", greaterThan(0)));
    }

    @Test
    void managerCanReadPositivityByPersonChart() throws Exception {
        mockMvc.perform(get("/api/v1/reports/charts/positivity-by-person")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-03-31")
                .param("limit", "5")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.period.from").value("2026-01-01"))
            .andExpect(jsonPath("$.data.items.length()", greaterThan(0)));
    }

    @Test
    void missingReportingPeriodParamReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/reports/charts/subcategory-frequency")
                .param("periodFrom", "2026-01-01")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void managerCanReadCompositeDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/reports/dashboard")
                .param("periodFrom", "2026-01-01")
                .param("periodTo", "2026-03-31")
                .with(jwtFor("manager", "ROLE_MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.kpis.responses", greaterThan(0)))
            .andExpect(jsonPath("$.data.charts.trend.metric").value("responses"));
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

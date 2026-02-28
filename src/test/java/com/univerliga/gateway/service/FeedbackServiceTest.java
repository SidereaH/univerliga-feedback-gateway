package com.univerliga.gateway.service;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.dto.FeedbackDtos;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.model.TaskRecord;
import com.univerliga.gateway.security.CurrentUser;
import com.univerliga.gateway.security.CurrentUserService;
import com.univerliga.gateway.security.SecurityRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackClient feedbackClient;

    @Mock
    private CrmClient crmClient;

    @Mock
    private CurrentUserService currentUserService;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackClient, crmClient, currentUserService);
    }

    @Test
    void createFeedbackForbiddenForEmployeeWithoutParticipation() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", java.util.Set.of(SecurityRoles.EMPLOYEE)));
        when(crmClient.findTaskById("task_1")).thenReturn(Optional.of(task(List.of("p_2", "p_3"))));

        FeedbackDtos.CreateFeedbackRequest request = new FeedbackDtos.CreateFeedbackRequest(
            "task_1", "p_2", "cat_1", "sub_1", 5, "ok"
        );

        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.create(request));
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void createFeedbackHidesAuthorInResponse() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", java.util.Set.of(SecurityRoles.MANAGER)));
        when(crmClient.findTaskById("task_1")).thenReturn(Optional.of(task(List.of("p_2", "p_3"))));
        when(feedbackClient.createFeedback("task_1", "p_2", "p_manager", "cat_1", "sub_1", 5, "ok"))
            .thenReturn(new FeedbackRecord("fb_1", "task_1", "p_2", "p_manager", "cat_1", "sub_1", 5, "ok", Instant.now()));

        FeedbackDtos.FeedbackItem result = feedbackService.create(new FeedbackDtos.CreateFeedbackRequest(
            "task_1", "p_2", "cat_1", "sub_1", 5, "ok"
        ));

        assertNull(result.authorPersonId());
        assertEquals("fb_1", result.id());
    }

    @Test
    void rawIsForbiddenForNonAdmin() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", java.util.Set.of(SecurityRoles.MANAGER)));

        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.raw(null, null, null, 1, 20));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void rawContainsAuthorForAdmin() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("admin", "p_admin", java.util.Set.of(SecurityRoles.ADMIN)));
        when(feedbackClient.findRaw(null, null, null)).thenReturn(List.of(
            new FeedbackRecord("fb_1", "task_1", "p_2", "p_3", "cat_1", "sub_1", 4, "ok", Instant.now())
        ));

        FeedbackDtos.FeedbackPage response = feedbackService.raw(null, null, null, 1, 20);
        FeedbackDtos.FeedbackItem item = (FeedbackDtos.FeedbackItem) response.items().get(0);

        assertEquals("p_3", item.authorPersonId());
        assertNotNull(item.createdAt());
    }

    private TaskRecord task(List<String> participants) {
        return new TaskRecord("task_1", "T", "D", "ACTIVE", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "p_1", "p_2", participants, Instant.now(), null);
    }
}

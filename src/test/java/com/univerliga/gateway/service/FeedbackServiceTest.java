package com.univerliga.gateway.service;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.dto.FeedbackDtos;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.CategoryRecord;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
        lenient().when(feedbackClient.categories()).thenReturn(List.of(
            new CategoryRecord("cat_work", "Work", List.of(
                new CategoryRecord.SubcategoryRecord("sub_comm_good", "Good", CategoryRecord.SubcategoryRecord.Polarity.POSITIVE, true),
                new CategoryRecord.SubcategoryRecord("sub_deadline_fail", "Bad", CategoryRecord.SubcategoryRecord.Polarity.NEGATIVE, true)
            ))
        ));
    }

    @Test
    void createReviewForbiddenForEmployeeWithoutParticipationOnTask() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", Set.of(SecurityRoles.EMPLOYEE)));
        when(crmClient.findTaskById("task_1")).thenReturn(Optional.of(task(List.of("p_2", "p_3"))));

        FeedbackDtos.CreateReviewRequest request = new FeedbackDtos.CreateReviewRequest(
            "p_2", FeedbackRecord.ContextType.TASK, "task_1", null, 5,
            FeedbackRecord.Sentiment.POSITIVE, List.of("sub_comm_good"), "ok"
        );

        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.create(request));
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void createReviewMapsLegacyTaskIdToTaskContext() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("manager", "p_manager", Set.of(SecurityRoles.MANAGER)));
        when(crmClient.findTaskById("task_1")).thenReturn(Optional.of(task(List.of("p_2", "p_3"))));
        when(feedbackClient.findDuplicate("p_manager", "p_2", FeedbackRecord.ContextType.TASK, "task_1")).thenReturn(Optional.empty());
        when(feedbackClient.createReview(any())).thenReturn(new FeedbackRecord(
            "fb_1", "p_2", "p_manager", FeedbackRecord.ContextType.TASK, "task_1",
            5, FeedbackRecord.Sentiment.POSITIVE, List.of("sub_comm_good"), "ok", Instant.now(), null
        ));

        FeedbackDtos.ReviewResponse result = feedbackService.create(new FeedbackDtos.CreateReviewRequest(
            "p_2", null, null, "task_1", 5,
            null, List.of("sub_comm_good"), "ok"
        ));

        assertEquals("fb_1", result.id());
        assertNull(result.updatedAt());
        assertEquals(FeedbackRecord.ContextType.TASK, result.contextType());
    }

    @Test
    void duplicateReviewReturnsConflict() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("employee", "p_employee", Set.of(SecurityRoles.EMPLOYEE)));
        when(crmClient.findTaskById("task_1")).thenReturn(Optional.of(task(List.of("p_employee", "p_3"))));
        when(feedbackClient.findDuplicate("p_employee", "p_2", FeedbackRecord.ContextType.TASK, "task_1"))
            .thenReturn(Optional.of(new FeedbackRecord(
                "fb_exists", "p_2", "p_employee", FeedbackRecord.ContextType.TASK, "task_1",
                5, FeedbackRecord.Sentiment.POSITIVE, List.of("sub_comm_good"), "old", Instant.now(), null
            )));

        FeedbackDtos.CreateReviewRequest request = new FeedbackDtos.CreateReviewRequest(
            "p_2", FeedbackRecord.ContextType.TASK, "task_1", null, 5,
            FeedbackRecord.Sentiment.POSITIVE, List.of("sub_comm_good"), "ok"
        );
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.create(request));

        assertEquals("DUPLICATE_REVIEW", ex.getCode());
        assertEquals("existingReviewId", ex.getDetails().getFirst().field());
    }

    @Test
    void rawContainsAuthorForHr() {
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser("hr", "p_hr", Set.of(SecurityRoles.HR)));
        when(feedbackClient.findRaw(null, null, null, null)).thenReturn(List.of(
            new FeedbackRecord("fb_1", "p_2", "p_3", FeedbackRecord.ContextType.EPISODE, "episode_1",
                4, FeedbackRecord.Sentiment.POSITIVE, List.of("sub_comm_good"), "ok", Instant.now(), null)
        ));

        FeedbackDtos.ReviewPage response = feedbackService.raw(null, null, null, null, 1, 20);
        FeedbackDtos.RawReviewResponse item = (FeedbackDtos.RawReviewResponse) response.items().getFirst();

        assertEquals("p_3", item.authorPersonId());
        assertNotNull(item.createdAt());
    }

    private TaskRecord task(List<String> participants) {
        return new TaskRecord("task_1", "T", "D", "ACTIVE",
            LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"),
            "p_1", "p_2", participants, Instant.now(), null);
    }
}

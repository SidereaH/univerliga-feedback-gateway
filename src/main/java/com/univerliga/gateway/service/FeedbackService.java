package com.univerliga.gateway.service;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.dto.FeedbackDtos;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.model.TaskRecord;
import com.univerliga.gateway.security.CurrentUser;
import com.univerliga.gateway.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackClient feedbackClient;
    private final CrmClient crmClient;
    private final CurrentUserService currentUserService;

    public FeedbackService(FeedbackClient feedbackClient, CrmClient crmClient, CurrentUserService currentUserService) {
        this.feedbackClient = feedbackClient;
        this.crmClient = crmClient;
        this.currentUserService = currentUserService;
    }

    public FeedbackDtos.CategoriesResponse categories() {
        return new FeedbackDtos.CategoriesResponse(
            feedbackClient.categories().stream()
                .map(c -> new FeedbackDtos.CategoryDto(c.id(), c.name(),
                    c.subcategories().stream().map(s -> new FeedbackDtos.SubcategoryDto(s.id(), s.name())).toList()))
                .toList()
        );
    }

    public FeedbackDtos.FeedbackItem create(FeedbackDtos.CreateFeedbackRequest request) {
        CurrentUser user = currentUserService.getCurrentUser();
        TaskRecord task = crmClient.findTaskById(request.taskId())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Task not found", HttpStatus.NOT_FOUND));
        if (user.isEmployee() && !task.participantIds().contains(user.personId())) {
            throw new ApiException("FORBIDDEN", "Employee can submit feedback only for participant tasks", HttpStatus.FORBIDDEN);
        }
        FeedbackRecord created = feedbackClient.createFeedback(request.taskId(), request.targetPersonId(), user.personId(),
            request.categoryId(), request.subcategoryId(), request.rating(), request.comment());
        return toItem(created, true);
    }

    public FeedbackDtos.FeedbackPage my(String taskId, int page, int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        List<FeedbackRecord> items = feedbackClient.findByAuthor(user.personId(), taskId);
        List<FeedbackDtos.FeedbackItem> mapped = PaginationUtils.slice(items, page, size).stream().map(f -> toItem(f, true)).toList();
        return new FeedbackDtos.FeedbackPage(mapped, PaginationUtils.page(items, page, size));
    }

    public FeedbackDtos.FeedbackPage inbox(String taskId, int page, int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        List<FeedbackRecord> items = feedbackClient.findInbox(user.personId(), taskId);
        List<FeedbackDtos.FeedbackInboxItem> mapped = PaginationUtils.slice(items, page, size).stream().map(this::toInbox).toList();
        return new FeedbackDtos.FeedbackPage(mapped, PaginationUtils.page(items, page, size));
    }

    public FeedbackDtos.FeedbackPage raw(String taskId, String targetPersonId, String authorPersonId, int page, int size) {
        if (!currentUserService.getCurrentUser().isAdmin()) {
            throw new ApiException("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN);
        }
        List<FeedbackRecord> items = feedbackClient.findRaw(taskId, targetPersonId, authorPersonId);
        List<FeedbackDtos.FeedbackItem> mapped = PaginationUtils.slice(items, page, size).stream().map(f -> toItem(f, false)).toList();
        return new FeedbackDtos.FeedbackPage(mapped, PaginationUtils.page(items, page, size));
    }

    private FeedbackDtos.FeedbackItem toItem(FeedbackRecord f, boolean hideAuthor) {
        return new FeedbackDtos.FeedbackItem(
            f.id(), f.taskId(), f.targetPersonId(), hideAuthor ? null : f.authorPersonId(),
            f.categoryId(), f.subcategoryId(), f.rating(), f.comment(), f.createdAt().toString(),
            new FeedbackDtos.VisibilityDto(true)
        );
    }

    private FeedbackDtos.FeedbackInboxItem toInbox(FeedbackRecord f) {
        return new FeedbackDtos.FeedbackInboxItem(
            f.id(), f.taskId(), f.targetPersonId(), f.categoryId(), f.subcategoryId(), f.rating(), f.comment(),
            f.createdAt().toString(), new FeedbackDtos.VisibilityDto(true)
        );
    }
}

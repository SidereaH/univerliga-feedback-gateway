package com.univerliga.gateway.service;

import com.univerliga.gateway.client.CrmClient;
import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.dto.FeedbackDtos;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import com.univerliga.gateway.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeedbackService {
    private final FeedbackClient feedbackClient;
    private final CrmClient crmClient;
    private final com.univerliga.gateway.security.CurrentUserService currentUserService;

    public FeedbackService(FeedbackClient feedbackClient,
                           CrmClient crmClient,
                           com.univerliga.gateway.security.CurrentUserService currentUserService) {
        this.feedbackClient = feedbackClient;
        this.crmClient = crmClient;
        this.currentUserService = currentUserService;
    }

    public FeedbackDtos.CategoriesResponse categories() {
        return new FeedbackDtos.CategoriesResponse(
            feedbackClient.categories().stream()
                .map(c -> new FeedbackDtos.CategoryDto(c.id(), c.name(),
                    c.subcategories().stream()
                        .map(s -> new FeedbackDtos.SubcategoryDto(s.id(), s.name(), s.polarity().name(), s.active()))
                        .toList()))
                .toList()
        );
    }

    public FeedbackDtos.ReviewResponse create(FeedbackDtos.CreateReviewRequest request) {
        CurrentUser user = currentUserService.getCurrentUser();
        ResolvedContext context = resolveContext(request.contextType(), request.contextRef(), request.taskId());
        validateTaskAccess(user, context);
        validateTags(request.tagIds());

        Optional<FeedbackRecord> duplicate = feedbackClient.findDuplicate(user.personId(), request.targetPersonId(),
            context.contextType(), context.contextRef());
        if (duplicate.isPresent()) {
            throw new ApiException("DUPLICATE_REVIEW", "Review for this context already exists", HttpStatus.CONFLICT,
                List.of(new ApiErrorDetail("existingReviewId", duplicate.get().id())));
        }

        FeedbackRecord created = feedbackClient.createReview(new FeedbackRecord(
            null,
            request.targetPersonId(),
            user.personId(),
            context.contextType(),
            context.contextRef(),
            request.rating(),
            request.sentiment() != null ? request.sentiment() : inferSentiment(request.rating(), request.tagIds()),
            request.tagIds() == null ? List.of() : request.tagIds(),
            request.comment(),
            null,
            null
        ));
        return toReview(created);
    }

    public FeedbackDtos.ReviewResponse update(String reviewId, FeedbackDtos.UpdateReviewRequest request) {
        CurrentUser user = currentUserService.getCurrentUser();
        FeedbackRecord existing = feedbackClient.findById(reviewId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Review not found", HttpStatus.NOT_FOUND));
        if (!(existing.authorPersonId().equals(user.personId()) || user.isAdmin() || user.isHr())) {
            throw new ApiException("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN);
        }
        if (request.tagIds() != null) {
            validateTags(request.tagIds());
        }
        FeedbackRecord updated = feedbackClient.updateReview(
            reviewId,
            request.rating(),
            request.sentiment(),
            request.tagIds(),
            request.comment()
        );
        return toReview(updated);
    }

    public FeedbackDtos.ReviewPage my(FeedbackRecord.ContextType contextType, String contextRef, int page, int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        List<FeedbackRecord> items = feedbackClient.findMy(user.personId(), contextType, contextRef);
        List<FeedbackDtos.ReviewResponse> mapped = PaginationUtils.slice(items, page, size).stream().map(this::toReview).toList();
        return new FeedbackDtos.ReviewPage(mapped, PaginationUtils.page(items, page, size));
    }

    public FeedbackDtos.ReviewPage inbox(FeedbackRecord.ContextType contextType, String contextRef, int page, int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        List<FeedbackRecord> source;
        if (user.isEmployee() && !user.isAdmin() && !user.isHr() && !user.isManager()) {
            source = feedbackClient.findInbox(user.personId(), contextType, contextRef);
        } else if (user.isManager()) {
            source = managerInbox(contextType, contextRef, user.personId());
        } else {
            source = feedbackClient.findRaw(contextType, contextRef, null, null);
        }
        List<FeedbackDtos.ReviewResponse> mapped = PaginationUtils.slice(source, page, size).stream().map(this::toReview).toList();
        return new FeedbackDtos.ReviewPage(mapped, PaginationUtils.page(source, page, size));
    }

    public FeedbackDtos.ReviewPage raw(FeedbackRecord.ContextType contextType,
                                       String contextRef,
                                       String targetPersonId,
                                       String authorPersonId,
                                       int page,
                                       int size) {
        CurrentUser user = currentUserService.getCurrentUser();
        if (!user.isAdmin() && !user.isHr()) {
            throw new ApiException("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN);
        }
        List<FeedbackRecord> items = feedbackClient.findRaw(contextType, contextRef, targetPersonId, authorPersonId);
        List<FeedbackDtos.RawReviewResponse> mapped = PaginationUtils.slice(items, page, size).stream().map(this::toRawReview).toList();
        return new FeedbackDtos.ReviewPage(mapped, PaginationUtils.page(items, page, size));
    }

    private List<FeedbackRecord> managerInbox(FeedbackRecord.ContextType contextType, String contextRef, String managerId) {
        PersonRecord manager = crmClient.findPersonById(managerId).orElse(null);
        if (manager == null) {
            return List.of();
        }
        Set<String> teamPeople = crmClient.findPeople(null, null, manager.teamId()).stream()
            .map(PersonRecord::id)
            .collect(Collectors.toSet());
        Map<String, FeedbackRecord> uniqueById = new LinkedHashMap<>();
        for (String targetPersonId : teamPeople) {
            for (FeedbackRecord review : feedbackClient.findInbox(targetPersonId, contextType, contextRef)) {
                uniqueById.put(review.id(), review);
            }
        }
        return List.copyOf(uniqueById.values());
    }

    private void validateTaskAccess(CurrentUser user, ResolvedContext context) {
        if (context.contextType() != FeedbackRecord.ContextType.TASK) {
            return;
        }
        TaskRecord task = crmClient.findTaskById(context.contextRef())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Task not found", HttpStatus.NOT_FOUND));
        if (user.isEmployee() && !task.participantIds().contains(user.personId())) {
            throw new ApiException("FORBIDDEN", "Employee can submit feedback only for participant tasks", HttpStatus.FORBIDDEN);
        }
    }

    private void validateTags(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new ApiErrorDetail("tagIds", "must contain from 1 to 3 tags")));
        }
        if (tagIds.size() > 3) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new ApiErrorDetail("tagIds", "must contain from 1 to 3 tags")));
        }
        Map<String, CategoryRecord.SubcategoryRecord> subcategories = subcategoryById();
        List<ApiErrorDetail> errors = new ArrayList<>();
        for (String tagId : tagIds) {
            CategoryRecord.SubcategoryRecord sub = subcategories.get(tagId);
            if (sub == null) {
                errors.add(new ApiErrorDetail("tagIds", "unknown tag: " + tagId));
            } else if (!sub.active()) {
                errors.add(new ApiErrorDetail("tagIds", "inactive tag: " + tagId));
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST, errors);
        }
    }

    private Map<String, CategoryRecord.SubcategoryRecord> subcategoryById() {
        return feedbackClient.categories().stream()
            .flatMap(c -> c.subcategories().stream())
            .collect(Collectors.toMap(CategoryRecord.SubcategoryRecord::id, Function.identity()));
    }

    private FeedbackRecord.Sentiment inferSentiment(Integer rating, List<String> tagIds) {
        if (rating != null) {
            if (rating >= 4) {
                return FeedbackRecord.Sentiment.POSITIVE;
            }
            if (rating <= 2) {
                return FeedbackRecord.Sentiment.NEGATIVE;
            }
        }
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        Map<String, CategoryRecord.SubcategoryRecord> subMap = subcategoryById();
        long positive = tagIds.stream()
            .map(subMap::get)
            .filter(s -> s != null && s.polarity() == CategoryRecord.SubcategoryRecord.Polarity.POSITIVE)
            .count();
        long negative = tagIds.stream()
            .map(subMap::get)
            .filter(s -> s != null && s.polarity() == CategoryRecord.SubcategoryRecord.Polarity.NEGATIVE)
            .count();
        if (positive == negative) {
            return null;
        }
        return positive > negative ? FeedbackRecord.Sentiment.POSITIVE : FeedbackRecord.Sentiment.NEGATIVE;
    }

    private ResolvedContext resolveContext(FeedbackRecord.ContextType contextType, String contextRef, String taskId) {
        if ((contextType == null || contextRef == null || contextRef.isBlank()) && taskId != null && !taskId.isBlank()) {
            return new ResolvedContext(FeedbackRecord.ContextType.TASK, taskId);
        }
        if (contextType == null || contextRef == null || contextRef.isBlank()) {
            throw new ApiException("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST,
                List.of(new ApiErrorDetail("context", "contextType and contextRef are required")));
        }
        return new ResolvedContext(contextType, contextRef);
    }

    private FeedbackDtos.ReviewResponse toReview(FeedbackRecord f) {
        return new FeedbackDtos.ReviewResponse(
            f.id(),
            f.createdAt().toString(),
            f.updatedAt() != null ? f.updatedAt().toString() : null,
            f.targetPersonId(),
            f.contextType(),
            f.contextRef(),
            f.rating(),
            f.sentiment(),
            f.tagIds(),
            f.comment(),
            new FeedbackDtos.VisibilityDto(true)
        );
    }

    private FeedbackDtos.RawReviewResponse toRawReview(FeedbackRecord f) {
        return new FeedbackDtos.RawReviewResponse(
            f.id(),
            f.createdAt().toString(),
            f.updatedAt() != null ? f.updatedAt().toString() : null,
            f.targetPersonId(),
            f.authorPersonId(),
            f.contextType(),
            f.contextRef(),
            f.rating(),
            f.sentiment(),
            f.tagIds(),
            f.comment(),
            new FeedbackDtos.VisibilityDto(true)
        );
    }

    private record ResolvedContext(FeedbackRecord.ContextType contextType, String contextRef) {
    }
}

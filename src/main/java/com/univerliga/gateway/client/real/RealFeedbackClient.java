package com.univerliga.gateway.client.real;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univerliga.gateway.client.FeedbackClient;
import com.univerliga.gateway.error.ApiErrorDetail;
import com.univerliga.gateway.error.ApiException;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "real")
public class RealFeedbackClient implements FeedbackClient {
    private static final ParameterizedTypeReference<Envelope<CategoriesResponse>> CATEGORIES_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<ReviewResponseDto>> REVIEW_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<ReviewListResponseDto>> REVIEW_LIST_TYPE =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<Envelope<RawReviewListResponseDto>> RAW_REVIEW_LIST_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealFeedbackClient(@Qualifier("feedbackRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<CategoryRecord> categories() {
        try {
            CategoriesResponse response = requireData(restClient.get()
                .uri("/api/v1/feedback/categories")
                .retrieve()
                .body(CATEGORIES_TYPE));
            return response.items().stream()
                .map(c -> new CategoryRecord(
                    c.id(),
                    c.name(),
                    c.subcategories().stream()
                        .map(s -> new CategoryRecord.SubcategoryRecord(
                            s.id(),
                            s.name(),
                            CategoryRecord.SubcategoryRecord.Polarity.valueOf(s.polarity()),
                            s.active()
                        ))
                        .toList()
                ))
                .toList();
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public FeedbackRecord createReview(FeedbackRecord draft) {
        try {
            ReviewResponseDto review = requireData(restClient.post()
                .uri("/api/v1/feedback")
                .body(new CreateReviewRequest(
                    draft.targetPersonId(),
                    draft.contextType(),
                    draft.contextRef(),
                    draft.rating(),
                    draft.sentiment(),
                    draft.tagIds(),
                    draft.comment()
                ))
                .retrieve()
                .body(REVIEW_TYPE));
            return toFeedbackRecord(review, currentPersonId().orElse(draft.authorPersonId()));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public Optional<FeedbackRecord> findById(String reviewId) {
        String currentAuthor = currentPersonId().orElse(null);

        for (FeedbackRecord review : findMy(currentAuthor, null, null)) {
            if (review.id().equals(reviewId)) {
                return Optional.of(review);
            }
        }

        try {
            for (FeedbackRecord review : findRaw(null, null, null, null)) {
                if (review.id().equals(reviewId)) {
                    return Optional.of(review);
                }
            }
        } catch (ApiException ex) {
            if (ex.getStatus() != HttpStatus.FORBIDDEN) {
                throw ex;
            }
        }

        return Optional.empty();
    }

    @Override
    public FeedbackRecord updateReview(String reviewId, Integer rating, FeedbackRecord.Sentiment sentiment, List<String> tagIds, String comment) {
        try {
            ReviewResponseDto review = requireData(restClient.put()
                .uri("/api/v1/feedback/{reviewId}", reviewId)
                .body(new UpdateReviewRequest(rating, sentiment, tagIds, comment))
                .retrieve()
                .body(REVIEW_TYPE));
            return toFeedbackRecord(review, currentPersonId().orElse(null));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    @Override
    public List<FeedbackRecord> findMy(String authorPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        List<ReviewResponseDto> all = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            ReviewListResponseDto result = getMyPage(contextType, contextRef, page, 200);
            all.addAll(result.items());
            totalPages = Math.max(result.page().totalPages(), 1);
            page++;
        } while (page <= totalPages);
        String resolvedAuthor = authorPersonId != null ? authorPersonId : currentPersonId().orElse(null);
        return all.stream().map(dto -> toFeedbackRecord(dto, resolvedAuthor)).toList();
    }

    @Override
    public List<FeedbackRecord> findInbox(String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        List<ReviewResponseDto> all = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            ReviewListResponseDto result = getInboxPage(contextType, contextRef, targetPersonId, page, 200);
            all.addAll(result.items());
            totalPages = Math.max(result.page().totalPages(), 1);
            page++;
        } while (page <= totalPages);
        return all.stream().map(dto -> toFeedbackRecord(dto, null)).toList();
    }

    @Override
    public List<FeedbackRecord> findRaw(FeedbackRecord.ContextType contextType, String contextRef, String targetPersonId, String authorPersonId) {
        List<RawReviewResponseDto> all = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            RawReviewListResponseDto result = getRawPage(contextType, contextRef, targetPersonId, authorPersonId, page, 200);
            all.addAll(result.items());
            totalPages = Math.max(result.page().totalPages(), 1);
            page++;
        } while (page <= totalPages);
        return all.stream().map(this::toFeedbackRecord).toList();
    }

    @Override
    public Optional<FeedbackRecord> findDuplicate(String authorPersonId, String targetPersonId, FeedbackRecord.ContextType contextType, String contextRef) {
        return findMy(authorPersonId, contextType, contextRef).stream()
            .filter(r -> targetPersonId.equals(r.targetPersonId()))
            .filter(r -> contextType == r.contextType())
            .filter(r -> contextRef.equals(r.contextRef()))
            .findFirst();
    }

    @Override
    public List<FeedbackRecord> findAll() {
        return findRaw(null, null, null, null);
    }

    private ReviewListResponseDto getMyPage(FeedbackRecord.ContextType contextType, String contextRef, int page, int size) {
        try {
            return requireData(restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/feedback/my")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParamIfPresent("contextType", Optional.ofNullable(contextType))
                    .queryParamIfPresent("contextRef", Optional.ofNullable(contextRef))
                    .build())
                .retrieve()
                .body(REVIEW_LIST_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private ReviewListResponseDto getInboxPage(FeedbackRecord.ContextType contextType,
                                               String contextRef,
                                               String targetPersonId,
                                               int page,
                                               int size) {
        try {
            return requireData(restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/feedback/inbox")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParamIfPresent("contextType", Optional.ofNullable(contextType))
                    .queryParamIfPresent("contextRef", Optional.ofNullable(contextRef))
                    .queryParamIfPresent("targetPersonId", Optional.ofNullable(targetPersonId))
                    .build())
                .retrieve()
                .body(REVIEW_LIST_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private RawReviewListResponseDto getRawPage(FeedbackRecord.ContextType contextType,
                                                String contextRef,
                                                String targetPersonId,
                                                String authorPersonId,
                                                int page,
                                                int size) {
        try {
            return requireData(restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/feedback/raw")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParamIfPresent("contextType", Optional.ofNullable(contextType))
                    .queryParamIfPresent("contextRef", Optional.ofNullable(contextRef))
                    .queryParamIfPresent("targetPersonId", Optional.ofNullable(targetPersonId))
                    .queryParamIfPresent("authorPersonId", Optional.ofNullable(authorPersonId))
                    .build())
                .retrieve()
                .body(RAW_REVIEW_LIST_TYPE));
        } catch (RestClientResponseException ex) {
            throw toApiException(ex);
        } catch (RestClientException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private FeedbackRecord toFeedbackRecord(ReviewResponseDto dto, String authorPersonId) {
        return new FeedbackRecord(
            dto.id(),
            dto.targetPersonId(),
            authorPersonId,
            dto.contextType(),
            dto.contextRef(),
            dto.rating(),
            dto.sentiment(),
            dto.tagIds() == null ? List.of() : dto.tagIds(),
            dto.comment(),
            parseInstant(dto.createdAt()),
            dto.updatedAt() == null ? null : parseInstant(dto.updatedAt())
        );
    }

    private FeedbackRecord toFeedbackRecord(RawReviewResponseDto dto) {
        return new FeedbackRecord(
            dto.id(),
            dto.targetPersonId(),
            dto.authorPersonId(),
            dto.contextType(),
            dto.contextRef(),
            dto.rating(),
            dto.sentiment(),
            dto.tagIds() == null ? List.of() : dto.tagIds(),
            dto.comment(),
            parseInstant(dto.createdAt()),
            dto.updatedAt() == null ? null : parseInstant(dto.updatedAt())
        );
    }

    private Instant parseInstant(String value) {
        return OffsetDateTime.parse(value).toInstant();
    }

    private <T> T requireData(Envelope<T> envelope) {
        if (envelope == null || envelope.data() == null) {
            throw new ApiException("BAD_GATEWAY", "Feedback response is empty", HttpStatus.BAD_GATEWAY);
        }
        return envelope.data();
    }

    private Optional<String> currentPersonId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String explicit = jwtAuth.getToken().getClaimAsString("personId");
            if (explicit != null && !explicit.isBlank()) {
                return Optional.of(explicit);
            }
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            return Optional.ofNullable(mapPersonId(username));
        }
        return Optional.empty();
    }

    private String mapPersonId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        if ("admin".equalsIgnoreCase(username)) {
            return "p_admin";
        }
        if ("manager".equalsIgnoreCase(username)) {
            return "p_manager";
        }
        if ("employee".equalsIgnoreCase(username)) {
            return "p_employee";
        }
        if ("hr".equalsIgnoreCase(username)) {
            return "p_hr";
        }
        return "p_" + username;
    }

    private ApiException toApiException(RestClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            JsonNode error = body.path("error");
            String code = error.path("code").asText();
            String message = error.path("message").asText();
            if (!code.isBlank() && !message.isBlank()) {
                return new ApiException(code, message, status, extractDetails(error.path("details")));
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return new ApiException("DOWNSTREAM_ERROR", "Feedback request failed", status);
    }

    private List<ApiErrorDetail> extractDetails(JsonNode detailsNode) {
        if (!detailsNode.isArray()) {
            return List.of();
        }
        List<ApiErrorDetail> details = new ArrayList<>();
        for (JsonNode node : detailsNode) {
            String field = node.path("field").asText("");
            String issue = node.path("issue").asText(node.path("message").asText(""));
            if (!field.isBlank() || !issue.isBlank()) {
                details.add(new ApiErrorDetail(field, issue));
            }
        }
        return details;
    }

    private ApiException serviceUnavailable(RestClientException ex) {
        return new ApiException("DOWNSTREAM_UNAVAILABLE", "Feedback service unavailable", HttpStatus.BAD_GATEWAY,
            List.of(new ApiErrorDetail("feedback", ex.getMessage())));
    }

    private record Envelope<T>(T data) {
    }

    private record CategoriesResponse(List<CategoryDto> items) {
    }

    private record CategoryDto(String id, String name, boolean active, List<SubcategoryDto> subcategories) {
    }

    private record SubcategoryDto(String id, String name, String polarity, boolean active) {
    }

    private record CreateReviewRequest(String targetPersonId,
                                       FeedbackRecord.ContextType contextType,
                                       String contextRef,
                                       Integer rating,
                                       FeedbackRecord.Sentiment sentiment,
                                       List<String> tagIds,
                                       String comment) {
    }

    private record UpdateReviewRequest(Integer rating,
                                       FeedbackRecord.Sentiment sentiment,
                                       List<String> tagIds,
                                       String comment) {
    }

    private record ReviewResponseDto(String id,
                                     String createdAt,
                                     String updatedAt,
                                     String targetPersonId,
                                     FeedbackRecord.ContextType contextType,
                                     String contextRef,
                                     FeedbackRecord.Sentiment sentiment,
                                     Integer rating,
                                     List<String> tagIds,
                                     String comment) {
    }

    private record RawReviewResponseDto(String id,
                                        String createdAt,
                                        String updatedAt,
                                        String authorPersonId,
                                        String targetPersonId,
                                        FeedbackRecord.ContextType contextType,
                                        String contextRef,
                                        FeedbackRecord.Sentiment sentiment,
                                        Integer rating,
                                        List<String> tagIds,
                                        String comment) {
    }

    private record ReviewListResponseDto(List<ReviewResponseDto> items, PageDto page) {
    }

    private record RawReviewListResponseDto(List<RawReviewResponseDto> items, PageDto page) {
    }

    private record PageDto(int page, int size, long totalItems, int totalPages) {
    }
}

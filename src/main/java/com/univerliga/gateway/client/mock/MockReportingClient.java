package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.model.PersonRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * For top tags share we use count/total count within the same polarity bucket (positive or negative).
 */
@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockReportingClient implements ReportingClient {
    private final MockDataStore dataStore;

    public MockReportingClient(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public ReportDtos.SummaryResponse summary(LocalDate from, LocalDate to, String departmentId, String teamId, String personId) {
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, personId, null);
        ReportDtos.SummaryKpis kpis = buildKpis(reviews, departmentId, teamId, personId);
        return new ReportDtos.SummaryResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeWithPersonDto(departmentId, teamId, personId),
            kpis
        );
    }

    @Override
    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from,
                                                                  LocalDate to,
                                                                  String departmentId,
                                                                  String teamId,
                                                                  String personId) {
        Map<String, String> categoryNames = categoryNames();
        Map<String, String> tagToCategory = subcategoryToCategory();
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, personId, null);

        List<ReportDtos.CategorySeriesItem> series = reviews.stream()
            .filter(r -> r.rating() != null)
            .map(r -> Map.entry(primaryCategoryId(r, tagToCategory), r.rating()))
            .filter(e -> e.getKey() != null)
            .collect(Collectors.groupingBy(Map.Entry::getKey))
            .entrySet().stream()
            .map(e -> new ReportDtos.CategorySeriesItem(
                e.getKey(),
                categoryNames.getOrDefault(e.getKey(), e.getKey()),
                round(e.getValue().stream().map(Map.Entry::getValue).mapToInt(Integer::intValue).average().orElse(0.0)),
                e.getValue().size()
            ))
            .sorted(Comparator.comparingLong(ReportDtos.CategorySeriesItem::count).reversed())
            .toList();

        return new ReportDtos.RatingsByCategoryResponse(series);
    }

    @Override
    public ReportDtos.TrendResponse trend(String metric,
                                          String granularity,
                                          LocalDate from,
                                          LocalDate to,
                                          String departmentId,
                                          String teamId,
                                          String personId) {
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, personId, null);
        Map<String, List<FeedbackRecord>> buckets = reviews.stream()
            .collect(Collectors.groupingBy(r -> bucketKey(toDate(r), granularity)));

        List<String> xAxis = enumerateBuckets(from, to, granularity);
        List<ReportDtos.TrendPoint> points = new ArrayList<>(xAxis.size());
        for (String x : xAxis) {
            List<FeedbackRecord> bucketReviews = buckets.getOrDefault(x, List.of());
            points.add(new ReportDtos.TrendPoint(x, round(metricValue(metric, bucketReviews))));
        }
        return new ReportDtos.TrendResponse(metric, granularity, points);
    }

    @Override
    public ReportDtos.PositivityByPersonResponse positivityByPerson(LocalDate from,
                                                                    LocalDate to,
                                                                    String departmentId,
                                                                    String teamId,
                                                                    int limit,
                                                                    String sort) {
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, null, null);
        Map<String, PersonRecord> people = peopleById();

        List<ReportDtos.PositivityByPersonItem> items = reviews.stream()
            .collect(Collectors.groupingBy(FeedbackRecord::targetPersonId))
            .entrySet().stream()
            .map(e -> toPositivityItem(e.getKey(), e.getValue(), people))
            .filter(i -> i.total() > 0)
            .sorted(positivityComparator(sort))
            .limit(safeLimit(limit, 20))
            .toList();

        return new ReportDtos.PositivityByPersonResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeWithPersonDto(departmentId, teamId, null),
            items
        );
    }

    @Override
    public ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(LocalDate from,
                                                                        LocalDate to,
                                                                        String departmentId,
                                                                        String teamId,
                                                                        String personId,
                                                                        String categoryId,
                                                                        int limit,
                                                                        String sort) {
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, personId, categoryId);
        Map<String, CategoryRecord.SubcategoryRecord> subMap = subcategoryById();

        List<ReportDtos.SubcategoryFrequencyItem> items = reviews.stream()
            .flatMap(r -> r.tagIds().stream().map(tagId -> Map.entry(tagId, resolveSentiment(r, subMap))))
            .collect(Collectors.groupingBy(Map.Entry::getKey))
            .entrySet().stream()
            .map(e -> {
                CategoryRecord.SubcategoryRecord sub = subMap.get(e.getKey());
                long positive = e.getValue().stream().filter(v -> v.getValue() == FeedbackRecord.Sentiment.POSITIVE).count();
                long negative = e.getValue().stream().filter(v -> v.getValue() == FeedbackRecord.Sentiment.NEGATIVE).count();
                return new ReportDtos.SubcategoryFrequencyItem(
                    e.getKey(),
                    sub != null ? sub.name() : e.getKey(),
                    positive,
                    negative,
                    positive + negative
                );
            })
            .filter(i -> i.total() > 0)
            .sorted(subcategoryComparator(sort))
            .limit(safeLimit(limit, 30))
            .toList();

        return new ReportDtos.SubcategoryFrequencyResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeWithCategoryDto(departmentId, teamId, personId, categoryId),
            items
        );
    }

    @Override
    public ReportDtos.TopTagsResponse topTags(LocalDate from,
                                              LocalDate to,
                                              String departmentId,
                                              String teamId,
                                              int limit) {
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, null, null);
        Map<String, CategoryRecord.SubcategoryRecord> subMap = subcategoryById();
        List<String> tags = reviews.stream().flatMap(r -> r.tagIds().stream()).toList();

        List<ReportDtos.TopTagItem> topPositive = topTagsByPolarity(tags, subMap, CategoryRecord.SubcategoryRecord.Polarity.POSITIVE, limit);
        List<ReportDtos.TopTagItem> topNegative = topTagsByPolarity(tags, subMap, CategoryRecord.SubcategoryRecord.Polarity.NEGATIVE, limit);
        return new ReportDtos.TopTagsResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            topPositive,
            topNegative
        );
    }

    @Override
    public ReportDtos.DashboardResponse dashboard(LocalDate from,
                                                  LocalDate to,
                                                  String departmentId,
                                                  String teamId,
                                                  String personId) {
        List<FeedbackRecord> reviews = filteredFeedback(from, to, departmentId, teamId, personId, null);
        ReportDtos.SummaryKpis kpis = buildKpis(reviews, departmentId, teamId, personId);
        ReportDtos.TrendResponse trend = trend("responses", "month", from, to, departmentId, teamId, personId);

        return new ReportDtos.DashboardResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeWithPersonDto(departmentId, teamId, personId),
            kpis,
            new ReportDtos.DashboardCharts(
                ratingsByCategory(from, to, departmentId, teamId, personId).series(),
                new ReportDtos.DashboardTrend(trend.metric(), trend.granularity(), trend.points()),
                positivityByPerson(from, to, departmentId, teamId, 20, "total").items(),
                subcategoryFrequency(from, to, departmentId, teamId, personId, null, 30, "total").items()
            ),
            new ReportDtos.DashboardInsights(topTags(from, to, departmentId, teamId, 5))
        );
    }

    private ReportDtos.SummaryKpis buildKpis(List<FeedbackRecord> reviews, String departmentId, String teamId, String personId) {
        long responses = reviews.size();
        long positive = reviews.stream().filter(r -> resolveSentiment(r, subcategoryById()) == FeedbackRecord.Sentiment.POSITIVE).count();
        long negative = reviews.stream().filter(r -> resolveSentiment(r, subcategoryById()) == FeedbackRecord.Sentiment.NEGATIVE).count();
        long uniqueAuthors = reviews.stream().map(FeedbackRecord::authorPersonId).filter(Objects::nonNull).distinct().count();
        long uniqueTargets = reviews.stream().map(FeedbackRecord::targetPersonId).filter(Objects::nonNull).distinct().count();
        double avgRating = round(reviews.stream().filter(r -> r.rating() != null).mapToInt(FeedbackRecord::rating).average().orElse(0.0));
        long pnTotal = positive + negative;
        double positiveShare = pnTotal == 0 ? 0.0 : round(positive / (double) pnTotal);
        double negativeShare = pnTotal == 0 ? 0.0 : round(negative / (double) pnTotal);
        long totalTargetsInScope = peopleInScope(departmentId, teamId, personId).size();
        long targetsWithAtLeastOne = reviews.stream().map(FeedbackRecord::targetPersonId).distinct().count();
        double coverageShare = totalTargetsInScope == 0 ? 0.0 : round(targetsWithAtLeastOne / (double) totalTargetsInScope);
        return new ReportDtos.SummaryKpis(
            responses,
            positive,
            negative,
            uniqueAuthors,
            uniqueTargets,
            avgRating,
            positiveShare,
            negativeShare,
            new ReportDtos.CoverageKpi(targetsWithAtLeastOne, totalTargetsInScope, coverageShare)
        );
    }

    private List<ReportDtos.TopTagItem> topTagsByPolarity(List<String> tags,
                                                          Map<String, CategoryRecord.SubcategoryRecord> subMap,
                                                          CategoryRecord.SubcategoryRecord.Polarity polarity,
                                                          int limit) {
        Map<String, Long> counts = tags.stream()
            .filter(tagId -> {
                CategoryRecord.SubcategoryRecord sub = subMap.get(tagId);
                return sub != null && sub.polarity() == polarity;
            })
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(safeLimit(limit, 5))
            .map(e -> {
                CategoryRecord.SubcategoryRecord sub = subMap.get(e.getKey());
                double share = total == 0 ? 0.0 : round(e.getValue() / (double) total);
                return new ReportDtos.TopTagItem(e.getKey(), sub != null ? sub.name() : e.getKey(), e.getValue(), share);
            })
            .toList();
    }

    private List<FeedbackRecord> filteredFeedback(LocalDate from,
                                                  LocalDate to,
                                                  String departmentId,
                                                  String teamId,
                                                  String personId,
                                                  String categoryId) {
        Set<String> peopleInScope = peopleInScope(departmentId, teamId, personId);
        Map<String, String> tagToCategory = subcategoryToCategory();
        return dataStore.feedback().stream()
            .filter(r -> {
                LocalDate date = toDate(r);
                return !(date.isBefore(from) || date.isAfter(to));
            })
            .filter(r -> peopleInScope.contains(r.targetPersonId()))
            .filter(r -> categoryId == null || r.tagIds().stream().anyMatch(tag -> categoryId.equals(tagToCategory.get(tag))))
            .toList();
    }

    private Set<String> peopleInScope(String departmentId, String teamId, String personId) {
        return dataStore.people().stream()
            .filter(p -> personId == null || personId.equals(p.id()))
            .filter(p -> departmentId == null || departmentId.equals(p.departmentId()))
            .filter(p -> teamId == null || teamId.equals(p.teamId()))
            .map(PersonRecord::id)
            .collect(Collectors.toSet());
    }

    private LocalDate toDate(FeedbackRecord review) {
        return LocalDate.ofInstant(review.createdAt(), ZoneOffset.UTC);
    }

    private String primaryCategoryId(FeedbackRecord review, Map<String, String> tagToCategory) {
        if (review.tagIds() == null || review.tagIds().isEmpty()) {
            return null;
        }
        return tagToCategory.get(review.tagIds().getFirst());
    }

    private String bucketKey(LocalDate date, String granularity) {
        if ("week".equals(granularity)) {
            int year = date.get(IsoFields.WEEK_BASED_YEAR);
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            return "%d-W%02d".formatted(year, week);
        }
        return "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
    }

    private List<String> enumerateBuckets(LocalDate from, LocalDate to, String granularity) {
        List<String> axis = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            String key = bucketKey(cursor, granularity);
            if (axis.isEmpty() || !axis.getLast().equals(key)) {
                axis.add(key);
            }
            cursor = "week".equals(granularity) ? cursor.plusWeeks(1) : cursor.plusMonths(1).withDayOfMonth(1);
        }
        return axis;
    }

    private double metricValue(String metric, List<FeedbackRecord> reviews) {
        long responses = reviews.size();
        long positive = reviews.stream().filter(r -> resolveSentiment(r, subcategoryById()) == FeedbackRecord.Sentiment.POSITIVE).count();
        long negative = reviews.stream().filter(r -> resolveSentiment(r, subcategoryById()) == FeedbackRecord.Sentiment.NEGATIVE).count();
        long pnTotal = positive + negative;
        if ("avgRating".equals(metric)) {
            return reviews.stream().filter(r -> r.rating() != null).mapToInt(FeedbackRecord::rating).average().orElse(0.0);
        }
        if ("positiveShare".equals(metric)) {
            return pnTotal == 0 ? 0.0 : positive / (double) pnTotal;
        }
        if ("negativeShare".equals(metric)) {
            return pnTotal == 0 ? 0.0 : negative / (double) pnTotal;
        }
        return responses;
    }

    private ReportDtos.PositivityByPersonItem toPositivityItem(String personId,
                                                               List<FeedbackRecord> reviews,
                                                               Map<String, PersonRecord> people) {
        long positive = reviews.stream().filter(r -> resolveSentiment(r, subcategoryById()) == FeedbackRecord.Sentiment.POSITIVE).count();
        long negative = reviews.stream().filter(r -> resolveSentiment(r, subcategoryById()) == FeedbackRecord.Sentiment.NEGATIVE).count();
        long total = positive + negative;
        double avgRating = round(reviews.stream().filter(r -> r.rating() != null).mapToInt(FeedbackRecord::rating).average().orElse(0.0));
        String displayName = people.containsKey(personId) ? people.get(personId).displayName() : personId;
        return new ReportDtos.PositivityByPersonItem(personId, displayName, positive, negative, total, avgRating);
    }

    private FeedbackRecord.Sentiment resolveSentiment(FeedbackRecord review, Map<String, CategoryRecord.SubcategoryRecord> subMap) {
        if (review.sentiment() != null) {
            return review.sentiment();
        }
        if (review.rating() != null) {
            if (review.rating() >= 4) {
                return FeedbackRecord.Sentiment.POSITIVE;
            }
            if (review.rating() <= 2) {
                return FeedbackRecord.Sentiment.NEGATIVE;
            }
        }
        long pos = review.tagIds().stream()
            .map(subMap::get)
            .filter(s -> s != null && s.polarity() == CategoryRecord.SubcategoryRecord.Polarity.POSITIVE)
            .count();
        long neg = review.tagIds().stream()
            .map(subMap::get)
            .filter(s -> s != null && s.polarity() == CategoryRecord.SubcategoryRecord.Polarity.NEGATIVE)
            .count();
        if (pos == neg) {
            return null;
        }
        return pos > neg ? FeedbackRecord.Sentiment.POSITIVE : FeedbackRecord.Sentiment.NEGATIVE;
    }

    private Map<String, String> categoryNames() {
        return dataStore.categories().stream().collect(Collectors.toMap(CategoryRecord::id, CategoryRecord::name));
    }

    private Map<String, String> subcategoryToCategory() {
        Map<String, String> map = new LinkedHashMap<>();
        for (CategoryRecord category : dataStore.categories()) {
            for (CategoryRecord.SubcategoryRecord subcategory : category.subcategories()) {
                map.put(subcategory.id(), category.id());
            }
        }
        return map;
    }

    private Map<String, CategoryRecord.SubcategoryRecord> subcategoryById() {
        return dataStore.categories().stream()
            .flatMap(c -> c.subcategories().stream())
            .collect(Collectors.toMap(CategoryRecord.SubcategoryRecord::id, Function.identity()));
    }

    private Map<String, PersonRecord> peopleById() {
        return dataStore.people().stream().collect(Collectors.toMap(PersonRecord::id, Function.identity()));
    }

    private Comparator<ReportDtos.PositivityByPersonItem> positivityComparator(String sort) {
        if ("positive".equals(sort)) {
            return Comparator.comparingLong(ReportDtos.PositivityByPersonItem::positive).reversed();
        }
        if ("negative".equals(sort)) {
            return Comparator.comparingLong(ReportDtos.PositivityByPersonItem::negative).reversed();
        }
        if ("avgRating".equals(sort)) {
            return Comparator.comparingDouble(ReportDtos.PositivityByPersonItem::avgRating).reversed();
        }
        return Comparator.comparingLong(ReportDtos.PositivityByPersonItem::total).reversed();
    }

    private Comparator<ReportDtos.SubcategoryFrequencyItem> subcategoryComparator(String sort) {
        if ("positive".equals(sort)) {
            return Comparator.comparingLong(ReportDtos.SubcategoryFrequencyItem::positive).reversed();
        }
        if ("negative".equals(sort)) {
            return Comparator.comparingLong(ReportDtos.SubcategoryFrequencyItem::negative).reversed();
        }
        return Comparator.comparingLong(ReportDtos.SubcategoryFrequencyItem::total).reversed();
    }

    private int safeLimit(int limit, int defaultValue) {
        if (limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, 200);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

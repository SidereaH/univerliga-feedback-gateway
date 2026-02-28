package com.univerliga.gateway.client.mock;

import com.univerliga.gateway.client.ReportingClient;
import com.univerliga.gateway.dto.ReportDtos;
import com.univerliga.gateway.model.CategoryRecord;
import com.univerliga.gateway.model.FeedbackRecord;
import com.univerliga.gateway.model.PersonRecord;
import com.univerliga.gateway.model.TaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "gateway", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockReportingClient implements ReportingClient {

    private final MockDataStore dataStore;

    public MockReportingClient(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public ReportDtos.SummaryResponse summary(LocalDate from, LocalDate to, String departmentId, String teamId) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, departmentId, teamId, null, null);
        long responses = feedback.size();
        double avgRating = average(feedback.stream().mapToInt(FeedbackRecord::rating).boxed().toList());
        long positive = feedback.stream().filter(this::isPositive).count();
        long negative = feedback.stream().filter(this::isNegative).count();
        long pnTotal = positive + negative;
        double positiveShare = pnTotal == 0 ? 0.0 : round(positive / (double) pnTotal);
        return new ReportDtos.SummaryResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.Kpis(responses, avgRating, positiveShare)
        );
    }

    @Override
    public ReportDtos.RatingsByCategoryResponse ratingsByCategory(LocalDate from, LocalDate to, String teamId) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, null, teamId, null, null);
        Map<String, String> categoryNames = categoryNames();

        List<ReportDtos.CategorySeriesItem> series = feedback.stream()
            .collect(Collectors.groupingBy(FeedbackRecord::categoryId))
            .entrySet().stream()
            .map(e -> new ReportDtos.CategorySeriesItem(
                e.getKey(),
                categoryNames.getOrDefault(e.getKey(), e.getKey()),
                round(average(e.getValue().stream().map(FeedbackRecord::rating).toList())),
                e.getValue().size()
            ))
            .sorted(Comparator.comparingLong(ReportDtos.CategorySeriesItem::count).reversed())
            .toList();

        return new ReportDtos.RatingsByCategoryResponse(series);
    }

    @Override
    public ReportDtos.TrendResponse trend(String metric, String period, LocalDate from, LocalDate to, String teamId) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, null, teamId, null, null);
        Map<String, TaskRecord> tasks = tasksById();

        Map<YearMonth, List<FeedbackRecord>> byMonth = feedback.stream()
            .collect(Collectors.groupingBy(f -> YearMonth.from(tasks.get(f.taskId()).periodFrom())));

        List<ReportDtos.TrendPoint> points = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            List<FeedbackRecord> monthFeedback = byMonth.getOrDefault(cursor, List.of());
            double value;
            if ("avgRating".equals(metric)) {
                value = average(monthFeedback.stream().map(FeedbackRecord::rating).toList());
            } else {
                value = monthFeedback.size();
            }
            points.add(new ReportDtos.TrendPoint(cursor.toString(), round(value)));
            cursor = cursor.plusMonths(1);
        }

        return new ReportDtos.TrendResponse(metric, points);
    }

    @Override
    public ReportDtos.PositivityByPersonResponse positivityByPerson(LocalDate from,
                                                                    LocalDate to,
                                                                    String departmentId,
                                                                    String teamId,
                                                                    int limit,
                                                                    String sort) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, departmentId, teamId, null, null);
        Map<String, PersonRecord> people = peopleById();
        Map<String, List<FeedbackRecord>> grouped = feedback.stream()
            .collect(Collectors.groupingBy(FeedbackRecord::targetPersonId));

        List<ReportDtos.PositivityByPersonItem> items = grouped.entrySet().stream()
            .map(e -> toPositivityItem(e.getKey(), e.getValue(), people))
            .filter(i -> i.total() > 0)
            .sorted(positivityComparator(sort))
            .limit(safeLimit(limit, 20))
            .toList();

        return new ReportDtos.PositivityByPersonResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeDto(departmentId, teamId),
            items
        );
    }

    @Override
    public ReportDtos.SubcategoryFrequencyResponse subcategoryFrequency(LocalDate from,
                                                                        LocalDate to,
                                                                        String departmentId,
                                                                        String teamId,
                                                                        String categoryId,
                                                                        int limit,
                                                                        String sort) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, departmentId, teamId, null, categoryId);
        Map<String, String> subcategoryNames = subcategoryNames();

        List<ReportDtos.SubcategoryFrequencyItem> items = feedback.stream()
            .collect(Collectors.groupingBy(FeedbackRecord::subcategoryId))
            .entrySet().stream()
            .map(e -> {
                long positive = e.getValue().stream().filter(this::isPositive).count();
                long negative = e.getValue().stream().filter(this::isNegative).count();
                long total = positive + negative;
                return new ReportDtos.SubcategoryFrequencyItem(
                    e.getKey(),
                    subcategoryNames.getOrDefault(e.getKey(), e.getKey()),
                    positive,
                    negative,
                    total
                );
            })
            .filter(i -> i.total() > 0)
            .sorted(subcategoryComparator(sort))
            .limit(safeLimit(limit, 30))
            .toList();

        return new ReportDtos.SubcategoryFrequencyResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeWithCategoryDto(departmentId, teamId, categoryId),
            items
        );
    }

    @Override
    public ReportDtos.DashboardResponse dashboard(LocalDate from,
                                                  LocalDate to,
                                                  String departmentId,
                                                  String teamId,
                                                  String personId) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, departmentId, teamId, personId, null);
        long responses = feedback.size();
        long uniqueAuthors = feedback.stream().map(FeedbackRecord::authorPersonId).filter(Objects::nonNull).distinct().count();
        long uniqueTargets = feedback.stream().map(FeedbackRecord::targetPersonId).filter(Objects::nonNull).distinct().count();
        double avgRating = average(feedback.stream().map(FeedbackRecord::rating).toList());
        long positive = feedback.stream().filter(this::isPositive).count();
        long negative = feedback.stream().filter(this::isNegative).count();
        long pnTotal = positive + negative;
        double positiveShare = pnTotal == 0 ? 0.0 : round(positive / (double) pnTotal);
        double negativeShare = pnTotal == 0 ? 0.0 : round(negative / (double) pnTotal);

        List<ReportDtos.CategorySeriesItem> ratingsByCategory = ratingsByCategory(from, to, teamId).series();
        List<ReportDtos.PositivityByPersonItem> positivityByPerson =
            positivityByPerson(from, to, departmentId, teamId, 20, "total").items();
        List<ReportDtos.SubcategoryFrequencyItem> subcategoryFrequency =
            subcategoryFrequency(from, to, departmentId, teamId, null, 30, "total").items();
        ReportDtos.TrendResponse trend = trend("responses", "month", from, to, teamId);

        return new ReportDtos.DashboardResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            new ReportDtos.ScopeWithPersonDto(departmentId, teamId, personId),
            new ReportDtos.DashboardKpis(responses, uniqueAuthors, uniqueTargets, avgRating, positiveShare, negativeShare),
            new ReportDtos.DashboardCharts(
                ratingsByCategory,
                new ReportDtos.DashboardTrend(trend.metric(), "month", trend.points()),
                positivityByPerson,
                subcategoryFrequency
            )
        );
    }

    @Override
    public ReportDtos.TopSubcategoriesInsightsResponse topSubcategories(LocalDate from,
                                                                        LocalDate to,
                                                                        String departmentId,
                                                                        String teamId,
                                                                        int limit) {
        List<FeedbackRecord> feedback = filteredFeedback(from, to, departmentId, teamId, null, null);
        Map<String, String> subcategoryNames = subcategoryNames();

        List<ReportDtos.TopSubcategoryInsightItem> all = feedback.stream()
            .collect(Collectors.groupingBy(FeedbackRecord::subcategoryId))
            .entrySet().stream()
            .map(e -> new ReportDtos.TopSubcategoryInsightItem(
                e.getKey(),
                subcategoryNames.getOrDefault(e.getKey(), e.getKey()),
                round(average(e.getValue().stream().map(FeedbackRecord::rating).toList())),
                e.getValue().size()
            ))
            .toList();

        int safeLimit = safeLimit(limit, 5);
        List<ReportDtos.TopSubcategoryInsightItem> best = all.stream()
            .sorted(Comparator.comparingDouble(ReportDtos.TopSubcategoryInsightItem::avgRating).reversed()
                .thenComparingLong(ReportDtos.TopSubcategoryInsightItem::count).reversed())
            .limit(safeLimit)
            .toList();

        List<ReportDtos.TopSubcategoryInsightItem> worst = all.stream()
            .sorted(Comparator.comparingDouble(ReportDtos.TopSubcategoryInsightItem::avgRating)
                .thenComparingLong(ReportDtos.TopSubcategoryInsightItem::count).reversed())
            .limit(safeLimit)
            .toList();

        return new ReportDtos.TopSubcategoriesInsightsResponse(
            new ReportDtos.ReportPeriod(from.toString(), to.toString()),
            best,
            worst
        );
    }

    private ReportDtos.PositivityByPersonItem toPositivityItem(String personId,
                                                               List<FeedbackRecord> feedback,
                                                               Map<String, PersonRecord> people) {
        long positive = feedback.stream().filter(this::isPositive).count();
        long negative = feedback.stream().filter(this::isNegative).count();
        long total = positive + negative;
        double avgRating = average(
            feedback.stream().filter(f -> isPositive(f) || isNegative(f)).map(FeedbackRecord::rating).toList()
        );
        String displayName = people.containsKey(personId) ? people.get(personId).displayName() : personId;
        return new ReportDtos.PositivityByPersonItem(personId, displayName, positive, negative, total, round(avgRating));
    }

    private List<FeedbackRecord> filteredFeedback(LocalDate from,
                                                  LocalDate to,
                                                  String departmentId,
                                                  String teamId,
                                                  String personId,
                                                  String categoryId) {
        Map<String, TaskRecord> tasks = tasksById();
        Map<String, PersonRecord> people = peopleById();

        return dataStore.feedback().stream()
            .filter(f -> {
                TaskRecord task = tasks.get(f.taskId());
                return task != null && !task.periodTo().isBefore(from) && !task.periodFrom().isAfter(to);
            })
            .filter(f -> categoryId == null || categoryId.equals(f.categoryId()))
            .filter(f -> personId == null || personId.equals(f.targetPersonId()))
            .filter(f -> {
                PersonRecord target = people.get(f.targetPersonId());
                if (target == null) {
                    return false;
                }
                boolean departmentMatch = departmentId == null || departmentId.equals(target.departmentId());
                boolean teamMatch = teamId == null || teamId.equals(target.teamId());
                return departmentMatch && teamMatch;
            })
            .toList();
    }

    private Map<String, String> categoryNames() {
        return dataStore.categories().stream().collect(Collectors.toMap(CategoryRecord::id, CategoryRecord::name));
    }

    private Map<String, String> subcategoryNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (CategoryRecord category : dataStore.categories()) {
            for (CategoryRecord.SubcategoryRecord sub : category.subcategories()) {
                names.put(sub.id(), sub.name());
            }
        }
        return names;
    }

    private Map<String, PersonRecord> peopleById() {
        return dataStore.people().stream().collect(Collectors.toMap(PersonRecord::id, Function.identity()));
    }

    private Map<String, TaskRecord> tasksById() {
        return dataStore.tasks().stream().collect(Collectors.toMap(TaskRecord::id, Function.identity()));
    }

    private boolean isPositive(FeedbackRecord f) {
        return f.rating() >= 4;
    }

    private boolean isNegative(FeedbackRecord f) {
        return f.rating() <= 2;
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

    private double average(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

package br.com.acorde.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummary(
        LocalDate from,
        LocalDate to,
        long activityCount,
        long pendingCount,
        BigDecimal emissionsKgCo2e,
        List<SourceSummary> sources,
        List<PeriodSummary> periods,
        String coverageNote) {
    public record SourceSummary(String category, long activityCount, long validCount,
                                long pendingCount, BigDecimal emissionsKgCo2e) {}

    public record PeriodSummary(LocalDate periodStart, LocalDate periodEnd, long activityCount,
                                long validCount, long pendingCount, BigDecimal emissionsKgCo2e) {}
}

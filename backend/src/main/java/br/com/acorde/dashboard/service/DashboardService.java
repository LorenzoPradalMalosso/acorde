package br.com.acorde.dashboard.service;

import br.com.acorde.dashboard.dto.DashboardSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardSummary summary(UUID organizationId, LocalDate from, LocalDate to) {
        List<DashboardSummary.SourceSummary> sources = jdbc.query("""
                WITH categorias(categoria) AS (
                    VALUES ('ENERGIA_ELETRICA'), ('DESLOCAMENTO')
                ), agregados AS (
                    SELECT a.categoria,
                           COUNT(*) AS atividades,
                           COUNT(*) FILTER (WHERE a.estado = 'VALIDA') AS validas,
                           COUNT(*) FILTER (WHERE a.estado = 'PENDENTE') AS pendentes,
                           SUM(r.resultado_kg_co2e) FILTER (WHERE r.vigente = TRUE AND r.estado = 'VALIDA') AS emissao
                    FROM atividades_emissoras a
                    LEFT JOIN registros_emissao r ON r.atividade_id = a.id AND r.vigente = TRUE
                    WHERE a.organizacao_id = ?
                      AND (CAST(? AS DATE) IS NULL OR a.periodo_inicio >= ?)
                      AND (CAST(? AS DATE) IS NULL OR a.periodo_fim <= ?)
                    GROUP BY a.categoria
                )
                SELECT c.categoria, COALESCE(a.atividades, 0) AS atividades,
                       COALESCE(a.validas, 0) AS validas, COALESCE(a.pendentes, 0) AS pendentes, a.emissao
                FROM categorias c LEFT JOIN agregados a ON a.categoria = c.categoria
                ORDER BY c.categoria
                """, (rs, row) -> new DashboardSummary.SourceSummary(
                rs.getString("categoria"), rs.getLong("atividades"), rs.getLong("validas"),
                rs.getLong("pendentes"), rs.getBigDecimal("emissao")),
                organizationId, from, from, to, to);

        List<DashboardSummary.PeriodSummary> periods = jdbc.query("""
                SELECT a.periodo_inicio, a.periodo_fim,
                       COUNT(*) AS atividades,
                       COUNT(*) FILTER (WHERE a.estado = 'VALIDA') AS validas,
                       COUNT(*) FILTER (WHERE a.estado = 'PENDENTE') AS pendentes,
                       SUM(r.resultado_kg_co2e) FILTER (WHERE r.vigente = TRUE AND r.estado = 'VALIDA') AS emissao
                FROM atividades_emissoras a
                LEFT JOIN registros_emissao r ON r.atividade_id = a.id AND r.vigente = TRUE
                WHERE a.organizacao_id = ?
                  AND (CAST(? AS DATE) IS NULL OR a.periodo_inicio >= ?)
                  AND (CAST(? AS DATE) IS NULL OR a.periodo_fim <= ?)
                GROUP BY a.periodo_inicio, a.periodo_fim
                ORDER BY a.periodo_inicio, a.periodo_fim
                """, (rs, row) -> new DashboardSummary.PeriodSummary(
                rs.getDate("periodo_inicio").toLocalDate(), rs.getDate("periodo_fim").toLocalDate(),
                rs.getLong("atividades"), rs.getLong("validas"), rs.getLong("pendentes"),
                rs.getBigDecimal("emissao")), organizationId, from, from, to, to);

        long activityCount = 0;
        long pendingCount = 0;
        BigDecimal emissions = BigDecimal.ZERO;
        boolean hasValidCalculation = false;
        for (DashboardSummary.SourceSummary source : sources) {
            activityCount += source.activityCount();
            pendingCount += source.pendingCount();
            if (source.validCount() > 0 && source.emissionsKgCo2e() != null) {
                emissions = emissions.add(source.emissionsKgCo2e());
                hasValidCalculation = true;
            }
        }
        return new DashboardSummary(from, to, activityCount, pendingCount,
                hasValidCalculation ? emissions : null,
                List.copyOf(sources), List.copyOf(periods),
                "Total parcial das fontes monitoradas; registros pendentes não são tratados como emissão zero. "
                        + "A cobertura não representa inventário corporativo completo.");
    }
}

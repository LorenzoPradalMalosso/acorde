package br.com.acorde.carbono.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FactorRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public FactorRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> list(String category, String region, LocalDate date) {
        return jdbc.queryForList("""
                SELECT id, categoria, escopo, valor, unidade_atividade, unidade_resultado,
                       fonte, versao, valido_desde, valido_ate, regiao, premissas
                FROM fatores_emissao
                WHERE (:category IS NULL OR categoria = :category)
                  AND (regiao = :region OR regiao = '*')
                  AND valido_desde <= :date AND (valido_ate IS NULL OR valido_ate >= :date)
                ORDER BY categoria, regiao, valido_desde DESC, versao DESC
                """, Map.of("category", category == null ? "" : category,
                        "region", region, "date", date));
    }

    public List<Map<String, Object>> findCompatible(String category, String scope, String unit, String region,
                                                    LocalDate periodStart, LocalDate periodEnd) {
        var params = new MapSqlParameterSource().addValue("category", category).addValue("scope", scope).addValue("unit", unit)
                .addValue("region", region).addValue("periodStart", periodStart).addValue("periodEnd", periodEnd);
        return jdbc.queryForList("""
                SELECT id, valor, unidade_atividade, unidade_resultado, fonte, versao, escopo,
                       regiao, premissas, valido_desde, valido_ate
                FROM fatores_emissao
                WHERE categoria = :category AND escopo = :scope AND unidade_atividade = :unit
                  AND (regiao = :region OR regiao = '*')
                  AND valido_desde <= :periodStart AND (valido_ate IS NULL OR valido_ate >= :periodEnd)
                  AND unidade_resultado = 'kg CO2e'
                ORDER BY (regiao = :region) DESC, valido_desde DESC, id
                """, params);
    }
}

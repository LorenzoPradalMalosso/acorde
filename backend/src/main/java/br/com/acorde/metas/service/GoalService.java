package br.com.acorde.metas.service;

import br.com.acorde.metas.dto.CreateGoalRequest;
import br.com.acorde.metas.dto.GoalResponse;
import br.com.acorde.platform.errors.ApiException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
    private static final Set<String> STATES = Set.of("ATIVA", "CONCLUIDA", "CANCELADA");
    private final JdbcTemplate jdbc;

    public GoalService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public GoalResponse create(UUID organizationId, CreateGoalRequest request) {
        if (request.basePeriodEnd().isBefore(request.basePeriodStart())
                || !List.of("ENERGIA_ELETRICA", "DESLOCAMENTO").contains(request.category())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "Categoria ou período-base inválido.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO metas_carbono(id, organizacao_id, categoria, periodo_base_inicio, periodo_base_fim,
                    valor_base_kg_co2e, percentual_alvo, prazo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, organizationId, request.category(), request.basePeriodStart(), request.basePeriodEnd(),
                request.baseValueKgCo2e(), request.targetPercent(), request.dueDate());
        return get(id, organizationId);
    }

    public List<GoalResponse> list(UUID organizationId) {
        return jdbc.query("""
                SELECT id, categoria, periodo_base_inicio, periodo_base_fim, valor_base_kg_co2e,
                    percentual_alvo, prazo, estado
                FROM metas_carbono WHERE organizacao_id = ? ORDER BY prazo, criado_em
                """, (rs, row) -> new GoalResponse(rs.getObject("id", UUID.class), rs.getString("categoria"),
                rs.getDate("periodo_base_inicio").toLocalDate(), rs.getDate("periodo_base_fim").toLocalDate(),
                rs.getBigDecimal("valor_base_kg_co2e"), rs.getBigDecimal("percentual_alvo"),
                rs.getDate("prazo").toLocalDate(), rs.getString("estado")), organizationId);
    }

    @Transactional
    public GoalResponse updateState(UUID id, UUID organizationId, String state) {
        if (!STATES.contains(state)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "Estado de meta inválido.");
        }
        if (jdbc.update("UPDATE metas_carbono SET estado = ?, atualizado_em = CURRENT_TIMESTAMP WHERE organizacao_id = ? AND id = ?",
                state, organizationId, id) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Meta não encontrada.");
        }
        return get(id, organizationId);
    }

    private GoalResponse get(UUID id, UUID organizationId) {
        return jdbc.queryForObject("""
                SELECT id, categoria, periodo_base_inicio, periodo_base_fim, valor_base_kg_co2e,
                    percentual_alvo, prazo, estado
                FROM metas_carbono WHERE organizacao_id = ? AND id = ?
                """, (rs, row) -> new GoalResponse(rs.getObject("id", UUID.class), rs.getString("categoria"),
                rs.getDate("periodo_base_inicio").toLocalDate(), rs.getDate("periodo_base_fim").toLocalDate(),
                rs.getBigDecimal("valor_base_kg_co2e"), rs.getBigDecimal("percentual_alvo"),
                rs.getDate("prazo").toLocalDate(), rs.getString("estado")), organizationId, id);
    }
}

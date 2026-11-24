package br.com.acorde.acoes.service;

import br.com.acorde.acoes.dto.ActionResponse;
import br.com.acorde.acoes.dto.CreateActionRequest;
import br.com.acorde.platform.errors.ApiException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionService {
    private static final Set<String> STATES = Set.of("PLANEJADA", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA");
    private final JdbcTemplate jdbc;

    public ActionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public ActionResponse create(UUID organizationId, CreateActionRequest request) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM metas_carbono WHERE organizacao_id = ? AND id = ?",
                Integer.class, organizationId, request.goalId()) == 0) {
            throw notFound();
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM membros WHERE organizacao_id = ? AND id = ? AND ativo = TRUE",
                Integer.class, organizationId, request.responsibleMemberId()) == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_responsible_member", "Responsável não pertence a esta organização ou está inativo.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO acoes_reducao(id, organizacao_id, meta_id, responsavel_membro_id, titulo, descricao, prazo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, organizationId, request.goalId(), request.responsibleMemberId(), request.title().trim(),
                request.description(), request.dueDate());
        return find(id, organizationId);
    }

    public List<ActionResponse> list(UUID organizationId, UUID memberId, boolean manager) {
        String sql = """
                SELECT id, meta_id, responsavel_membro_id, titulo, descricao, prazo, estado
                FROM acoes_reducao WHERE organizacao_id = ? AND (? = TRUE OR responsavel_membro_id = ?)
                ORDER BY prazo, criado_em
                """;
        return jdbc.query(sql, (rs, row) -> new ActionResponse(rs.getObject("id", UUID.class),
                rs.getObject("meta_id", UUID.class), rs.getObject("responsavel_membro_id", UUID.class),
                rs.getString("titulo"), rs.getString("descricao"), rs.getDate("prazo").toLocalDate(),
                rs.getString("estado")), organizationId, manager, memberId);
    }

    @Transactional
    public ActionResponse updateState(UUID id, String state, UUID organizationId, UUID memberId, boolean manager) {
        if (!STATES.contains(state)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "Estado de ação inválido.");
        }
        ActionResponse current = find(id, organizationId);
        if (!manager && !current.responsibleMemberId().equals(memberId)) throw notFound();
        if (!manager && !allowedCollaboratorTransition(current.state(), state)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_transition", "Responsáveis podem avançar uma ação atribuída; cancelamento e reabertura cabem ao gestor.");
        }
        jdbc.update("UPDATE acoes_reducao SET estado = ?, atualizado_em = CURRENT_TIMESTAMP WHERE organizacao_id = ? AND id = ?",
                state, organizationId, id);
        return find(id, organizationId);
    }

    private static boolean allowedCollaboratorTransition(String current, String next) {
        return current.equals(next) || (current.equals("PLANEJADA") && next.equals("EM_ANDAMENTO"))
                || (current.equals("EM_ANDAMENTO") && next.equals("CONCLUIDA"));
    }

    private ActionResponse find(UUID id, UUID organizationId) {
        List<ActionResponse> rows = jdbc.query("""
                SELECT id, meta_id, responsavel_membro_id, titulo, descricao, prazo, estado
                FROM acoes_reducao WHERE organizacao_id = ? AND id = ?
                """, (rs, row) -> new ActionResponse(rs.getObject("id", UUID.class),
                rs.getObject("meta_id", UUID.class), rs.getObject("responsavel_membro_id", UUID.class),
                rs.getString("titulo"), rs.getString("descricao"), rs.getDate("prazo").toLocalDate(),
                rs.getString("estado")), organizationId, id);
        if (rows.isEmpty()) throw notFound();
        return rows.getFirst();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Ação não encontrada.");
    }
}

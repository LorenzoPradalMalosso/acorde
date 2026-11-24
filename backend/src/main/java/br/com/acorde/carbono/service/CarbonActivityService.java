package br.com.acorde.carbono.service;

import br.com.acorde.carbono.dto.ActivityRequest;
import br.com.acorde.carbono.dto.ActivityRevisionRequest;
import br.com.acorde.carbono.dto.ActivityResponse;
import br.com.acorde.carbono.dto.CalculationResponse;
import br.com.acorde.carbono.repository.FactorRepository;
import br.com.acorde.organizacoes.model.MemberRole;
import br.com.acorde.organizacoes.repository.OrganizationMemberRepository;
import br.com.acorde.platform.errors.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarbonActivityService {
    private final JdbcTemplate jdbc;
    private final FactorRepository factors;
    private final OrganizationMemberRepository members;

    public CarbonActivityService(JdbcTemplate jdbc, FactorRepository factors, OrganizationMemberRepository members) {
        this.jdbc = jdbc;
        this.factors = factors;
        this.members = members;
    }

    @Transactional
    public ActivityResponse registerEnergy(ActivityRequest request, Jwt jwt) {
        requireRole(jwt, MemberRole.GESTOR);
        validatePeriod(request);
        if (request.consumptionKwh() == null || request.consumptionKwh().signum() <= 0
                || request.mode() != null || request.roundTripDistanceKm() != null || request.commutingDays() != null) {
            throw invalid("Energia exige consumo positivo em kWh e não aceita campos de deslocamento");
        }
        return persist("ENERGIA_ELETRICA", request.consumptionKwh(), "kWh", request, jwt,
                activityId -> jdbc.update("INSERT INTO dados_energia(atividade_id, consumo_kwh, regiao_rede) VALUES (?, ?, ?)",
                        activityId, request.consumptionKwh(), request.region()));
    }

    @Transactional
    public ActivityResponse registerCommute(ActivityRequest request, Jwt jwt) {
        requireRole(jwt, MemberRole.COLABORADOR);
        validatePeriod(request);
        if (request.mode() == null || request.mode().isBlank() || request.roundTripDistanceKm() == null
                || request.commutingDays() == null || request.consumptionKwh() != null) {
            throw invalid("Deslocamento exige modalidade, distância de ida e volta e dias com viagem");
        }
        BigDecimal quantity = request.roundTripDistanceKm().multiply(request.commutingDays());
        return persist("DESLOCAMENTO", quantity, "passageiro-km", request, jwt,
                activityId -> jdbc.update("""
                        INSERT INTO dados_deslocamento(atividade_id, modalidade, regiao, distancia_ida_volta_km, dias_viagens, ocupacao)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, activityId, request.mode().trim().toUpperCase(java.util.Locale.ROOT),
                        request.region().trim(), request.roundTripDistanceKm(), request.commutingDays(), request.occupancy()));
    }

    private ActivityResponse persist(String category, BigDecimal quantity, String unit, ActivityRequest request,
                                     Jwt jwt, Consumer<UUID> saveDetails) {
        UUID organizationId = claimUuid(jwt, "organization_id");
        UUID memberId = claimUuid(jwt, "member_id");
        UUID existing = jdbc.query("""
                SELECT id FROM atividades_emissoras WHERE organizacao_id = ? AND membro_id = ? AND chave_idempotencia = ?
                """, rs -> rs.next() ? rs.getObject(1, UUID.class) : null, organizationId, memberId, request.idempotencyKey());
        if (existing != null) return find(existing, organizationId, jwt);

        UUID activityId = UUID.randomUUID();
        String state = "PENDENTE";
        String reason = "Nenhum fator de emissão aprovado e compatível foi cadastrado para esta fonte, região, unidade e período.";
        List<Map<String, Object>> compatibleFactors = factors.findCompatible(category, expectedScope(category), unit, request.region().trim(),
                request.periodStart(), request.periodEnd());
        Map<String, Object> factor = selectUnambiguousFactor(compatibleFactors, request.region().trim());
        state = factor == null ? "PENDENTE" : "VALIDA";
        if (factor != null) reason = null;
        int inserted = jdbc.update("""
                INSERT INTO atividades_emissoras(id, organizacao_id, membro_id, categoria, periodo_inicio, periodo_fim,
                    quantidade, unidade, estado, justificativa_pendencia, chave_idempotencia)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (organizacao_id, membro_id, chave_idempotencia) DO NOTHING
                """, activityId, organizationId, memberId, category, request.periodStart(), request.periodEnd(),
                quantity, unit, state, reason, request.idempotencyKey());
        if (inserted == 0) {
            UUID concurrentDuplicate = jdbc.query("""
                    SELECT id FROM atividades_emissoras
                    WHERE organizacao_id = ? AND membro_id = ? AND chave_idempotencia = ?
                    """, rs -> rs.next() ? rs.getObject(1, UUID.class) : null,
                    organizationId, memberId, request.idempotencyKey());
            return find(concurrentDuplicate, organizationId, jwt);
        }
        saveDetails.accept(activityId);
        insertCalculation(activityId, organizationId, 1, factor, unit, quantity, request.periodStart(),
                request.periodEnd(), request.region().trim(), state, reason);
        return find(activityId, organizationId, jwt);
    }

    private void insertCalculation(UUID activityId, UUID organizationId, int revision, Map<String, Object> factor,
                                   String unit, BigDecimal quantity, LocalDate periodStart, LocalDate periodEnd,
                                   String region, String state, String reason) {
        jdbc.update("""
                INSERT INTO registros_emissao(id, atividade_id, organizacao_id, revisao, fator_id,
                    fator_valor_aplicado, fator_fonte_aplicada, fator_versao_aplicada, fator_escopo_aplicado,
                    fator_regiao_aplicada, fator_premissas_aplicadas, fator_unidade_aplicada,
                    unidade_atividade_aplicada, quantidade_aplicada, periodo_inicio_aplicado, periodo_fim_aplicado,
                    regiao_aplicada, resultado_kg_co2e, unidade_resultado,
                    estado, vigente, justificativa)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), activityId, organizationId, revision,
                factor == null ? null : factor.get("id"),
                factor == null ? null : factor.get("valor"),
                factor == null ? null : factor.get("fonte"),
                factor == null ? null : factor.get("versao"),
                factor == null ? null : factor.get("escopo"),
                factor == null ? null : factor.get("regiao"),
                factor == null ? null : factor.get("premissas"),
                factor == null ? null : factor.get("unidade_atividade"),
                unit, quantity, periodStart, periodEnd, region,
                factor == null ? null : ((BigDecimal) factor.get("valor")).multiply(quantity).setScale(8, RoundingMode.HALF_UP),
                factor == null ? null : factor.get("unidade_resultado"),
                state, factor != null, reason);
    }

    private static Map<String, Object> selectUnambiguousFactor(List<Map<String, Object>> matches, String region) {
        List<Map<String, Object>> exact = matches.stream()
                .filter(factor -> region.equals(factor.get("regiao"))).toList();
        List<Map<String, Object>> candidates = exact.isEmpty()
                ? matches.stream().filter(factor -> "*".equals(factor.get("regiao"))).toList()
                : exact;
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private static String expectedScope(String category) {
        return switch (category) {
            case "ENERGIA_ELETRICA" -> "ESCOPO_2";
            case "DESLOCAMENTO" -> "ESCOPO_3";
            default -> "ESCOPO_NAO_DEFINIDO";
        };
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(Jwt jwt) {
        UUID organization = claimUuid(jwt, "organization_id");
        UUID member = claimUuid(jwt, "member_id");
        boolean manager = "GESTOR".equals(jwt.getClaimAsString("role"));
        return jdbc.query("""
                SELECT a.id, a.categoria, a.periodo_inicio, a.periodo_fim, a.quantidade, a.unidade,
                       a.estado, a.justificativa_pendencia, r.revisao, r.resultado_kg_co2e, r.vigente
                FROM atividades_emissoras a LEFT JOIN registros_emissao r ON r.atividade_id = a.id
                    AND r.revisao = (SELECT MAX(last_revision.revisao) FROM registros_emissao last_revision WHERE last_revision.atividade_id = a.id)
                WHERE a.organizacao_id = ? AND (? = TRUE OR a.membro_id = ?)
                ORDER BY a.periodo_inicio DESC, a.criado_em DESC
                """, mapper(), organization, manager, member);
    }

    @Transactional(readOnly = true)
    public ActivityResponse find(UUID activityId, Jwt jwt) {
        return find(activityId, claimUuid(jwt, "organization_id"), jwt);
    }

    @Transactional
    public ActivityResponse revise(UUID activityId, ActivityRevisionRequest request, Jwt jwt) {
        UUID organizationId = claimUuid(jwt, "organization_id");
        UUID memberId = claimUuid(jwt, "member_id");
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT categoria FROM atividades_emissoras
                WHERE id = ? AND organizacao_id = ? AND membro_id = ?
                """, activityId, organizationId, memberId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Activity not found");
        String category = (String) rows.getFirst().get("categoria");
        requireRole(jwt, "ENERGIA_ELETRICA".equals(category) ? MemberRole.GESTOR : MemberRole.COLABORADOR);
        if (request.periodEnd().isBefore(request.periodStart())) throw invalid("End date must not precede start date");

        BigDecimal quantity;
        String unit;
        if ("ENERGIA_ELETRICA".equals(category)) {
            if (request.consumptionKwh() == null || request.consumptionKwh().signum() <= 0
                    || request.mode() != null || request.roundTripDistanceKm() != null || request.commutingDays() != null) {
                throw invalid("Energy revision requires positive kWh and no commute fields");
            }
            quantity = request.consumptionKwh();
            unit = "kWh";
            jdbc.update("UPDATE dados_energia SET consumo_kwh = ?, regiao_rede = ? WHERE atividade_id = ?",
                    quantity, request.region().trim(), activityId);
        } else {
            if (request.mode() == null || request.mode().isBlank() || request.roundTripDistanceKm() == null
                    || request.commutingDays() == null || request.consumptionKwh() != null) {
                throw invalid("Commute revision requires mode, round-trip distance and commuting days");
            }
            quantity = request.roundTripDistanceKm().multiply(request.commutingDays());
            unit = "passageiro-km";
            jdbc.update("""
                    UPDATE dados_deslocamento
                    SET modalidade = ?, regiao = ?, distancia_ida_volta_km = ?, dias_viagens = ?, ocupacao = ?
                    WHERE atividade_id = ?
                    """, request.mode().trim().toUpperCase(java.util.Locale.ROOT), request.region().trim(),
                    request.roundTripDistanceKm(), request.commutingDays(), request.occupancy(), activityId);
        }
        jdbc.update("""
                UPDATE atividades_emissoras
                SET periodo_inicio = ?, periodo_fim = ?, quantidade = ?, unidade = ?
                WHERE id = ? AND organizacao_id = ? AND membro_id = ?
                """, request.periodStart(), request.periodEnd(), quantity, unit, activityId, organizationId, memberId);
        return reprocess(activityId, jwt);
    }

    @Transactional
    public ActivityResponse reprocess(UUID activityId, Jwt jwt) {
        UUID organizationId = claimUuid(jwt, "organization_id");
        UUID memberId = claimUuid(jwt, "member_id");
        List<Map<String, Object>> activities = jdbc.queryForList("""
                SELECT a.id, a.categoria, a.periodo_inicio, a.periodo_fim, a.quantidade, a.unidade,
                       COALESCE(e.regiao_rede, d.regiao) AS regiao
                FROM atividades_emissoras a
                LEFT JOIN dados_energia e ON e.atividade_id = a.id
                LEFT JOIN dados_deslocamento d ON d.atividade_id = a.id
                WHERE a.id = ? AND a.organizacao_id = ? AND a.membro_id = ?
                """, activityId, organizationId, memberId);
        if (activities.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Recurso nÃ£o encontrado");

        Map<String, Object> activity = activities.getFirst();
        String category = (String) activity.get("categoria");
        requireRole(jwt, "ENERGIA_ELETRICA".equals(category) ? MemberRole.GESTOR : MemberRole.COLABORADOR);
        String unit = (String) activity.get("unidade");
        String region = (String) activity.get("regiao");
        LocalDate periodStart = ((java.sql.Date) activity.get("periodo_inicio")).toLocalDate();
        LocalDate periodEnd = ((java.sql.Date) activity.get("periodo_fim")).toLocalDate();
        BigDecimal quantity = (BigDecimal) activity.get("quantidade");
        List<Map<String, Object>> compatibleFactors = factors.findCompatible(category, expectedScope(category), unit,
                region, periodStart, periodEnd);
        Map<String, Object> factor = selectUnambiguousFactor(compatibleFactors, region);
        String state = factor == null ? "PENDENTE" : "VALIDA";
        String reason = factor == null
                ? "NÃ£o hÃ¡ um fator Ãºnico aprovado, com unidade, regiÃ£o e validade compatÃ­veis para todo o perÃ­odo."
                : null;
        Integer nextRevision = jdbc.queryForObject(
                "SELECT COALESCE(MAX(revisao), 0) + 1 FROM registros_emissao WHERE atividade_id = ?", Integer.class, activityId);

        jdbc.update("UPDATE registros_emissao SET estado = 'SUBSTITUIDA', vigente = FALSE WHERE atividade_id = ? AND vigente = TRUE",
                activityId);
        insertCalculation(activityId, organizationId, nextRevision, factor, unit, quantity, periodStart,
                periodEnd, region, state, reason);
        jdbc.update("UPDATE atividades_emissoras SET estado = ?, justificativa_pendencia = ? WHERE id = ?",
                state, reason, activityId);
        return find(activityId, organizationId, jwt);
    }

    @Transactional(readOnly = true)
    public List<CalculationResponse> calculations(UUID activityId, Jwt jwt) {
        UUID organizationId = claimUuid(jwt, "organization_id");
        UUID memberId = claimUuid(jwt, "member_id");
        boolean manager = "GESTOR".equals(jwt.getClaimAsString("role"));
        if (find(activityId, organizationId, jwt) == null) throw new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Recurso nÃ£o encontrado");
        return jdbc.query("""
                SELECT r.revisao, r.fator_id, r.fator_valor_aplicado, r.fator_fonte_aplicada,
                       r.fator_versao_aplicada, r.fator_escopo_aplicado, r.fator_regiao_aplicada,
                       r.fator_premissas_aplicadas, r.fator_unidade_aplicada,
                       r.unidade_atividade_aplicada, r.quantidade_aplicada, r.periodo_inicio_aplicado,
                       r.periodo_fim_aplicado, r.regiao_aplicada, r.resultado_kg_co2e,
                       r.unidade_resultado, r.estado, r.vigente, r.justificativa, r.calculado_em
                FROM registros_emissao r JOIN atividades_emissoras a ON a.id = r.atividade_id
                WHERE r.atividade_id = ? AND a.organizacao_id = ? AND (? = TRUE OR a.membro_id = ?)
                ORDER BY r.revisao
                """, (rs, row) -> new CalculationResponse(
                rs.getInt("revisao"), rs.getObject("fator_id", UUID.class), rs.getBigDecimal("fator_valor_aplicado"),
                rs.getString("fator_fonte_aplicada"), rs.getString("fator_versao_aplicada"),
                rs.getString("fator_escopo_aplicado"), rs.getString("fator_regiao_aplicada"),
                rs.getString("fator_premissas_aplicadas"), rs.getString("fator_unidade_aplicada"),
                rs.getString("unidade_atividade_aplicada"), rs.getBigDecimal("quantidade_aplicada"),
                rs.getObject("periodo_inicio_aplicado", LocalDate.class), rs.getObject("periodo_fim_aplicado", LocalDate.class),
                rs.getString("regiao_aplicada"),
                rs.getBigDecimal("resultado_kg_co2e"), rs.getString("unidade_resultado"),
                rs.getString("estado"), rs.getBoolean("vigente"), rs.getString("justificativa"),
                rs.getObject("calculado_em", OffsetDateTime.class)), activityId, organizationId, manager, memberId);
    }

    private ActivityResponse find(UUID id, UUID organization, Jwt jwt) {
        boolean manager = "GESTOR".equals(jwt.getClaimAsString("role"));
        List<ActivityResponse> rows = jdbc.query("""
                SELECT a.id, a.categoria, a.periodo_inicio, a.periodo_fim, a.quantidade, a.unidade,
                       a.estado, a.justificativa_pendencia, r.revisao, r.resultado_kg_co2e, r.vigente
                FROM atividades_emissoras a LEFT JOIN registros_emissao r ON r.atividade_id = a.id
                    AND r.revisao = (SELECT MAX(last_revision.revisao) FROM registros_emissao last_revision WHERE last_revision.atividade_id = a.id)
                WHERE a.id = ? AND a.organizacao_id = ? AND (? = TRUE OR a.membro_id = ?)
                """, mapper(), id, organization, manager, claimUuid(jwt, "member_id"));
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", "Recurso não encontrado");
        return rows.getFirst();
    }

    private RowMapper<ActivityResponse> mapper() {
        return (rs, row) -> new ActivityResponse(rs.getObject("id", UUID.class), rs.getString("categoria"),
                rs.getObject("periodo_inicio", LocalDate.class), rs.getObject("periodo_fim", LocalDate.class),
                rs.getBigDecimal("quantidade"), rs.getString("unidade"), rs.getString("estado"),
                rs.getString("justificativa_pendencia"), (Integer) rs.getObject("revisao"),
                rs.getBigDecimal("resultado_kg_co2e"), rs.getBoolean("vigente"));
    }

    private void requireRole(Jwt jwt, MemberRole required) {
        MemberRole actual;
        try { actual = MemberRole.valueOf(jwt.getClaimAsString("role")); }
        catch (RuntimeException ex) { throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Perfil não permitido"); }
        if (actual != required) throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Perfil não permitido");
        UUID org = claimUuid(jwt, "organization_id");
        UUID member = claimUuid(jwt, "member_id");
        if (members.findByIdAndOrganization_IdAndActiveTrueAndUser_ActiveTrue(member, org).isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Vínculo inativo");
        }
    }

    private static UUID claimUuid(Jwt jwt, String name) { return UUID.fromString(jwt.getClaimAsString(name)); }
    private static void validatePeriod(ActivityRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) throw invalid("Período final deve ser igual ou posterior ao inicial");
    }
    private static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "validation_error", message); }
}

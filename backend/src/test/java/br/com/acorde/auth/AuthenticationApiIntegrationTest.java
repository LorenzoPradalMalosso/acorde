package br.com.acorde.auth;

import br.com.acorde.auth.model.AuthenticationSession;
import br.com.acorde.auth.repository.AuthenticationSessionRepository;
import br.com.acorde.organizacoes.model.MemberRole;
import br.com.acorde.organizacoes.model.Organization;
import br.com.acorde.organizacoes.model.OrganizationMember;
import br.com.acorde.organizacoes.model.UserAccount;
import br.com.acorde.organizacoes.repository.OrganizationMemberRepository;
import br.com.acorde.organizacoes.repository.OrganizationRepository;
import br.com.acorde.organizacoes.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationApiIntegrationTest {
    private static final String PASSWORD = "Acorde-Teste-2026!";
    private final UUID organizationAId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID organizationBId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID managerAId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID collaboratorAId = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private final UUID managerBId = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private final UUID collaboratorBId = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private final UUID userManagerAId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final UUID userCollaboratorAId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID userManagerBId = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private final UUID userCollaboratorBId = UUID.fromString("20000000-0000-0000-0000-000000000004");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired OrganizationRepository organizations;
    @Autowired UserAccountRepository users;
    @Autowired OrganizationMemberRepository members;
    @Autowired AuthenticationSessionRepository sessions;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seedIsolatedOrganizationsAndAccounts() {
        String databaseName = jdbc.queryForObject("SELECT current_database()", String.class);
        if (!"acorde_test".equals(databaseName)) {
            throw new IllegalStateException("Integration tests require the isolated PostgreSQL database named acorde_test");
        }
        jdbc.update("DELETE FROM acoes_reducao");
        jdbc.update("DELETE FROM metas_carbono");
        jdbc.update("DELETE FROM registros_emissao");
        jdbc.update("DELETE FROM dados_energia");
        jdbc.update("DELETE FROM dados_deslocamento");
        jdbc.update("DELETE FROM atividades_emissoras");
        jdbc.update("DELETE FROM fatores_emissao");
        sessions.deleteAll();
        members.deleteAll();
        users.deleteAll();
        organizations.deleteAll();
        Organization orgA = organizations.save(new Organization(organizationAId, "Organização de teste A"));
        Organization orgB = organizations.save(new Organization(organizationBId, "Organização de teste B"));
        String passwordHash = passwordEncoder.encode(PASSWORD);
        UserAccount managerA = users.save(new UserAccount(userManagerAId, "Gestora A", "gestora-a@example.test", passwordHash, true));
        UserAccount collaboratorA = users.save(new UserAccount(userCollaboratorAId, "Colaborador A", "colaborador-a@example.test", passwordHash, true));
        UserAccount managerB = users.save(new UserAccount(userManagerBId, "Gestor B", "gestor-b@example.test", passwordHash, true));
        UserAccount collaboratorB = users.save(new UserAccount(userCollaboratorBId, "Colaborador B", "colaborador-b@example.test", passwordHash, true));
        members.save(new OrganizationMember(managerAId, orgA, managerA, MemberRole.GESTOR, true));
        members.save(new OrganizationMember(collaboratorAId, orgA, collaboratorA, MemberRole.COLABORADOR, true));
        members.save(new OrganizationMember(managerBId, orgB, managerB, MemberRole.GESTOR, true));
        members.save(new OrganizationMember(collaboratorBId, orgB, collaboratorB, MemberRole.COLABORADOR, true));
    }

    @Test
    void validLoginAndAuthenticatedProfile() throws Exception {
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gestora-a@example.test"))
                .andExpect(jsonPath("$.role").value("GESTOR"))
                .andExpect(jsonPath("$.organizationId").value(organizationAId.toString()));
    }

    @Test
    void invalidCredentialsAreGenericAndDoNotAuthenticate() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("gestora-a@example.test", "wrong-password", organizationAId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("gestora-a@example.test", PASSWORD, organizationBId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void unauthenticatedInvalidAndExpiredTokensAreRejected() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        String expiredToken = expiredToken();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void collaboratorCannotListOrChangeOrganizationRoles() throws Exception {
        String token = login("colaborador-a@example.test", PASSWORD, organizationAId);
        mvc.perform(get(membersPath(organizationAId)).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(patch(membersPath(organizationAId) + "/" + collaboratorAId + "/role")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"GESTOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCannotReadOrChangeMembersInAnotherOrganization() throws Exception {
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        mvc.perform(get(membersPath(organizationBId)).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mvc.perform(patch(membersPath(organizationBId) + "/" + collaboratorBId + "/role")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"GESTOR\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(membersPath(organizationAId)).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'gestor-b@example.test')]").doesNotExist());
    }

    @Test
    void logoutRevokesSessionImmediately() throws Exception {
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeRevokesExistingTokensAndCannotRemoveLastManager() throws Exception {
        String managerToken = login("gestora-a@example.test", PASSWORD, organizationAId);
        String collaboratorToken = login("colaborador-a@example.test", PASSWORD, organizationAId);
        mvc.perform(patch(membersPath(organizationAId) + "/" + collaboratorAId + "/role")
                        .header("Authorization", bearer(managerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"GESTOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("GESTOR"));
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(collaboratorToken)))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch(membersPath(organizationAId) + "/" + managerAId + "/role")
                        .header("Authorization", bearer(managerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"COLABORADOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("self_role_change_not_allowed"));
    }

    @Test
    void loginRequiresExplicitOrganizationWhenAccountHasSeveralMemberships() throws Exception {
        Organization orgB = organizations.findById(organizationBId).orElseThrow();
        UserAccount managerA = users.findById(userManagerAId).orElseThrow();
        UUID secondMembership = UUID.fromString("10000000-0000-0000-0000-000000000005");
        members.save(new OrganizationMember(secondMembership, orgB, managerA, MemberRole.COLABORADOR, true));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "gestora-a@example.test", "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("organization_selection_required"))
                .andExpect(jsonPath("$.memberships.length()").value(2));
    }

    @Test
    void managerRegistersEnergyPendingWithoutApprovedFactorAndIdempotencyPreventsDuplicates() throws Exception {
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        UUID idempotencyKey = UUID.randomUUID();
        String request = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", idempotencyKey,
                "periodStart", "2026-01-01", "periodEnd", "2026-01-31",
                "region", "BR-SP", "consumptionKwh", "125.500000"));
        String first = mvc.perform(post("/api/carbon/activities/energy")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("ENERGIA_ELETRICA"))
                .andExpect(jsonPath("$.quantity").value(125.5))
                .andExpect(jsonPath("$.unit").value("kWh"))
                .andExpect(jsonPath("$.state").value("PENDENTE"))
                .andExpect(jsonPath("$.emissionsKgCo2e").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/carbon/activities/energy")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(objectMapper.readTree(first).get("id").asText()));
        mvc.perform(get("/api/carbon/activities").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void collaboratorRegistersCommuteAsPassengerKilometersWithoutInventedEmission() throws Exception {
        String token = login("colaborador-a@example.test", PASSWORD, organizationAId);
        String request = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID(), "periodStart", "2026-02-01", "periodEnd", "2026-02-28",
                "region", "BR-SP", "mode", "ONIBUS", "roundTripDistanceKm", "18.250000", "commutingDays", "20"));
        String first = mvc.perform(post("/api/carbon/activities/commutes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(365))
                .andExpect(jsonPath("$.unit").value("passageiro-km"))
                .andExpect(jsonPath("$.state").value("PENDENTE"))
                .andExpect(jsonPath("$.emissionsKgCo2e").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/carbon/activities/commutes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(objectMapper.readTree(first).get("id").asText()));
        mvc.perform(get("/api/carbon/activities").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void compatibleSyntheticCommuteFactorUsesPassengerKilometersAndRecordedRegion() throws Exception {
        insertSyntheticFactor("DESLOCAMENTO", "passageiro-km", "BR-SP", "0.0010000000",
                "2026-01-01", "2026-12-31", "synthetic-commute-v1");
        String token = login("colaborador-a@example.test", PASSWORD, organizationAId);
        String request = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID(), "periodStart", "2026-02-01", "periodEnd", "2026-02-28",
                "region", "BR-SP", "mode", "ONIBUS", "roundTripDistanceKm", "18.25", "commutingDays", "20"));

        String response = mvc.perform(post("/api/carbon/activities/commutes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(365))
                .andExpect(jsonPath("$.unit").value("passageiro-km"))
                .andExpect(jsonPath("$.state").value("VALIDA"))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(0.365))
                .andReturn().getResponse().getContentAsString();
        UUID activityId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        org.junit.jupiter.api.Assertions.assertEquals("BR-SP", jdbc.queryForObject(
                "SELECT regiao FROM dados_deslocamento WHERE atividade_id = ?", String.class, activityId));
    }

    @Test
    void negativeEnergyQuantityIsRejected() throws Exception {
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-01-01", "2026-01-31", "-1", "BR-SP")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void collaboratorCannotRegisterOrganizationEnergy() throws Exception {
        String token = login("colaborador-a@example.test", PASSWORD, organizationAId);
        String request = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID(), "periodStart", "2026-01-01", "periodEnd", "2026-01-31",
                "region", "BR-SP", "consumptionKwh", "50"));
        mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void compatibleSyntheticFactorCalculatesAndSnapshotsDecimalInputs() throws Exception {
        UUID factorId = insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.1234567890",
                "2026-01-01", "2026-12-31", "synthetic-energy-v1");
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        String request = energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "125.500000", "BR-SP");

        String response = mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("VALIDA"))
                .andExpect(jsonPath("$.current").value(true))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(15.49382702))
                .andReturn().getResponse().getContentAsString();
        UUID activityId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        Map<String, Object> snapshot = jdbc.queryForMap("""
                SELECT fator_id, fator_valor_aplicado, fator_fonte_aplicada, fator_versao_aplicada,
                       unidade_atividade_aplicada, quantidade_aplicada, periodo_inicio_aplicado,
                       periodo_fim_aplicado, regiao_aplicada, resultado_kg_co2e
                FROM registros_emissao WHERE atividade_id = ? AND vigente = TRUE
                """, activityId);
        org.junit.jupiter.api.Assertions.assertEquals(factorId, snapshot.get("fator_id"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("0.1234567890"), snapshot.get("fator_valor_aplicado"));
        org.junit.jupiter.api.Assertions.assertEquals("synthetic-energy-v1", snapshot.get("fator_versao_aplicada"));
        org.junit.jupiter.api.Assertions.assertEquals(java.sql.Date.valueOf("2026-02-01"), snapshot.get("periodo_inicio_aplicado"));
        org.junit.jupiter.api.Assertions.assertEquals("BR-SP", snapshot.get("regiao_aplicada"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("15.49382702"), snapshot.get("resultado_kg_co2e"));
    }

    @Test
    void factorMustCoverTheEntireActivityPeriodAndMatchTheActivityUnit() throws Exception {
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.25",
                "2026-01-01", "2026-02-14", "synthetic-partial-validity");
        insertSyntheticFactor("ENERGIA_ELETRICA", "MWh", "BR-SP", "0.25",
                "2026-01-01", "2026-12-31", "synthetic-wrong-unit");
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.25",
                "2026-01-01", "2026-12-31", "synthetic-wrong-scope", "ESCOPO_3");
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);

        mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "100", "BR-SP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("PENDENTE"))
                .andExpect(jsonPath("$.emissionsKgCo2e").doesNotExist());
    }

    @Test
    void ambiguousFactorsRemainPendingAndDoNotCreateAnEmissionEstimate() throws Exception {
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.10",
                "2026-01-01", "2026-12-31", "synthetic-overlap-v1");
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.11",
                "2026-01-01", "2026-12-31", "synthetic-overlap-v2");
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);

        mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "100", "BR-SP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("PENDENTE"))
                .andExpect(jsonPath("$.emissionsKgCo2e").doesNotExist());
    }

    @Test
    void factorVersionsDoNotRewriteExistingCalculationSnapshots() throws Exception {
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.10",
                "2026-01-01", "2026-12-31", "synthetic-snapshot-v1");
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        String response = mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "100", "BR-SP")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID activityId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.20",
                "2026-01-01", "2026-12-31", "synthetic-snapshot-v2");

        Map<String, Object> snapshot = jdbc.queryForMap("""
                SELECT fator_versao_aplicada, fator_valor_aplicado, resultado_kg_co2e
                FROM registros_emissao WHERE atividade_id = ? AND vigente = TRUE
                """, activityId);
        org.junit.jupiter.api.Assertions.assertEquals("synthetic-snapshot-v1", snapshot.get("fator_versao_aplicada"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("0.1000000000"), snapshot.get("fator_valor_aplicado"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("10.00000000"), snapshot.get("resultado_kg_co2e"));
    }

    @Test
    void explicitReprocessingKeepsHistoryAndOnlyOneCurrentValidRevision() throws Exception {
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "*", "0.10",
                "2026-01-01", "2026-12-31", "synthetic-global-v1");
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        String response = mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "100", "BR-SP")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.emissionsKgCo2e").value(10.0))
                .andReturn().getResponse().getContentAsString();
        String activityId = objectMapper.readTree(response).get("id").asText();
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.20",
                "2026-01-01", "2026-12-31", "synthetic-region-v2");

        mvc.perform(post("/api/carbon/activities/" + activityId + "/reprocess")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(2))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(20.0))
                .andExpect(jsonPath("$.current").value(true));
        mvc.perform(get("/api/carbon/activities/" + activityId + "/calculations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].factorVersion").value("synthetic-global-v1"))
                .andExpect(jsonPath("$[0].state").value("SUBSTITUIDA"))
                .andExpect(jsonPath("$[0].current").value(false))
                .andExpect(jsonPath("$[1].factorVersion").value("synthetic-region-v2"))
                .andExpect(jsonPath("$[1].current").value(true));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbc.queryForObject("SELECT COUNT(*) FROM registros_emissao WHERE atividade_id = ? AND vigente = TRUE",
                        Integer.class, UUID.fromString(activityId)));
    }

    @Test
    void activityRevisionPreservesThePriorCalculationAndUsesUpdatedQuantity() throws Exception {
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.10",
                "2026-01-01", "2026-12-31", "synthetic-activity-revision");
        String token = login("gestora-a@example.test", PASSWORD, organizationAId);
        String response = mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "100", "BR-SP")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.emissionsKgCo2e").value(10.0))
                .andReturn().getResponse().getContentAsString();
        String activityId = objectMapper.readTree(response).get("id").asText();
        String revision = objectMapper.writeValueAsString(Map.of(
                "periodStart", "2026-03-01", "periodEnd", "2026-03-31", "region", "BR-SP", "consumptionKwh", "200"));

        mvc.perform(put("/api/carbon/activities/" + activityId).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(revision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(200))
                .andExpect(jsonPath("$.revision").value(2))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(20.0));
        mvc.perform(get("/api/carbon/activities/" + activityId + "/calculations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].activityQuantity").value(100))
                .andExpect(jsonPath("$[0].periodStart").value("2026-02-01"))
                .andExpect(jsonPath("$[0].emissionsKgCo2e").value(10))
                .andExpect(jsonPath("$[0].state").value("SUBSTITUIDA"))
                .andExpect(jsonPath("$[1].activityQuantity").value(200))
                .andExpect(jsonPath("$[1].periodStart").value("2026-03-01"))
                .andExpect(jsonPath("$[1].current").value(true));
    }

    @Test
    void activityHistoryIsIsolatedBetweenOrganizations() throws Exception {
        String managerA = login("gestora-a@example.test", PASSWORD, organizationAId);
        String managerB = login("gestor-b@example.test", PASSWORD, organizationBId);
        String response = mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-01-01", "2026-01-31", "50", "BR-SP")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String activityId = objectMapper.readTree(response).get("id").asText();

        mvc.perform(get("/api/carbon/activities").header("Authorization", bearer(managerB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/carbon/activities/" + activityId).header("Authorization", bearer(managerB)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/carbon/activities/" + activityId + "/reprocess")
                        .header("Authorization", bearer(managerB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardAggregatesOnlyCurrentValidCalculationsAndIsOrganizationScoped() throws Exception {
        String managerA = login("gestora-a@example.test", PASSWORD, organizationAId);
        String managerB = login("gestor-b@example.test", PASSWORD, organizationBId);
        String collaboratorA = login("colaborador-a@example.test", PASSWORD, organizationAId);
        mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-01-01", "2026-01-31", "50", "BR-SP")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("PENDENTE"));
        insertSyntheticFactor("ENERGIA_ELETRICA", "kWh", "BR-SP", "0.1", "2026-02-01", "2026-02-28", "DASH-TEST");
        mvc.perform(post("/api/carbon/activities/energy").header("Authorization", bearer(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyRequest(UUID.randomUUID(), "2026-02-01", "2026-02-28", "100", "BR-SP")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("VALIDA"));

        mvc.perform(get("/api/dashboard").header("Authorization", bearer(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityCount").value(2))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(10))
                .andExpect(jsonPath("$.sources[1].validCount").value(1))
                .andExpect(jsonPath("$.periods.length()").value(2));
        mvc.perform(get("/api/dashboard?from=2026-02-01&to=2026-02-28")
                        .header("Authorization", bearer(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityCount").value(1))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(10));
        mvc.perform(get("/api/dashboard").header("Authorization", bearer(managerB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityCount").value(0))
                .andExpect(jsonPath("$.emissionsKgCo2e").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/dashboard").header("Authorization", bearer(collaboratorA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard?from=2026-02-28&to=2026-02-01")
                        .header("Authorization", bearer(managerA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void goalsAndAssignedActionsRespectOrganizationAndRoleBoundaries() throws Exception {
        String managerA = login("gestora-a@example.test", PASSWORD, organizationAId);
        String managerB = login("gestor-b@example.test", PASSWORD, organizationBId);
        String collaboratorA = login("colaborador-a@example.test", PASSWORD, organizationAId);
        String collaboratorB = login("colaborador-b@example.test", PASSWORD, organizationBId);
        String goalBody = objectMapper.writeValueAsString(Map.of(
                "category", "ENERGIA_ELETRICA", "basePeriodStart", "2025-01-01", "basePeriodEnd", "2025-12-31",
                "baseValueKgCo2e", "0", "targetPercent", "15", "dueDate", "2027-12-31"));
        String goal = mvc.perform(post("/api/goals").header("Authorization", bearer(managerA))
                        .contentType(MediaType.APPLICATION_JSON).content(goalBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("ATIVA"))
                .andExpect(jsonPath("$.baseValueKgCo2e").value(0))
                .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(goal).get("id").asText();
        mvc.perform(patch("/api/goals/" + goalId + "/state").header("Authorization", bearer(collaboratorA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CONCLUIDA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/goals/" + goalId + "/state").header("Authorization", bearer(managerB))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CONCLUIDA\"}"))
                .andExpect(status().isNotFound());
        String actionBody = objectMapper.writeValueAsString(Map.of(
                "goalId", goalId, "responsibleMemberId", collaboratorAId, "title", "Revisar iluminação",
                "description", "Ação de demonstração operacional", "dueDate", "2027-06-30"));
        String action = mvc.perform(post("/api/actions").header("Authorization", bearer(managerA))
                        .contentType(MediaType.APPLICATION_JSON).content(actionBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("PLANEJADA"))
                .andReturn().getResponse().getContentAsString();
        String actionId = objectMapper.readTree(action).get("id").asText();

        mvc.perform(post("/api/goals").header("Authorization", bearer(collaboratorA))
                        .contentType(MediaType.APPLICATION_JSON).content(goalBody))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/actions").header("Authorization", bearer(collaboratorA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/actions").header("Authorization", bearer(collaboratorB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(patch("/api/actions/" + actionId + "/state").header("Authorization", bearer(collaboratorB))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CONCLUIDA\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/actions/" + actionId + "/state").header("Authorization", bearer(collaboratorA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CONCLUIDA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/actions/" + actionId + "/state").header("Authorization", bearer(collaboratorA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"EM_ANDAMENTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("EM_ANDAMENTO"));
        mvc.perform(patch("/api/actions/" + actionId + "/state").header("Authorization", bearer(collaboratorA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CANCELADA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/actions/" + actionId + "/state").header("Authorization", bearer(collaboratorA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CONCLUIDA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CONCLUIDA"));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("SELECT COUNT(*) FROM registros_emissao", Integer.class));
        mvc.perform(get("/api/goals").header("Authorization", bearer(managerB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(patch("/api/goals/" + goalId + "/state").header("Authorization", bearer(managerA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CONCLUIDA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CONCLUIDA"));
    }

    private UUID insertSyntheticFactor(String category, String unit, String region, String value,
                                      String validFrom, String validTo, String version) {
        String scope = category.equals("ENERGIA_ELETRICA") ? "ESCOPO_2" : "ESCOPO_3";
        return insertSyntheticFactor(category, unit, region, value, validFrom, validTo, version, scope);
    }

    private UUID insertSyntheticFactor(String category, String unit, String region, String value,
                                       String validFrom, String validTo, String version, String scope) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO fatores_emissao(id, categoria, escopo, valor, unidade_atividade, unidade_resultado,
                    fonte, versao, valido_desde, valido_ate, regiao, premissas)
                VALUES (?, ?, ?, ?, ?, 'kg CO2e', 'Fixture sintética somente para teste automatizado',
                    ?, ?, ?, ?, 'Premissas artificiais; não representam fator ambiental real.')
                """, id, category, scope, new java.math.BigDecimal(value), unit, version,
                java.time.LocalDate.parse(validFrom), java.time.LocalDate.parse(validTo), region);
        return id;
    }

    private String energyRequest(UUID idempotencyKey, String start, String end, String quantity, String region) throws Exception {
        return objectMapper.writeValueAsString(Map.of("idempotencyKey", idempotencyKey,
                "periodStart", start, "periodEnd", end, "region", region, "consumptionKwh", quantity));
    }

    private String login(String email, String password, UUID organizationId) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password, organizationId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("accessToken").asText();
    }

    private String expiredToken() {
        Instant now = Instant.now();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("acorde-api-test").subject(userManagerAId.toString())
                        .issuedAt(now.minusSeconds(120)).expiresAt(now.minusSeconds(60))
                        .id(UUID.randomUUID().toString()).claim("organization_id", organizationAId.toString())
                        .claim("member_id", managerAId.toString()).claim("role", "GESTOR").build())).getTokenValue();
    }

    private String loginJson(String email, String password, UUID organizationId) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password, "organizationId", organizationId));
    }

    private String membersPath(UUID organizationId) {
        return "/api/organizations/" + organizationId + "/members";
    }

    private String bearer(String token) { return "Bearer " + token; }
}

package br.com.acorde.organizacoes.service;

import br.com.acorde.organizacoes.model.MemberRole;
import br.com.acorde.organizacoes.model.Organization;
import br.com.acorde.organizacoes.model.OrganizationMember;
import br.com.acorde.organizacoes.model.UserAccount;
import br.com.acorde.organizacoes.repository.OrganizationMemberRepository;
import br.com.acorde.organizacoes.repository.OrganizationRepository;
import br.com.acorde.organizacoes.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local-demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final OrganizationRepository organizations;
    private final UserAccountRepository users;
    private final OrganizationMemberRepository members;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataInitializer(OrganizationRepository organizations,
                               UserAccountRepository users,
                               OrganizationMemberRepository members,
                               JdbcTemplate jdbc,
                               PasswordEncoder passwordEncoder,
                               @Value("${acorde.demo.password:}") String demoPassword) {
        this.organizations = organizations;
        this.users = users;
        this.members = members;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        byte[] passwordBytes = demoPassword.getBytes(StandardCharsets.UTF_8);
        if (passwordBytes.length < 12 || passwordBytes.length > 72) {
            throw new IllegalStateException("ACORDE_DEMO_PASSWORD must be between 12 and 72 UTF-8 bytes for local-demo");
        }
        long organizationCount = organizations.count();
        long userCount = users.count();
        long memberCount = members.count();
        if (organizationCount + userCount + memberCount > 0) return;

        seedSyntheticFactorsIfCatalogEmpty();
        Organization orgA = organizations.save(new Organization(UUID.randomUUID(), "Organização de demonstração A"));
        Organization orgB = organizations.save(new Organization(UUID.randomUUID(), "Organização de demonstração B"));
        String hash = passwordEncoder.encode(demoPassword);
        UserAccount managerA = newUser("Gestor A", "gestor-a@acorde.local", hash);
        UserAccount collaboratorA = newUser("Colaborador A", "colaborador-a@acorde.local", hash);
        UserAccount managerB = newUser("Gestor B", "gestor-b@acorde.local", hash);
        UserAccount collaboratorB = newUser("Colaborador B", "colaborador-b@acorde.local", hash);
        users.saveAll(java.util.List.of(managerA, collaboratorA, managerB, collaboratorB));
        members.saveAll(java.util.List.of(
                new OrganizationMember(UUID.randomUUID(), orgA, managerA, MemberRole.GESTOR, true),
                new OrganizationMember(UUID.randomUUID(), orgA, collaboratorA, MemberRole.COLABORADOR, true),
                new OrganizationMember(UUID.randomUUID(), orgB, managerB, MemberRole.GESTOR, true),
                new OrganizationMember(UUID.randomUUID(), orgB, collaboratorB, MemberRole.COLABORADOR, true)));
    }

    private void seedSyntheticFactorsIfCatalogEmpty() {
        Long factorCount = jdbc.queryForObject("SELECT COUNT(*) FROM fatores_emissao", Long.class);
        if (factorCount == null || factorCount > 0) return;

        String source = "SINTÉTICO — demonstração local; não representa fator ambiental real";
        String assumptions = "Valor fictício exclusivo do perfil local-demo. Não usar em análises, relatórios ou produção.";
        LocalDate validFrom = LocalDate.of(2020, 1, 1);
        LocalDate validTo = LocalDate.of(2030, 12, 31);
        jdbc.update("""
                INSERT INTO fatores_emissao(id, categoria, escopo, valor, unidade_atividade, unidade_resultado,
                    fonte, versao, valido_desde, valido_ate, regiao, premissas)
                VALUES (?, 'ENERGIA_ELETRICA', 'ESCOPO_2', 0.1000000000, 'kWh', 'kg CO2e', ?,
                    'TESTE-LOCAL-1', ?, ?, 'BR-SP', ?)
                """, UUID.fromString("f0000000-0000-0000-0000-000000000001"), source, validFrom, validTo, assumptions);
        jdbc.update("""
                INSERT INTO fatores_emissao(id, categoria, escopo, valor, unidade_atividade, unidade_resultado,
                    fonte, versao, valido_desde, valido_ate, regiao, premissas)
                VALUES (?, 'DESLOCAMENTO', 'ESCOPO_3', 0.0500000000, 'passageiro-km', 'kg CO2e', ?,
                    'TESTE-LOCAL-1', ?, ?, 'BR-SP', ?)
                """, UUID.fromString("f0000000-0000-0000-0000-000000000002"), source, validFrom, validTo, assumptions);
    }

    private UserAccount newUser(String name, String email, String passwordHash) {
        return new UserAccount(UUID.randomUUID(), name, email, passwordHash, true);
    }
}

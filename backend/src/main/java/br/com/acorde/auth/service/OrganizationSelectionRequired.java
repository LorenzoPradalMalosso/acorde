package br.com.acorde.auth.service;

import br.com.acorde.auth.dto.MembershipOption;
import br.com.acorde.platform.errors.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;

public class OrganizationSelectionRequired extends ApiException {
    private final List<MembershipOption> memberships;

    public OrganizationSelectionRequired(List<MembershipOption> memberships) {
        super(HttpStatus.CONFLICT, "organization_selection_required", "Selecione uma organização para continuar");
        this.memberships = memberships;
    }

    public List<MembershipOption> getMemberships() { return memberships; }
}

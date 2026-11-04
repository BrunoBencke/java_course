package cerne.members;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/** Member registration: ifs in sequence, the first failure stops it. */
@Stateless
public class RegistrationService {

    @Inject
    private MemberRepository members;

    public RegistrationService() {
    }

    public RegistrationService(MemberRepository members) {
        this.members = members;
    }

    public Member register(String cpf, String name, MemberType type) {
        if (cpf == null || cpf.isBlank()) {
            throw new InvalidRegistrationException("CPF is required");
        }
        if (!Cpf.isValid(cpf)) {
            throw new InvalidRegistrationException("Invalid CPF: " + cpf);
        }
        if (name == null || name.isBlank()) {
            throw new InvalidRegistrationException("Name is required");
        }
        if (name.strip().length() < 3) {
            throw new InvalidRegistrationException("Name too short");
        }
        if (type == null) {
            throw new InvalidRegistrationException("Member type is required");
        }
        if (members.findByCpf(cpf).isPresent()) {
            throw new InvalidRegistrationException("CPF already registered: " + cpf);
        }
        var member = new Member(cpf, name.strip(), type);
        members.save(member);
        return member;
    }
}

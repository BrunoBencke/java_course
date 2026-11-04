package cerne.members;

import javax.ejb.Stateless;
import javax.inject.Inject;

import cerne.util.CerneUtil;

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
        if (cpf == null || cpf.trim().isEmpty()) {
            throw new InvalidRegistrationException("CPF is required");
        }
        if (!CerneUtil.isValidCpf(cpf)) {
            throw new InvalidRegistrationException("Invalid CPF: " + cpf);
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidRegistrationException("Name is required");
        }
        if (name.trim().length() < 3) {
            throw new InvalidRegistrationException("Name too short");
        }
        if (type == null) {
            throw new InvalidRegistrationException("Member type is required");
        }
        if (members.findByCpf(cpf) != null) {
            throw new InvalidRegistrationException("CPF already registered: " + cpf);
        }
        Member member = new Member(cpf, name.trim(), type);
        members.save(member);
        return member;
    }
}

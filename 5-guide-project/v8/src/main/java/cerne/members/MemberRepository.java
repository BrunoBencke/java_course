package cerne.members;

import java.util.List;

public interface MemberRepository {

    /** Returns the member, or null when the CPF does not exist. */
    Member findByCpf(String cpf);

    List<Member> listAll();

    void save(Member member);
}

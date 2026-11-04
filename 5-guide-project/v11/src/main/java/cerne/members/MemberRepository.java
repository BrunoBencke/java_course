package cerne.members;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    /** Absence is in the signature: the caller decides what to do. */
    Optional<Member> findByCpf(String cpf);

    List<Member> listAll();

    void save(Member member);
}

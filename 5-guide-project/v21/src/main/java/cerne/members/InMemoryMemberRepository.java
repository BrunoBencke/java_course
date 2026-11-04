package cerne.members;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repository for tests and measurements; no database. */
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> byCpf = new LinkedHashMap<>();

    @Override
    public Optional<Member> findByCpf(String cpf) {
        return Optional.ofNullable(byCpf.get(cpf));
    }

    @Override
    public List<Member> listAll() {
        return List.copyOf(byCpf.values());
    }

    @Override
    public void save(Member member) {
        byCpf.put(member.getCpf(), member);
    }
}

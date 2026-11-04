package cerne.members;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repository for tests and measurements; no database. */
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> byCpf = new LinkedHashMap<String, Member>();

    @Override
    public Member findByCpf(String cpf) {
        return byCpf.get(cpf);
    }

    @Override
    public List<Member> listAll() {
        return new ArrayList<Member>(byCpf.values());
    }

    @Override
    public void save(Member member) {
        byCpf.put(member.getCpf(), member);
    }
}

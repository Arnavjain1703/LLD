package library.repository;

import library.model.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single map keyed by email — no secondary index needed
 * because email IS the unique identity.
 */
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> memberStore = new ConcurrentHashMap<>();

    @Override
    public Member save(Member member) {
        memberStore.put(member.getEmail(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(String email) {
        return Optional.ofNullable(memberStore.get(email));
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(memberStore.values());
    }

    @Override
    public void delete(String email) {
        memberStore.remove(email);
    }
}

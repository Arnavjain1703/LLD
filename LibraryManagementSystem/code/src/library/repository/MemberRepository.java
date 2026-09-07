package library.repository;

import library.model.Member;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(String email);
    List<Member> findAll();
    void delete(String email);
}

package library.repository;

import library.model.Fine;
import java.util.List;
import java.util.Optional;

/** Stores only PAID fines — acts as a payment ledger. */
public interface FineRepository {
    Fine save(Fine fine);
    Optional<Fine> findById(String fineId);
    List<Fine> findByMember(String memberEmail);   // full payment history
}

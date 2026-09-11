package library.repository;

import library.model.Fine;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryFineRepository implements FineRepository {

    private final Map<String, Fine> fineStore = new ConcurrentHashMap<>();

    @Override
    public Fine save(Fine fine) {
        fineStore.put(fine.getFineId(), fine);
        return fine;
    }

    @Override
    public Optional<Fine> findById(String fineId) {
        return Optional.ofNullable(fineStore.get(fineId));
    }

    @Override
    public List<Fine> findByMember(String memberEmail) {
        return fineStore.values().stream()
                .filter(f -> f.getMember().getEmail().equals(memberEmail))
                .collect(Collectors.toList());
    }
}

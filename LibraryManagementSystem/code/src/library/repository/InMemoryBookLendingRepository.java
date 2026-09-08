package library.repository;

import library.model.BookLending;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryBookLendingRepository implements BookLendingRepository {

    private final Map<String, BookLending> lendingStore = new ConcurrentHashMap<>();

    @Override
    public BookLending save(BookLending lending) {
        lendingStore.put(lending.getLendingId(), lending);
        return lending;
    }

    @Override
    public Optional<BookLending> findById(String lendingId) {
        return Optional.ofNullable(lendingStore.get(lendingId));
    }

    @Override
    public List<BookLending> findActiveByMember(String memberEmail) {
        return lendingStore.values().stream()
                .filter(l -> l.getMember().getEmail().equals(memberEmail) && !l.isReturned())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BookLending> findActiveByBarcode(String barcode) {
        return lendingStore.values().stream()
                .filter(l -> l.getBookItem().getBarcode().equals(barcode) && !l.isReturned())
                .findFirst();
    }

    @Override
    public List<BookLending> findAllByMember(String memberEmail) {
        return lendingStore.values().stream()
                .filter(l -> l.getMember().getEmail().equals(memberEmail))
                .collect(Collectors.toList());
    }
}

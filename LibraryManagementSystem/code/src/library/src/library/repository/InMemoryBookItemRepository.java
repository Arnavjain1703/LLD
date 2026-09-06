package library.repository;

import library.exception.BookNotFoundException;
import library.model.BookItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryBookItemRepository implements BookItemRepository {

    // barcode → BookItem — O(1) lookup by barcode
    private final Map<String, BookItem> bookItemStore = new ConcurrentHashMap<>();

    // isbn → List of barcodes — O(1) lookup of all copies for a title
    // single source of truth for copy ownership
    private final Map<String, List<String>> isbnToCopies = new ConcurrentHashMap<>();

    @Override
    public synchronized BookItem save(String isbn, BookItem bookItem) {
        // synchronized — compound op: write to both bookItemStore + isbnToCopies
        bookItemStore.put(bookItem.getBarcode(), bookItem);
        isbnToCopies.computeIfAbsent(isbn, k -> new ArrayList<>())
                    .add(bookItem.getBarcode());
        return bookItem;
    }

    @Override
    public Optional<BookItem> findByBarcode(String barcode) {
        return Optional.ofNullable(bookItemStore.get(barcode));
    }

    @Override
    public synchronized void delete(String barcode) {
        // synchronized — compound op: remove from bookItemStore + isbnToCopies
        BookItem item = bookItemStore.remove(barcode);
        if (item == null) {
            throw new BookNotFoundException("BookItem not found with barcode: " + barcode);
        }
        String isbn = item.getBook().getIsbn();
        isbnToCopies.getOrDefault(isbn, Collections.emptyList()).remove(barcode);
    }

    @Override
    public synchronized void deleteAllCopies(String isbn) {
        // called when parent Book is deleted — cascade delete all its copies
        List<String> barcodes = isbnToCopies.remove(isbn);
        if (barcodes != null) {
            barcodes.forEach(bookItemStore::remove);
        }
    }

    @Override
    public Optional<BookItem> findAvailableCopy(String isbn) {
        // O(1) map lookup + O(k) scan — k = number of copies, typically small
        return isbnToCopies.getOrDefault(isbn, Collections.emptyList())
                           .stream()
                           .map(bookItemStore::get)
                           .filter(Objects::nonNull)
                           .filter(BookItem::isAvailable)
                           .findFirst();
    }

    @Override
    public boolean hasAvailableCopy(String isbn) {
        return findAvailableCopy(isbn).isPresent();
    }

    @Override
    public List<BookItem> findAllCopies(String isbn) {
        return isbnToCopies.getOrDefault(isbn, Collections.emptyList())
                           .stream()
                           .map(bookItemStore::get)
                           .filter(Objects::nonNull)
                           .collect(Collectors.toList());
    }
}

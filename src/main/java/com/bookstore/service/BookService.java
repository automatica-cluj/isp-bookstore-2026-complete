package com.bookstore.service;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.model.Author;
import com.bookstore.model.Book;
import com.bookstore.repository.AuthorRepository;
import com.bookstore.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));
        return toResponse(book);
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A book with ISBN " + request.isbn() + " already exists");
        }
        Author author = findAuthor(request.authorId());
        Set<String> tags = request.tags() != null ? request.tags() : new HashSet<>();
        Book book = new Book(request.title(), author, request.isbn(), request.price(), tags);
        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));

        // Only check for duplicate ISBN if the ISBN is actually being changed
        if (!existing.getIsbn().equals(request.isbn()) && bookRepository.existsByIsbn(request.isbn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A book with ISBN " + request.isbn() + " already exists");
        }

        Author author = findAuthor(request.authorId());
        existing.setTitle(request.title());
        existing.setAuthor(author);
        existing.setIsbn(request.isbn());
        existing.setPrice(request.price());
        existing.setTags(request.tags());

        Book saved = bookRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    // ── Collections Demo: Set ──────────────────────────────────────────

    /**
     * Returns all unique tags across every book in the catalogue.
     *
     * Uses a HashSet to collect tags — duplicates are automatically removed.
     * We loop through all books and addAll() their tags into one Set.
     */
    public Set<String> getAllTags() {
        List<Book> allBooks = bookRepository.findAll();

        Set<String> allTags = new HashSet<>();
        for (Book book : allBooks) {
            allTags.addAll(book.getTags());
        }
        return allTags;
    }

    // ── Collections Demo: Map<String, List<>> with groupingBy ──────────

    /**
     * Returns all books grouped by author name.
     *
     * Uses Collectors.groupingBy() to build a Map where:
     *   - key = author full name (String)
     *   - value = list of that author's books (List<BookResponse>)
     */
    public Map<String, List<BookResponse>> getBooksByAuthor() {
        List<Book> allBooks = bookRepository.findAll();

        // Group books by author name using Collectors.groupingBy()
        return allBooks.stream()
                .collect(Collectors.groupingBy(
                        book -> book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName(),
                        Collectors.mapping(this::toResponse, Collectors.toList())
                ));
    }

    // ── Private helpers ────────────────────────────────────────────────

    private Author findAuthor(Long authorId) {
        return authorRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: " + authorId));
    }

    private BookResponse toResponse(Book book) {
        Author author = book.getAuthor();
        String authorName = author.getFirstName() + " " + author.getLastName();
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                author.getId(),
                authorName,
                book.getIsbn(),
                book.getPrice(),
                book.getTags()
        );
    }
}

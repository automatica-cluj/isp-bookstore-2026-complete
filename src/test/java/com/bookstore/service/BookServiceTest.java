package com.bookstore.service;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.model.Author;
import com.bookstore.model.Book;
import com.bookstore.repository.AuthorRepository;
import com.bookstore.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    private Author createAuthor(Long id, String firstName, String lastName) {
        Author author = new Author(firstName, lastName);
        author.setId(id);
        return author;
    }

    private Book createBook(Long id, String title, Author author, String isbn, BigDecimal price) {
        Book book = new Book(title, author, isbn, price);
        book.setId(id);
        return book;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns mapped responses")
        void returnsMappedResponses() {
            Author author = createAuthor(1L, "Joshua", "Bloch");
            when(bookRepository.findAll()).thenReturn(List.of(
                    createBook(1L, "Effective Java", author, "9780134685991", new BigDecimal("45.00")),
                    createBook(2L, "Java Puzzlers", author, "9780321336781", new BigDecimal("35.00"))
            ));

            List<BookResponse> result = bookService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).title()).isEqualTo("Effective Java");
            assertThat(result.get(0).authorName()).isEqualTo("Joshua Bloch");
            assertThat(result.get(1).title()).isEqualTo("Java Puzzlers");
        }

        @Test
        @DisplayName("returns empty list when no books exist")
        void returnsEmptyList() {
            when(bookRepository.findAll()).thenReturn(List.of());

            List<BookResponse> result = bookService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns response for existing book")
        void returnsResponseForExistingBook() {
            Author author = createAuthor(1L, "Robert", "Martin");
            Book book = createBook(1L, "Clean Code", author, "9780132350884", new BigDecimal("39.99"));
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            BookResponse result = bookService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Clean Code");
            assertThat(result.authorId()).isEqualTo(1L);
            assertThat(result.authorName()).isEqualTo("Robert Martin");
            assertThat(result.isbn()).isEqualTo("9780132350884");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("39.99"));
        }

        @Test
        @DisplayName("throws ResponseStatusException for non-existing book")
        void throwsNotFoundForNonExistingBook() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.findById(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves and returns response")
        void savesAndReturnsResponse() {
            BookRequest request = new BookRequest("Clean Code", 1L, "9780132350884", new BigDecimal("39.99"), null);
            Author author = createAuthor(1L, "Robert", "Martin");
            Book saved = createBook(1L, "Clean Code", author, "9780132350884", new BigDecimal("39.99"));

            when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
            when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
            when(bookRepository.save(any(Book.class))).thenReturn(saved);

            BookResponse result = bookService.create(request);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Clean Code");
            assertThat(result.isbn()).isEqualTo("9780132350884");
        }

        @Test
        @DisplayName("throws ResponseStatusException for duplicate ISBN")
        void throwsConflictForDuplicateIsbn() {
            BookRequest request = new BookRequest("Clean Code", 1L, "9780132350884", new BigDecimal("39.99"), null);
            when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

            assertThatThrownBy(() -> bookService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("9780132350884");
        }

        @Test
        @DisplayName("throws ResponseStatusException for invalid author ID")
        void throwsNotFoundForInvalidAuthorId() {
            BookRequest request = new BookRequest("Clean Code", 999L, "9780132350884", new BigDecimal("39.99"), null);
            when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
            when(authorRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates all fields including author")
        void updatesAllFieldsIncludingAuthor() {
            Author oldAuthor = createAuthor(1L, "Robert", "Martin");
            Author newAuthor = createAuthor(2L, "Joshua", "Bloch");
            Book existing = createBook(1L, "Clean Code", oldAuthor, "9780132350884", new BigDecimal("39.99"));
            BookRequest request = new BookRequest("Effective Java", 2L, "9780134685991", new BigDecimal("45.00"), null);
            Book updated = createBook(1L, "Effective Java", newAuthor, "9780134685991", new BigDecimal("45.00"));

            when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(bookRepository.existsByIsbn("9780134685991")).thenReturn(false);
            when(authorRepository.findById(2L)).thenReturn(Optional.of(newAuthor));
            when(bookRepository.save(any(Book.class))).thenReturn(updated);

            BookResponse result = bookService.update(1L, request);

            assertThat(result.title()).isEqualTo("Effective Java");
            assertThat(result.authorId()).isEqualTo(2L);
            assertThat(result.isbn()).isEqualTo("9780134685991");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("45.00"));
        }

        @Test
        @DisplayName("throws ResponseStatusException for invalid book ID")
        void throwsNotFoundForInvalidBookId() {
            BookRequest request = new BookRequest("Clean Code", 1L, "9780132350884", new BigDecimal("39.99"), null);
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.update(999L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("throws ResponseStatusException when ISBN already exists")
        void throwsConflictWhenIsbnAlreadyExists() {
            Author author = createAuthor(1L, "Robert", "Martin");
            Book existing = createBook(1L, "Clean Code", author, "9780132350884", new BigDecimal("39.99"));
            BookRequest request = new BookRequest("Clean Code", 1L, "9780134685991", new BigDecimal("39.99"), null);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(bookRepository.existsByIsbn("9780134685991")).thenReturn(true);

            assertThatThrownBy(() -> bookService.update(1L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("9780134685991");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes existing book")
        void deletesExistingBook() {
            when(bookRepository.existsById(1L)).thenReturn(true);

            bookService.delete(1L);

            verify(bookRepository).deleteById(1L);
        }

        @Test
        @DisplayName("throws ResponseStatusException for non-existing book")
        void throwsNotFoundForNonExistingBook() {
            when(bookRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> bookService.delete(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("999");
        }
    }
}

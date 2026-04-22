package com.bookstore.repository;

import com.bookstore.model.Author;
import com.bookstore.model.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("BookRepository")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Author persistAuthor(String firstName, String lastName) {
        Author author = new Author(firstName, lastName);
        return entityManager.persistAndFlush(author);
    }

    @Nested
    @DisplayName("existsByIsbn")
    class ExistsByIsbn {

        @Test
        @DisplayName("returns true for existing ISBN")
        void returnsTrueForExistingIsbn() {
            // ISBN 9780132350884 is seeded by data.sql
            boolean exists = bookRepository.existsByIsbn("9780132350884");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("returns false for non-existing ISBN")
        void returnsFalseForNonExistingIsbn() {
            boolean exists = bookRepository.existsByIsbn("0000000000");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists book with author and generates ID")
        void persistsBookWithAuthorAndGeneratesId() {
            Author author = persistAuthor("Kent", "Beck");

            Book book = new Book("Test-Driven Development", author, "9780321146533", new BigDecimal("45.00"));
            Book saved = bookRepository.save(book);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTitle()).isEqualTo("Test-Driven Development");
            assertThat(saved.getAuthor().getId()).isEqualTo(author.getId());
            assertThat(saved.getIsbn()).isEqualTo("9780321146533");
            assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("45.00"));
        }

        @Test
        @DisplayName("unique ISBN constraint prevents duplicates")
        void uniqueIsbnConstraintPreventsDuplicates() {
            Author author = persistAuthor("Eric", "Evans");
            entityManager.persistAndFlush(new Book("DDD", author, "9780321125217", new BigDecimal("49.99")));

            Book duplicate = new Book("Another Book", author, "9780321125217", new BigDecimal("29.99"));

            assertThatThrownBy(() -> {
                bookRepository.saveAndFlush(duplicate);
            }).isInstanceOf(Exception.class);
        }
    }
}

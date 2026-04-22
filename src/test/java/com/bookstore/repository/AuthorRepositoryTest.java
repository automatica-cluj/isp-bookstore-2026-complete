package com.bookstore.repository;

import com.bookstore.model.Author;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("AuthorRepository")
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists author and generates ID")
        void persistsAuthorAndGeneratesId() {
            Author author = new Author("Joshua", "Bloch");

            Author saved = authorRepository.save(author);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getFirstName()).isEqualTo("Joshua");
            assertThat(saved.getLastName()).isEqualTo("Bloch");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns saved author")
        void returnsSavedAuthor() {
            Author author = authorRepository.save(new Author("Martin", "Fowler"));

            Optional<Author> found = authorRepository.findById(author.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Martin");
            assertThat(found.get().getLastName()).isEqualTo("Fowler");
        }

        @Test
        @DisplayName("returns empty for non-existing ID")
        void returnsEmptyForNonExistingId() {
            Optional<Author> found = authorRepository.findById(999L);

            assertThat(found).isEmpty();
        }
    }
}

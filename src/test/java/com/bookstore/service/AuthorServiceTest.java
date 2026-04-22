package com.bookstore.service;

import com.bookstore.dto.AuthorRequest;
import com.bookstore.dto.AuthorResponse;
import com.bookstore.model.Author;
import com.bookstore.repository.AuthorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorService")
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author createAuthor(Long id, String firstName, String lastName) {
        Author author = new Author(firstName, lastName);
        author.setId(id);
        return author;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns mapped responses")
        void returnsMappedResponses() {
            when(authorRepository.findAll()).thenReturn(List.of(
                    createAuthor(1L, "Joshua", "Bloch"),
                    createAuthor(2L, "Robert", "Martin")
            ));

            List<AuthorResponse> result = authorService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).firstName()).isEqualTo("Joshua");
            assertThat(result.get(1).lastName()).isEqualTo("Martin");
        }

        @Test
        @DisplayName("returns empty list when no authors exist")
        void returnsEmptyList() {
            when(authorRepository.findAll()).thenReturn(List.of());

            List<AuthorResponse> result = authorService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns response for existing author")
        void returnsResponseForExistingAuthor() {
            Author author = createAuthor(1L, "Martin", "Fowler");
            when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

            AuthorResponse result = authorService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.firstName()).isEqualTo("Martin");
            assertThat(result.lastName()).isEqualTo("Fowler");
        }

        @Test
        @DisplayName("throws ResponseStatusException for non-existing author")
        void throwsNotFoundForNonExistingAuthor() {
            when(authorRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authorService.findById(999L))
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
            AuthorRequest request = new AuthorRequest("Kent", "Beck");
            Author saved = createAuthor(1L, "Kent", "Beck");
            when(authorRepository.save(any(Author.class))).thenReturn(saved);

            AuthorResponse result = authorService.create(request);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.firstName()).isEqualTo("Kent");
            assertThat(result.lastName()).isEqualTo("Beck");
        }
    }
}

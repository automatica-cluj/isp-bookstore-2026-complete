package com.bookstore.controller;

import com.bookstore.dto.BookResponse;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtAuthFilter;
import com.bookstore.security.JwtUtil;
import com.bookstore.security.SecurityConfig;
import com.bookstore.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BookController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("BookController")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final String VALID_BOOK_JSON = """
            {
                "title": "Clean Code",
                "authorId": 1,
                "isbn": "9780132350884",
                "price": 39.99
            }
            """;

    @Nested
    @DisplayName("GET /api/books")
    class GetAllBooks {

        @Test
        @DisplayName("returns 200 with JSON array (no auth required)")
        void returns200WithJsonArray() throws Exception {
            when(bookService.findAll()).thenReturn(List.of(
                    new BookResponse(1L, "Clean Code", 1L, "Robert Martin", "9780132350884", new BigDecimal("39.99"), Set.of("programming")),
                    new BookResponse(2L, "Effective Java", 2L, "Joshua Bloch", "9780134685991", new BigDecimal("45.00"), Set.of("java"))
            ));

            mockMvc.perform(get("/api/books"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].title").value("Clean Code"))
                    .andExpect(jsonPath("$[1].title").value("Effective Java"));
        }
    }

    @Nested
    @DisplayName("GET /api/books/{id}")
    class GetBookById {

        @Test
        @DisplayName("returns 200 with JSON object (no auth required)")
        void returns200WithJsonObject() throws Exception {
            when(bookService.findById(1L)).thenReturn(
                    new BookResponse(1L, "Clean Code", 1L, "Robert Martin", "9780132350884", new BigDecimal("39.99"), Set.of("programming"))
            );

            mockMvc.perform(get("/api/books/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Clean Code"))
                    .andExpect(jsonPath("$.authorName").value("Robert Martin"));
        }

        @Test
        @DisplayName("returns 404 when book not found")
        void returns404WhenBookNotFound() throws Exception {
            when(bookService.findById(999L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: 999"));

            mockMvc.perform(get("/api/books/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/books")
    class CreateBook {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 201 with valid body when admin")
        void returns201WithValidBody() throws Exception {
            when(bookService.create(any())).thenReturn(
                    new BookResponse(1L, "Clean Code", 1L, "Robert Martin", "9780132350884", new BigDecimal("39.99"), Set.of("programming"))
            );

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BOOK_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Clean Code"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("returns 401 when not authenticated")
        void returns401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BOOK_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 400 with blank title")
        void returns400WithBlankTitle() throws Exception {
            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "", "authorId": 1, "isbn": "9780132350884", "price": 39.99}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 400 with null authorId")
        void returns400WithNullAuthorId() throws Exception {
            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Clean Code", "isbn": "9780132350884", "price": 39.99}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 400 with negative price")
        void returns400WithNegativePrice() throws Exception {
            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Clean Code", "authorId": 1, "isbn": "9780132350884", "price": -5.00}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 409 for duplicate ISBN")
        void returns409ForDuplicateIsbn() throws Exception {
            when(bookService.create(any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "A book with ISBN 9780132350884 already exists"));

            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BOOK_JSON))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/books/{id}")
    class UpdateBook {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 200 with valid body when admin")
        void returns200WithValidBody() throws Exception {
            when(bookService.update(eq(1L), any())).thenReturn(
                    new BookResponse(1L, "Clean Code", 1L, "Robert Martin", "9780132350884", new BigDecimal("39.99"), Set.of("programming"))
            );

            mockMvc.perform(put("/api/books/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BOOK_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("returns 401 when not authenticated")
        void returns401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(put("/api/books/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BOOK_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 404 when book not found")
        void returns404WhenBookNotFound() throws Exception {
            when(bookService.update(eq(999L), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: 999"));

            mockMvc.perform(put("/api/books/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BOOK_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/books/{id}")
    class DeleteBook {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 204 when admin deletes existing book")
        void returns204ForExistingBook() throws Exception {
            doNothing().when(bookService).delete(1L);

            mockMvc.perform(delete("/api/books/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("returns 403 when non-admin attempts delete")
        void returns403ForNonAdmin() throws Exception {
            mockMvc.perform(delete("/api/books/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("returns 401 when not authenticated")
        void returns401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete("/api/books/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 404 when book not found")
        void returns404WhenBookNotFound() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: 999"))
                    .when(bookService).delete(999L);

            mockMvc.perform(delete("/api/books/999"))
                    .andExpect(status().isNotFound());
        }
    }
}

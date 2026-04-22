package com.bookstore.controller;

import com.bookstore.dto.AuthorResponse;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.AuthorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorController.class)
@DisplayName("AuthorController")
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Nested
    @DisplayName("GET /api/authors")
    class GetAllAuthors {

        @Test
        @DisplayName("returns 200 with JSON array (no auth required)")
        void returns200WithJsonArray() throws Exception {
            when(authorService.findAll()).thenReturn(List.of(
                    new AuthorResponse(1L, "Joshua", "Bloch"),
                    new AuthorResponse(2L, "Robert", "Martin")
            ));

            mockMvc.perform(get("/api/authors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].firstName").value("Joshua"))
                    .andExpect(jsonPath("$[1].lastName").value("Martin"));
        }
    }

    @Nested
    @DisplayName("GET /api/authors/{id}")
    class GetAuthorById {

        @Test
        @DisplayName("returns 200 with JSON object (no auth required)")
        void returns200WithJsonObject() throws Exception {
            when(authorService.findById(1L)).thenReturn(
                    new AuthorResponse(1L, "Martin", "Fowler")
            );

            mockMvc.perform(get("/api/authors/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Martin"))
                    .andExpect(jsonPath("$.lastName").value("Fowler"));
        }

        @Test
        @DisplayName("returns 404 when author not found")
        void returns404WhenAuthorNotFound() throws Exception {
            when(authorService.findById(999L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found with id: 999"));

            mockMvc.perform(get("/api/authors/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/authors")
    class CreateAuthor {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 201 with valid body when admin")
        void returns201WithValidBody() throws Exception {
            when(authorService.create(any())).thenReturn(
                    new AuthorResponse(1L, "Kent", "Beck")
            );

            mockMvc.perform(post("/api/authors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Kent", "lastName": "Beck"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Kent"));
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void returns401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/authors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Kent", "lastName": "Beck"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 400 with blank firstName")
        void returns400WithBlankFirstName() throws Exception {
            mockMvc.perform(post("/api/authors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "", "lastName": "Beck"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 400 with blank lastName")
        void returns400WithBlankLastName() throws Exception {
            mockMvc.perform(post("/api/authors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Kent", "lastName": ""}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}

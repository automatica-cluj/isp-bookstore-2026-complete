package com.bookstore.controller;

import com.bookstore.repository.UserRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = StatisticsController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("StatisticsController")
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("GET /api/statistics")
    class GetStatistics {

        @Test
        @WithMockUser
        @DisplayName("returns 200 with bookCount and tagCount for an authenticated user")
        void returnsBookAndTagCount() throws Exception {
            when(bookService.countAll()).thenReturn(3);
            when(bookService.getAllTags()).thenReturn(Set.of("java", "spring", "testing"));

            mockMvc.perform(get("/api/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookCount").value(3))
                    .andExpect(jsonPath("$.tagCount").value(3));
        }

        @Test
        @DisplayName("returns 401 for an unauthenticated request")
        void returns401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/statistics"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

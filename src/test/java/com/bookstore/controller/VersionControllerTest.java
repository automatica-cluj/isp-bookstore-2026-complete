package com.bookstore.controller;

import com.bookstore.repository.UserRepository;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtAuthFilter;
import com.bookstore.security.JwtUtil;
import com.bookstore.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = VersionController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        properties = "app.version=9.9.9")
@Import({SecurityConfig.class, JwtAuthFilter.class})
@DisplayName("VersionController")
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("GET /api/version")
    class GetVersion {

        @Test
        @DisplayName("returns 200 with the build version for an unauthenticated request")
        void returnsVersionPublicly() throws Exception {
            mockMvc.perform(get("/api/version"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value("9.9.9"));
        }
    }
}

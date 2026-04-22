package com.bookstore.controller;

import com.bookstore.model.Role;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 with token for valid registration")
        void returns201WithToken() throws Exception {
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    "alice", "encoded-password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(customUserDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
            when(jwtUtil.generateToken(any(UserDetails.class), anyString())).thenReturn("test-jwt-token");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "alice", "password": "password123"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("test-jwt-token"));
        }

        @Test
        @DisplayName("returns 409 for duplicate username")
        void returns409ForDuplicateUsername() throws Exception {
            when(userRepository.existsByUsername("alice")).thenReturn(true);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "alice", "password": "password123"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 with blank username")
        void returns400WithBlankUsername() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "", "password": "password123"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 with short password")
        void returns400WithShortPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "alice", "password": "12345"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with token for valid credentials")
        void returns200WithToken() throws Exception {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken("alice", "password123"));
            User user = new User("alice", "encoded-password", Role.USER);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    "alice", "encoded-password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(customUserDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
            when(jwtUtil.generateToken(any(UserDetails.class), anyString())).thenReturn("test-jwt-token");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "alice", "password": "password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("test-jwt-token"));
        }

        @Test
        @DisplayName("returns 401 for invalid credentials")
        void returns401ForInvalidCredentials() throws Exception {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "alice", "password": "wrongpassword"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }
}

package com.bookstore.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "dGhpc0lzQVNlY3JldEtleUZvckpXVFNpZ25pbmdUaGF0SXNMb25nRW5vdWdo";
    private static final long EXPIRATION_MS = 3600000;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    private UserDetails createUser(String username, String role) {
        return new User(username, "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("generates a non-null token")
        void generatesNonNullToken() {
            UserDetails user = createUser("alice", "USER");
            String token = jwtUtil.generateToken(user, "ROLE_USER");
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("generates different tokens for different users")
        void generatesDifferentTokens() {
            String token1 = jwtUtil.generateToken(createUser("alice", "USER"), "ROLE_USER");
            String token2 = jwtUtil.generateToken(createUser("bob", "USER"), "ROLE_USER");
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("extracts the correct username from token")
        void extractsCorrectUsername() {
            UserDetails user = createUser("alice", "USER");
            String token = jwtUtil.generateToken(user, "ROLE_USER");
            assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("returns true for valid token and matching user")
        void returnsTrueForValidToken() {
            UserDetails user = createUser("alice", "USER");
            String token = jwtUtil.generateToken(user, "ROLE_USER");
            assertThat(jwtUtil.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("returns false when username does not match")
        void returnsFalseForWrongUser() {
            UserDetails alice = createUser("alice", "USER");
            UserDetails bob = createUser("bob", "USER");
            String token = jwtUtil.generateToken(alice, "ROLE_USER");
            assertThat(jwtUtil.isTokenValid(token, bob)).isFalse();
        }

        @Test
        @DisplayName("throws for expired token")
        void throwsForExpiredToken() {
            JwtUtil shortLivedUtil = new JwtUtil(SECRET, 0);
            UserDetails user = createUser("alice", "USER");
            String token = shortLivedUtil.generateToken(user, "ROLE_USER");
            assertThatThrownBy(() -> jwtUtil.isTokenValid(token, user))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("throws for tampered token")
        void throwsForTamperedToken() {
            UserDetails user = createUser("alice", "USER");
            String token = jwtUtil.generateToken(user, "ROLE_USER") + "tampered";
            assertThatThrownBy(() -> jwtUtil.isTokenValid(token, user))
                    .isInstanceOf(Exception.class);
        }
    }
}

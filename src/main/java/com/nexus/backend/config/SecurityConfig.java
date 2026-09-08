package com.nexus.backend.config;

import com.nexus.backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    try {
                        res.setContentType("application/json");
                        res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                        res.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: " + e.getMessage() + "\"}");
                    } catch (Exception ignored) {}
                })
                .accessDeniedHandler((req, res, e) -> {
                    try {
                        res.setContentType("application/json");
                        res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                        res.getWriter().write("{\"success\":false,\"message\":\"Access Denied: " + e.getMessage() + "\"}");
                    } catch (Exception ignored) {}
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health", "/api/db-test", "/api/debug-all").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // Enquiries
                .requestMatchers("/api/enquiries", "/api/enquiries/**").permitAll()

                // Materials & Recordings
                .requestMatchers("/api/materials", "/api/materials/**").permitAll()
                .requestMatchers("/api/recordings", "/api/recordings/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/student/recordings", "/api/student/materials").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/student/activity-status").permitAll()

                // Courses
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/courses/*/covered-topics", "/api/courses/**/covered-topics", "/api/courses/by-title/covered-topics").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/courses/*/meet-link", "/api/courses/**/meet-link").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/courses", "/api/courses/**").hasAnyRole("ADMIN", "TEACHER")

                // Batches
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/batches", "/api/batches/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/batches/*/covered-topics", "/api/batches/**/covered-topics").permitAll()
                .requestMatchers("/api/batches", "/api/batches/**").hasAnyRole("ADMIN", "TEACHER")

                // Teachers
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/teachers/all", "/api/teachers/{id}").permitAll()
                .requestMatchers("/api/teachers/profile", "/api/teachers/my-batches", "/api/teachers/my-courses-batches").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("TEACHER", "ADMIN")

                // Admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Enrollments
                .requestMatchers("/api/enrollments/count/course").permitAll()
                .requestMatchers("/api/enrollments/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                // Student & Notifications & Users
                .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "ADMIN", "TEACHER")
                .requestMatchers("/api/notifications/**").hasAnyRole("TEACHER", "ADMIN", "STUDENT")
                .requestMatchers("/api/users/**").authenticated()

                // Tests
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tests", "/api/tests/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tests/submit").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tests", "/api/tests/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/tests/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/tests/submissions/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/tests/**").authenticated()
                .requestMatchers("/api/test-attempts/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

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
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/batches").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/batches/**").authenticated()
                .requestMatchers("/api/batches/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/teachers/profile").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/teachers/my-batches").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/teachers/my-courses-batches").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/teachers/all").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/teachers/{id}").authenticated()
                .requestMatchers("/api/teachers/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/enquiries/**").permitAll()
                .requestMatchers("/api/materials/**").permitAll()
                .requestMatchers("/api/recordings/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/courses/*/meet-link").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/courses", "/api/courses/**").hasRole("ADMIN")
                .requestMatchers("/api/enrollments/count/course").permitAll()
                .requestMatchers("/api/student/materials", "/api/student/recordings").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers("/api/enrollments/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/notifications/**").hasAnyRole("TEACHER", "ADMIN", "STUDENT")
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/tests/**").authenticated()
                .requestMatchers("/api/test-attempts/**").authenticated()

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

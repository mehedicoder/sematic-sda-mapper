package com.sda.mapper.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Security configuration with form-login and role-based access skeleton.
 * Disabled in the test profile to allow unauthenticated API testing.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers(
                        request -> request.getRequestURI().startsWith("/api/"),
                    request -> request.getRequestURI().startsWith("/VAADIN/"),
                    request -> request.getRequestURI().startsWith("/HEARTBEAT/"),
                    request -> "uidl".equals(request.getParameter("v-r")),
                    request -> "push".equals(request.getParameter("v-r"))
                )
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .permitAll()
            )
                .httpBasic(Customizer.withDefaults())
            .logout(logout -> logout
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var user = User.builder()
            .username("user")
            .password("{noop}user")
            .roles("USER")
            .build();
        var admin = User.builder()
            .username("admin")
            .password("{noop}admin")
            .roles("USER", "ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user, admin);
    }
}

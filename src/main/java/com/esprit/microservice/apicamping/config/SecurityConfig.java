package com.esprit.microservice.apicamping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/site-camping/getAll").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/site-camping/getsite/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/site-camping/*/availability").permitAll()
                
                // Allow Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                
                // Camping Service
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/site-camping/addSite").hasAnyRole("ADMIN", "SITE_OWNER", "ORGANIZER")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/site-camping/updateSite/**").hasAnyRole("ADMIN", "SITE_OWNER", "ORGANIZER")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/site-camping/close/**").hasAnyRole("ADMIN", "SITE_OWNER", "ORGANIZER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/site-camping/my-sites").hasAnyRole("ADMIN", "SITE_OWNER", "ORGANIZER")
                
                // Booking Service
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/inscriptionsite/add").hasAnyRole("ADMIN", "CAMPER", "USER")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/inscriptionsite/cancel/**").hasAnyRole("ADMIN", "CAMPER", "USER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/inscriptionsite/my-inscriptions/**").hasAnyRole("ADMIN", "CAMPER", "USER")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/inscriptionsite/confirm-payment/**").hasAnyRole("ADMIN", "CAMPER", "USER", "SITE_OWNER", "ORGANIZER")
                
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/inscriptionsite/bySite/**").hasAnyRole("ADMIN", "SITE_OWNER", "ORGANIZER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/inscriptionsite/my-camp-booking-list/**").hasAnyRole("ADMIN", "SITE_OWNER", "ORGANIZER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/inscriptionsite/getAll").hasRole("ADMIN")
                
                // Secure all other endpoints
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
            
        return http.build();
    }

    @Bean
    public org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter() {
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    static final class KeycloakRealmRoleConverter
            implements org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, java.util.Collection<org.springframework.security.core.GrantedAuthority>> {

        @Override
        public java.util.Collection<org.springframework.security.core.GrantedAuthority> convert(org.springframework.security.oauth2.jwt.Jwt jwt) {
            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
            Object realmAccessClaim = jwt.getClaims().get("realm_access");
            if (!(realmAccessClaim instanceof java.util.Map<?, ?> realmAccess)) {
                return authorities;
            }

            Object rolesClaim = realmAccess.get("roles");
            if (!(rolesClaim instanceof java.util.Collection<?> roles)) {
                return authorities;
            }

            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::toUpperCase)
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
            return authorities;
        }
    }
}

package com.findmyfaculty.config;

import com.findmyfaculty.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfiguration {
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean public UserDetailsService userDetailsService(UserRepository users){return username->{
        com.findmyfaculty.domain.UserAccount a=users.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("User not found"));
        return User.withUsername(a.username).password(a.passwordHash).roles(a.role.name()).disabled(!a.enabled).build();
    };}
    @Bean public org.springframework.boot.web.servlet.FilterRegistrationBean<ActiveAccountFilter> activeAccountFilterRegistration(ActiveAccountFilter filter){org.springframework.boot.web.servlet.FilterRegistrationBean<ActiveAccountFilter> registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;}
    @Bean public SecurityFilterChain filterChain(HttpSecurity http,ActiveAccountFilter activeAccountFilter,ObjectProvider<PreviewBearerAuthenticationFilter> previewBearerFilterProvider,@Value("${app.preview.allow-frames:false}") boolean allowFrames) throws Exception {
        http.csrf().disable().cors().and().exceptionHandling().authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)).and().authorizeRequests()
            .antMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/api/health").permitAll()
            .antMatchers(HttpMethod.GET,"/api/preview/demo-logins").permitAll()
            .antMatchers(HttpMethod.POST,"/api/auth/login","/api/auth/logout").permitAll()
            .antMatchers(HttpMethod.GET,"/api/faculty/*/appointments").hasAnyRole("FACULTY","ADMIN")
            .antMatchers("/api/admin/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.POST,"/api/faculty/*/status").hasAnyRole("FACULTY","ADMIN")
            .antMatchers(HttpMethod.POST,"/api/appointments").hasRole("STUDENT")
            .antMatchers(HttpMethod.GET,"/api/faculty/**","/api/rooms/**","/api/sections/**").permitAll()
            .antMatchers("/api/appointments/**").authenticated()
            .anyRequest().authenticated();
        PreviewBearerAuthenticationFilter previewBearerFilter=previewBearerFilterProvider.getIfAvailable();
        if(previewBearerFilter!=null){http.addFilterAfter(previewBearerFilter,org.springframework.security.web.context.SecurityContextPersistenceFilter.class);http.addFilterAfter(activeAccountFilter,PreviewBearerAuthenticationFilter.class);}
        else http.addFilterAfter(activeAccountFilter,org.springframework.security.web.context.SecurityContextPersistenceFilter.class);
        if(allowFrames)http.headers().frameOptions().disable();else http.headers().frameOptions().deny();
        return http.build();
    }
    @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins){
        CorsConfiguration c=new CorsConfiguration();c.setAllowedOriginPatterns(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toList()));c.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",c);return source;
    }
}

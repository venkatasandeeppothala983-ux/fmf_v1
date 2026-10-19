package com.findmyfaculty.api;

import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import com.findmyfaculty.service.PreviewTokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final ObjectProvider<PreviewTokenService> previewTokens;

    public AuthController(UserRepository users, PasswordEncoder passwords,ObjectProvider<PreviewTokenService> previewTokens) {
        this.users = users;
        this.passwords = passwords;
        this.previewTokens=previewTokens;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest input, HttpServletRequest request) {
        String username = input.username == null ? "" : input.username.trim();
        UserAccount user = users.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!user.enabled || input.password == null || !passwords.matches(input.password, user.passwordHash) ||
            (input.loginType != null && !input.loginType.trim().isEmpty() && !user.role.name().equalsIgnoreCase(input.loginType.trim()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        HttpSession existing = request.getSession(false);
        if (existing != null) request.changeSessionId();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.role.name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        Map<String,Object> result=accountView(user);
        previewTokens.ifAvailable(tokens->result.put("previewToken",tokens.issue(user)));
        return result;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        String authorization=request.getHeader("Authorization");
        if(authorization!=null&&authorization.startsWith("Bearer "))previewTokens.ifAvailable(tokens->tokens.revoke(authorization.substring(7).trim()));
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return Collections.singletonMap("status", "ok");
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        UserAccount user = users.findByUsername(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return accountView(user);
    }

    private Map<String, Object> accountView(UserAccount user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.id);
        result.put("username", user.username);
        result.put("role", user.role.name());
        result.put("facultyId", user.faculty == null ? null : user.faculty.id);
        result.put("facultyName", user.faculty == null ? null : user.faculty.name);
        result.put("sectionId", user.section == null ? null : user.section.id);
        result.put("sectionName", user.section == null ? null : user.section.displayName());
        return result;
    }

    public static class LoginRequest {
        public String username;
        public String password;
        public String loginType;
    }
}

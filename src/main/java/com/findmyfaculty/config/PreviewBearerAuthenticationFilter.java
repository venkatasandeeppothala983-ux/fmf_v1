package com.findmyfaculty.config;

import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.service.PreviewTokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/** Authenticates the short-lived preview token when browser cookie partitioning blocks JSESSIONID. */
@Component
@Profile("preview")
public class PreviewBearerAuthenticationFilter extends OncePerRequestFilter {
    private final PreviewTokenService tokens;
    public PreviewBearerAuthenticationFilter(PreviewTokenService tokens){this.tokens=tokens;}

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")){
            tokens.resolve(header.substring(7).trim()).ifPresent(user->{
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    user.username,null,Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+user.role.name()))));
            });
        }
        chain.doFilter(request,response);
    }
}

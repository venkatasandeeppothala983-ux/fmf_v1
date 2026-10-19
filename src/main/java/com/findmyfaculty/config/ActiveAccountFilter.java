package com.findmyfaculty.config;

import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public ActiveAccountFilter(UserRepository users){this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof String&&!("anonymousUser".equals(auth.getPrincipal()))){
            UserAccount user=users.findByUsername((String)auth.getPrincipal()).orElse(null);
            if(user==null||!user.enabled){
                SecurityContextHolder.clearContext();
                if(request.getSession(false)!=null)request.getSession(false).invalidate();
            }
        }
        chain.doFilter(request,response);
    }
}

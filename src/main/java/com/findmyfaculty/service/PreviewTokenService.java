package com.findmyfaculty.service;

import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.repo.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived, in-memory bearer fallback for the embedded H2 preview only. */
@Service
@Profile("preview")
public class PreviewTokenService {
    private static final long TTL_SECONDS=30*60;
    private final UserRepository users;
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Entry> tokens=new ConcurrentHashMap<>();

    public PreviewTokenService(UserRepository users){this.users=users;}

    public String issue(UserAccount user){
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token,new Entry(user.username,Instant.now().plusSeconds(TTL_SECONDS)));return token;
    }

    public Optional<UserAccount> resolve(String token){
        if(token==null||token.trim().isEmpty())return Optional.empty();Entry entry=tokens.get(token);
        if(entry==null)return Optional.empty();if(!entry.expiresAt.isAfter(Instant.now())){tokens.remove(token);return Optional.empty();}
        Optional<UserAccount> user=users.findByUsername(entry.username);
        if(!user.isPresent()||!user.get().enabled){tokens.remove(token);return Optional.empty();}
        return user;
    }

    public void revoke(String token){if(token!=null)tokens.remove(token);}

    private static class Entry {
        final String username;final Instant expiresAt;
        Entry(String username,Instant expiresAt){this.username=username;this.expiresAt=expiresAt;}
    }
}

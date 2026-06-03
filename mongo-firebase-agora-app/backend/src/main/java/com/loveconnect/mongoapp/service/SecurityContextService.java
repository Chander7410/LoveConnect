package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.security.FirebasePrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityContextService {
    public FirebasePrincipal currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof FirebasePrincipal principal)) {
            throw new IllegalStateException("No authenticated Firebase user found");
        }
        return principal;
    }
}

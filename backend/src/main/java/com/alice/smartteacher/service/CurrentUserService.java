package com.alice.smartteacher.service;

import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.security.AppUserDetails;
import com.alice.smartteacher.web.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public UserAccount get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails)) {
            throw new ApiException(401, "Please log in.");
        }
        Long id = ((AppUserDetails) auth.getPrincipal()).getId();
        return users.findById(id).orElseThrow(() -> new ApiException(401, "Please log in again."));
    }
}

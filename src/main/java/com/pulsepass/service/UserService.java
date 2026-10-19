package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

    
    UserResponse register(RegisterUserRequest request);

    @PreAuthorize("@authz.canAccessUserByEmail(#p0)")
    UserResponse findByEmail(String email);

    @PreAuthorize("@authz.canAccessUserByUsername(#p0)")
    UserResponse findByUsername(String username);
}

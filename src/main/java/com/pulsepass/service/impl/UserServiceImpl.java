package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (isBlank(request.username())) {
            throw new BusinessRuleException("Username is required.");
        }
        if (isBlank(request.email())) {
            throw new BusinessRuleException("Email is required.");
        }
        // BR-USER-005: la fecha de nacimiento no puede ser futura
        if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future.");
        }

        // El email se normaliza para que la unicidad no dependa de mayúsculas/minúsculas
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim();

        // BR-USER-001
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }
        // BR-USER-002
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        // BR-USER-003: activo por defecto
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setActive(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setCity(request.city());
        profile.setBirthDate(request.birthDate());
        profile.setUser(user);
        user.setProfile(profile);

        // BR-USER-004: User + UserProfile en la misma transacción (cascade ALL desde User)
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("User", email));
    }

    @Override
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("User", username));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

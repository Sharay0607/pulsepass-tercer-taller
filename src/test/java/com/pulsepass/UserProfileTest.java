package com.pulsepass;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void shouldAllowOnlyOneProfilePerUser() {
        User user = new User();
        user.setUsername("andrea");
        user.setEmail("andrea@pulsepass.com");
        userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setFirstName("Andrea");
        profile.setLastName("Gómez");
        profile.setUser(user);
        userProfileRepository.saveAndFlush(profile);

        // Intento de un SEGUNDO perfil para el mismo usuario -> debe violar UNIQUE
        UserProfile secondProfile = new UserProfile();
        secondProfile.setFirstName("Andrea-Duplicado");
        secondProfile.setUser(user);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(secondProfile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
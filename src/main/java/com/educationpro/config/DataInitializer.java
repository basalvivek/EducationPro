package com.educationpro.config;

import com.educationpro.domain.Role;
import com.educationpro.domain.User;
import com.educationpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createDefaultAdmin();
    }

    private void createDefaultAdmin() {
        String adminEmail = "admin@educationpro.com";
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode("Admin@1234"));
        admin.setFullName("Super Admin");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Default admin created: {}", adminEmail);
    }
}

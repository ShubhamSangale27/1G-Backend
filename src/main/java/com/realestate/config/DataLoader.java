package com.realestate.config;

import com.realestate.entity.User;
import com.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@realestate.com").isEmpty()) {
            User admin = User.builder()
                    .email("admin@realestate.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Admin User")
                    .mobile("+919876543210")
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .mobileVerified(true)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user: admin@realestate.com / admin123");
        }
        if (userRepository.findByEmail("agent@realestate.com").isEmpty()) {
            User agent = User.builder()
                    .email("agent@realestate.com")
                    .passwordHash(passwordEncoder.encode("agent123"))
                    .fullName("Test Agent")
                    .mobile("+919876543211")
                    .role(User.Role.AGENT)
                    .emailVerified(true)
                    .mobileVerified(true)
                    .active(true)
                    .build();
            userRepository.save(agent);
            log.info("Created test agent: agent@realestate.com / agent123");
        }
        if (userRepository.findByEmail("builder@realestate.com").isEmpty()) {
            User builder = User.builder()
                    .email("builder@realestate.com")
                    .passwordHash(passwordEncoder.encode("builder123"))
                    .fullName("Test Builder")
                    .mobile("+919876543212")
                    .role(User.Role.USER)
                    .emailVerified(true)
                    .mobileVerified(true)
                    .active(true)
                    .build();
            userRepository.save(builder);
            log.info("Created test builder (user): builder@realestate.com / builder123");
        }
        if (userRepository.findByEmail("user@realestate.com").isEmpty()) {
            User user = User.builder()
                    .email("user@realestate.com")
                    .passwordHash(passwordEncoder.encode("user123"))
                    .fullName("Test User")
                    .mobile("+919876543213")
                    .role(User.Role.USER)
                    .emailVerified(true)
                    .mobileVerified(true)
                    .active(true)
                    .build();
            userRepository.save(user);
            log.info("Created test user: user@realestate.com / user123");
        }
    }
}

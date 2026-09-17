package com.marketgrid.userservice.config;

import com.marketgrid.userservice.entity.Role;
import com.marketgrid.userservice.entity.User;
import com.marketgrid.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default administrative data on startup if not already present.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("Admin@12345"),
                    "admin@marketgrid.com",
                    Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Default admin account created");
        } else {
            log.info("Default admin account already exists, skipping");
        }
    }
}

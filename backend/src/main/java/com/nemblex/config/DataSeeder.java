package com.nemblex.config;

import com.nemblex.entity.AppUser;
import com.nemblex.entity.enums.Role;
import com.nemblex.repository.AppUserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record SeedUser(String name, String email, String rawPassword, Role role) {
    }

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("Admin Nemblex", "admin@nemblex.dev", "admin123", Role.ADMIN),
            new SeedUser("Beatriz Ruiz", "beatriz.ruiz@nemblex.dev", "supervisor123", Role.SUPERVISOR),
            new SeedUser("Ana Torres", "ana.torres@nemblex.dev", "technician123", Role.TECHNICIAN));

    private final AppUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (SeedUser seed : SEED_USERS) {
            userRepository.findByEmail(seed.email()).ifPresentOrElse(
                    existing -> {
                        if (existing.getPasswordHash() == null || !existing.getPasswordHash().startsWith("$2")) {
                            existing.setPasswordHash(passwordEncoder.encode(seed.rawPassword()));
                            userRepository.save(existing);
                            log.info("Updated password hash for {}", seed.email());
                        }
                    },
                    () -> {
                        userRepository.save(AppUser.builder()
                                .name(seed.name())
                                .email(seed.email())
                                .role(seed.role())
                                .passwordHash(passwordEncoder.encode(seed.rawPassword()))
                                .build());
                        log.info("Created {} {}", seed.role(), seed.email());
                    });
        }
    }
}

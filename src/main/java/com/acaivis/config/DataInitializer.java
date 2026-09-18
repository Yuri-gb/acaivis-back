package com.acaivis.config;

import com.acaivis.model.Admin;
import com.acaivis.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(AdminRepository repo, PasswordEncoder encoder,
            @Value("${admin.email}") String adminEmail,
            @Value("${admin.password}") String adminPassword,
            @Value("${driver.email}") String driverEmail,
            @Value("${driver.password}") String driverPassword) {
        return args -> {
            seed(repo, encoder, "Administrador", adminEmail, adminPassword, "ADMIN");
            seed(repo, encoder, "Entregador", driverEmail, driverPassword, "DELIVERER");
        };
    }

    private void seed(AdminRepository repo, PasswordEncoder encoder, String name, String email, String password, String role) {
        Admin user = repo.findByEmail(email).orElseGet(Admin::new);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setActive(true);
        user.setRole(role);
        repo.save(user);
    }
}

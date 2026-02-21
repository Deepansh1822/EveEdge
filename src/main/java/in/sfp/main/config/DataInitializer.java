package in.sfp.main.config;

import in.sfp.main.model.User;
import in.sfp.main.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create or update default admin user
        java.util.Optional<User> existingAdmin = userRepository.findByUsername("Deepansh");

        User admin;
        if (existingAdmin.isPresent()) {
            admin = existingAdmin.get();
            System.out.println("ℹ️  Updating existing Admin user 'Deepansh'");
        } else {
            admin = new User();
            admin.setUsername("Deepansh");
            System.out.println("✅ Creating new Admin user 'Deepansh'");
        }

        admin.setEmail("deepanshshakya987@gmail.com");
        admin.setPassword(passwordEncoder.encode("Deepansh@18"));
        admin.setMobileNumber("9599514750");
        admin.setOrganization("SFPedutech");

        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        roles.add("USER");
        admin.setRoles(roles);

        userRepository.save(admin);
        System.out.println("🚀 Admin user configured successfully.");
    }
}

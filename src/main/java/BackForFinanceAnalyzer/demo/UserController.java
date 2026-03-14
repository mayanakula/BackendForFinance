package BackForFinanceAnalyzer.demo;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import BackForFinanceAnalyzer.demo.models.User;
import BackForFinanceAnalyzer.demo.repositories.UserRepository;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/demo")
    public String demo() {
        return "Finance Analyzer is running";
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return this.userRepo.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String username = payload.get("username");
        String password = payload.get("password");
        if (password == null && payload.containsKey("passwordHash")) {
            password = payload.get("passwordHash");
        }
        String role = payload.getOrDefault("role", "user");

        Optional<User> u = this.userRepo.findByEmail(email);
        if (u.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPasswordHash(password);
        newUser.setRole(role);
        newUser.setIsActive(true);

        this.userRepo.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (password == null && payload.containsKey("passwordHash")) {
            password = payload.get("passwordHash");
        }

        Optional<User> uOpt = this.userRepo.findByEmail(email);
        if (uOpt.isPresent()) {
            User u = uOpt.get();
            if (u.getPasswordHash() != null && u.getPasswordHash().equals(password)) {
                return ResponseEntity.ok(u);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}

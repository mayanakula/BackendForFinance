package BackForFinanceAnalyzer.demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
    Users findByEmail(String email);
    java.util.List<Users> findByUsername(String username);
}
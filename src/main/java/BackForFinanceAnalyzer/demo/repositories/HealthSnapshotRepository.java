package BackForFinanceAnalyzer.demo.repositories;

import BackForFinanceAnalyzer.demo.models.HealthSnapshot;
import BackForFinanceAnalyzer.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthSnapshotRepository extends JpaRepository<HealthSnapshot, Long> {
    List<HealthSnapshot> findByUserOrderByCreatedAtDesc(User user);
}

package BackForFinanceAnalyzer.demo.repositories;

import BackForFinanceAnalyzer.demo.models.SavingsGoal;
import BackForFinanceAnalyzer.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUser(User user);
}

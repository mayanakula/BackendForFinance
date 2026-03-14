package BackForFinanceAnalyzer.demo.repositories;

import BackForFinanceAnalyzer.demo.models.Budget;
import BackForFinanceAnalyzer.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUser(User user);
    Optional<Budget> findByUserAndCategory(User user, String category);
}

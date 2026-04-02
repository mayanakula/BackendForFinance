package com.pfa.backend.repo;

import com.pfa.backend.domain.Budget;
import com.pfa.backend.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
	Optional<Budget> findByYearAndMonthAndCategory(int year, int month, Category category);

	List<Budget> findAllByYearAndMonth(int year, int month);
}


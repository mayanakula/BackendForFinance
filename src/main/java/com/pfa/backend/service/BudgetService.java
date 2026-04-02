package com.pfa.backend.service;

import com.pfa.backend.api.dto.BudgetStatus;
import com.pfa.backend.api.dto.UpsertBudgetRequest;
import com.pfa.backend.domain.Budget;
import com.pfa.backend.domain.Category;
import com.pfa.backend.domain.TransactionType;
import com.pfa.backend.repo.BudgetRepository;
import com.pfa.backend.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

	private final BudgetRepository budgetRepository;
	private final TransactionRepository transactionRepository;

	@Transactional
	public Budget upsert(UpsertBudgetRequest req) {
		Budget b = budgetRepository
			.findByYearAndMonthAndCategory(req.year(), req.month(), req.category())
			.orElseGet(Budget::new);
		b.setYear(req.year());
		b.setMonth(req.month());
		b.setCategory(req.category());
		b.setMonthlyLimit(req.monthlyLimit());
		return budgetRepository.save(b);
	}

	public List<Budget> list(int year, int month) {
		return budgetRepository.findAllByYearAndMonth(year, month);
	}

	public List<BudgetStatus> status(int year, int month) {
		YearMonth ym = YearMonth.of(year, month);
		LocalDate start = ym.atDay(1);
		LocalDate end = ym.atEndOfMonth();
		List<Budget> budgets = budgetRepository.findAllByYearAndMonth(year, month);
		List<BudgetStatus> out = new ArrayList<>();

		for (Budget b : budgets) {
			BigDecimal spent = transactionRepository
				.findFiltered(start, end, TransactionType.EXPENSE, b.getCategory())
				.stream()
				.map(t -> t.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

			BigDecimal remaining = b.getMonthlyLimit().subtract(spent);
			int pct = b.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0
				? 0
				: spent.multiply(BigDecimal.valueOf(100))
					.divide(b.getMonthlyLimit(), 0, RoundingMode.HALF_UP)
					.intValue();

			String level = pct >= 100 ? "EXCEEDED" : (pct >= 80 ? "WARNING" : "OK");
			out.add(new BudgetStatus(b.getCategory(), b.getMonthlyLimit(), spent, remaining, pct, level));
		}
		return out;
	}
}


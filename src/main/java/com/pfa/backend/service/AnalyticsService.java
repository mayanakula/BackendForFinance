package com.pfa.backend.service;

import com.pfa.backend.api.dto.BudgetStatus;
import com.pfa.backend.api.dto.analytics.*;
import com.pfa.backend.domain.Category;
import com.pfa.backend.domain.Transaction;
import com.pfa.backend.domain.TransactionType;
import com.pfa.backend.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

	private final TransactionRepository transactionRepository;
	private final BudgetService budgetService;

	public DashboardResponse dashboard(LocalDate start, LocalDate end) {
		LocalDate s = start == null ? LocalDate.now().minusMonths(6).withDayOfMonth(1) : start;
		LocalDate e = end == null ? LocalDate.now() : end;

		List<Transaction> txns = transactionRepository.findFiltered(s, e, null, null);

		BigDecimal totalIncome = sum(txns, TransactionType.INCOME);
		BigDecimal totalExpense = sum(txns, TransactionType.EXPENSE);
		BigDecimal net = totalIncome.subtract(totalExpense);

		List<CategoryTotal> byCategory = spendingByCategory(txns);
		List<MonthlyPoint> monthly = monthlyTrend(txns, s, e);
		List<DailyPoint> daily = dailyExpenseTrend(txns, Math.max(14, (int) (e.toEpochDay() - s.toEpochDay())));

		HealthScore health = healthScore(txns, e);
		Forecast forecast = forecastExpenses(txns, e);
		List<RecurringPayment> recurring = recurringPayments(txns);
		List<Anomaly> anomalies = anomalies(txns);
		List<Recommendation> recommendations = recommendations(totalIncome, totalExpense, byCategory, health);

		return new DashboardResponse(
			totalIncome,
			totalExpense,
			net,
			byCategory,
			monthly,
			daily,
			health,
			forecast,
			recurring,
			anomalies,
			recommendations
		);
	}

	private BigDecimal sum(List<Transaction> txns, TransactionType type) {
		return txns.stream()
			.filter(t -> t.getType() == type)
			.map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private List<CategoryTotal> spendingByCategory(List<Transaction> txns) {
		Map<Category, BigDecimal> map = txns.stream()
			.filter(t -> t.getType() == TransactionType.EXPENSE)
			.collect(Collectors.groupingBy(Transaction::getCategory,
				Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

		return map.entrySet().stream()
			.sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
			.map(e -> new CategoryTotal(e.getKey(), e.getValue()))
			.toList();
	}

	private List<MonthlyPoint> monthlyTrend(List<Transaction> txns, LocalDate start, LocalDate end) {
		YearMonth startYm = YearMonth.from(start);
		YearMonth endYm = YearMonth.from(end);
		Map<String, BigDecimal> income = new HashMap<>();
		Map<String, BigDecimal> expense = new HashMap<>();

		for (Transaction t : txns) {
			String key = YearMonth.from(t.getTxnDate()).toString();
			if (t.getType() == TransactionType.INCOME) income.merge(key, t.getAmount(), BigDecimal::add);
			else expense.merge(key, t.getAmount(), BigDecimal::add);
		}

		List<MonthlyPoint> out = new ArrayList<>();
		for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
			String key = ym.toString();
			out.add(new MonthlyPoint(key, income.getOrDefault(key, BigDecimal.ZERO), expense.getOrDefault(key, BigDecimal.ZERO)));
		}
		return out;
	}

	private List<DailyPoint> dailyExpenseTrend(List<Transaction> txns, int maxDays) {
		Map<LocalDate, BigDecimal> expense = txns.stream()
			.filter(t -> t.getType() == TransactionType.EXPENSE)
			.collect(Collectors.groupingBy(Transaction::getTxnDate,
				Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

	 List<LocalDate> days = expense.keySet().stream().sorted().toList();
	 if (days.isEmpty()) return List.of();
	 LocalDate start = days.get(Math.max(0, days.size() - maxDays));
	 LocalDate end = days.get(days.size() - 1);

	 List<DailyPoint> out = new ArrayList<>();
	 for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
	 	out.add(new DailyPoint(d.toString(), expense.getOrDefault(d, BigDecimal.ZERO)));
	 }
	 return out;
	}

	private HealthScore healthScore(List<Transaction> txns, LocalDate now) {
		YearMonth ym = YearMonth.from(now);
		LocalDate start = ym.atDay(1);
		LocalDate end = ym.atEndOfMonth();

		List<Transaction> monthTxns = txns.stream()
			.filter(t -> !t.getTxnDate().isBefore(start) && !t.getTxnDate().isAfter(end))
			.toList();

		BigDecimal income = sum(monthTxns, TransactionType.INCOME);
		BigDecimal expense = sum(monthTxns, TransactionType.EXPENSE);

		BigDecimal savingsRate = income.compareTo(BigDecimal.ZERO) <= 0
			? BigDecimal.ZERO
			: income.subtract(expense).max(BigDecimal.ZERO)
				.divide(income, 4, RoundingMode.HALF_UP);

		BigDecimal expenseToIncome = income.compareTo(BigDecimal.ZERO) <= 0
			? BigDecimal.ONE
			: expense.divide(income, 4, RoundingMode.HALF_UP);

		List<BudgetStatus> statuses = budgetService.status(ym.getYear(), ym.getMonthValue());
		int adherence = statuses.isEmpty()
			? 100
			: (int) Math.round(100.0 * statuses.stream().filter(s -> !"EXCEEDED".equals(s.level())).count() / statuses.size());

		int score = 0;
		score += clamp((int) Math.round(savingsRate.doubleValue() * 100), 0, 40);        // up to 40
		score += clamp(40 - (int) Math.round(expenseToIncome.doubleValue() * 40), 0, 40); // up to 40
		score += clamp((int) Math.round(adherence * 0.2), 0, 20);                         // up to 20

		return new HealthScore(clamp(score, 0, 100), savingsRate, expenseToIncome, adherence);
	}

	private Forecast forecastExpenses(List<Transaction> txns, LocalDate now) {
		// Simple moving average over last 3 months per category (expenses only)
		YearMonth end = YearMonth.from(now).minusMonths(1);
		List<YearMonth> months = List.of(end.minusMonths(2), end.minusMonths(1), end);

		Map<YearMonth, List<Transaction>> byMonth = txns.stream()
			.filter(t -> t.getType() == TransactionType.EXPENSE)
			.collect(Collectors.groupingBy(t -> YearMonth.from(t.getTxnDate())));

		Map<Category, BigDecimal> avgByCategory = new EnumMap<>(Category.class);
		for (Category c : Category.values()) {
			BigDecimal sum = BigDecimal.ZERO;
			int count = 0;
			for (YearMonth m : months) {
				BigDecimal total = byMonth.getOrDefault(m, List.of()).stream()
					.filter(t -> t.getCategory() == c)
					.map(Transaction::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
				sum = sum.add(total);
				count++;
			}
			avgByCategory.put(c, count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
		}

		YearMonth next = YearMonth.from(now).plusMonths(1);
		List<CategoryTotal> predicted = avgByCategory.entrySet().stream()
			.map(e -> new CategoryTotal(e.getKey(), e.getValue()))
			.filter(ct -> ct.amount().compareTo(BigDecimal.ZERO) > 0)
			.sorted(Comparator.comparing(CategoryTotal::amount).reversed())
			.toList();

		BigDecimal total = predicted.stream().map(CategoryTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Forecast(next.toString(), total, predicted);
	}

	private List<RecurringPayment> recurringPayments(List<Transaction> txns) {
		// Heuristic: same merchant + same amount appears >=3 times in last 6 months as expense
		List<Transaction> expenses = txns.stream()
			.filter(t -> t.getType() == TransactionType.EXPENSE)
			.filter(t -> t.getMerchant() != null && !t.getMerchant().isBlank())
			.toList();

		record Key(String merchant, BigDecimal amount, String currency) {}

		Map<Key, List<Transaction>> groups = expenses.stream()
			.collect(Collectors.groupingBy(t -> new Key(t.getMerchant().toLowerCase(Locale.ROOT), t.getAmount(), t.getCurrency())));

		return groups.entrySet().stream()
			.filter(e -> e.getValue().size() >= 3)
			.map(e -> {
				List<Transaction> sorted = e.getValue().stream().sorted(Comparator.comparing(Transaction::getTxnDate)).toList();
				Transaction last = sorted.get(sorted.size() - 1);
				return new RecurringPayment(
					last.getMerchant(),
					last.getAmount(),
					last.getCurrency(),
					sorted.size(),
					last.getTxnDate().toString()
				);
			})
			.sorted(Comparator.comparing(RecurringPayment::amount).reversed())
			.limit(20)
			.toList();
	}

	private List<Anomaly> anomalies(List<Transaction> txns) {
		// Category-based: mark expenses that are > mean + 2*stddev within that category (last 6 months)
		List<Transaction> expenses = txns.stream()
			.filter(t -> t.getType() == TransactionType.EXPENSE)
			.toList();

		Map<Category, List<Transaction>> byCat = expenses.stream().collect(Collectors.groupingBy(Transaction::getCategory));
		List<Anomaly> out = new ArrayList<>();

		for (Map.Entry<Category, List<Transaction>> e : byCat.entrySet()) {
			List<Transaction> list = e.getValue();
			if (list.size() < 10) continue;
			double mean = list.stream().map(Transaction::getAmount).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
			double variance = list.stream().map(Transaction::getAmount).mapToDouble(a -> {
				double d = a.doubleValue() - mean;
				return d * d;
			}).average().orElse(0);
			double std = Math.sqrt(variance);
			double threshold = mean + 2 * std;

			for (Transaction t : list) {
				if (t.getAmount().doubleValue() >= threshold && threshold > 0) {
					out.add(new Anomaly(
						t.getId(),
						t.getTxnDate().toString(),
						t.getMerchant(),
						t.getCategory(),
						t.getAmount(),
						"Unusually high for " + t.getCategory()
					));
				}
			}
		}

		return out.stream()
			.sorted(Comparator.comparing((Anomaly a) -> a.amount()).reversed())
			.limit(20)
			.toList();
	}

	private List<Recommendation> recommendations(BigDecimal income, BigDecimal expense, List<CategoryTotal> byCategory, HealthScore health) {
		List<Recommendation> out = new ArrayList<>();

		if (health.expenseToIncomeRatio().compareTo(new BigDecimal("0.90")) >= 0) {
			out.add(new Recommendation(
				"Reduce expense-to-income ratio",
				"Your expenses are close to your income. Try capping non-essential spend and setting category budgets.",
				"BUDGET"
			));
		}

		if (health.savingsRate().compareTo(new BigDecimal("0.20")) < 0) {
			out.add(new Recommendation(
				"Improve savings rate",
				"Target at least 20% savings. Consider automating transfers on payday and using the 50/30/20 rule.",
				"SAVE"
			));
		}

		if (!byCategory.isEmpty()) {
			CategoryTotal top = byCategory.get(0);
			out.add(new Recommendation(
				"Cut back on " + pretty(top.category()),
				"This is your highest spending category in the selected period. Set a budget and track weekly trends.",
				"SPEND"
			));
		}

		if (income.compareTo(BigDecimal.ZERO) == 0) {
			out.add(new Recommendation(
				"Record income transactions",
				"Add income entries so health score and ratios become more accurate.",
				"BUDGET"
			));
		}

		return out;
	}

	private String pretty(Category c) {
		return c.name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	private int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}


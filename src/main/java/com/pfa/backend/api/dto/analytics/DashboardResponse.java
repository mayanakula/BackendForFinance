package com.pfa.backend.api.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
	BigDecimal totalIncome,
	BigDecimal totalExpense,
	BigDecimal net,
	List<CategoryTotal> spendingByCategory,
	List<MonthlyPoint> monthlyTrend,
	List<DailyPoint> dailyExpenseTrend,
	HealthScore healthScore,
	Forecast forecast,
	List<RecurringPayment> recurringPayments,
	List<Anomaly> anomalies,
	List<Recommendation> recommendations
) {}


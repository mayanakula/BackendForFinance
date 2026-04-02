package com.pfa.backend.api.dto.analytics;

import java.math.BigDecimal;

public record HealthScore(
	int score, // 0..100
	BigDecimal savingsRate,
	BigDecimal expenseToIncomeRatio,
	int budgetAdherencePercent
) {}


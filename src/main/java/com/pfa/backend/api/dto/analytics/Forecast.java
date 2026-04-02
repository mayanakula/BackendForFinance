package com.pfa.backend.api.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record Forecast(
	String nextMonth, // yyyy-MM
	BigDecimal predictedTotalExpense,
	List<CategoryTotal> predictedByCategory
) {}


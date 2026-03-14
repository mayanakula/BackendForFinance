package com.pfa.backend.api.dto.analytics;

import java.math.BigDecimal;

public record MonthlyPoint(
	String month, // yyyy-MM
	BigDecimal income,
	BigDecimal expense
) {}


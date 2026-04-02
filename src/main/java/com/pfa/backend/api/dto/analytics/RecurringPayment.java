package com.pfa.backend.api.dto.analytics;

import java.math.BigDecimal;

public record RecurringPayment(
	String merchant,
	BigDecimal amount,
	String currency,
	int occurrences,
	String lastDate // yyyy-MM-dd
) {}


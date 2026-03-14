package com.pfa.backend.api.dto.analytics;

import com.pfa.backend.domain.Category;

import java.math.BigDecimal;

public record Anomaly(
	Long transactionId,
	String date, // yyyy-MM-dd
	String merchant,
	Category category,
	BigDecimal amount,
	String reason
) {}


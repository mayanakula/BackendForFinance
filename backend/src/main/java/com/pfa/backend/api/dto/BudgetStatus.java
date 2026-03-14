package com.pfa.backend.api.dto;

import com.pfa.backend.domain.Category;

import java.math.BigDecimal;

public record BudgetStatus(
	Category category,
	BigDecimal limit,
	BigDecimal spent,
	BigDecimal remaining,
	int usedPercent,
	String level // OK | WARNING | EXCEEDED
) {}


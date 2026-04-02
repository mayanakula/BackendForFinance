package com.pfa.backend.api.dto.analytics;

import com.pfa.backend.domain.Category;

import java.math.BigDecimal;

public record CategoryTotal(
	Category category,
	BigDecimal amount
) {}


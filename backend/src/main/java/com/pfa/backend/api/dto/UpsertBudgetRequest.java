package com.pfa.backend.api.dto;

import com.pfa.backend.domain.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpsertBudgetRequest(
	@NotNull Integer year,
	@NotNull @Min(1) @Max(12) Integer month,
	@NotNull Category category,
	@NotNull BigDecimal monthlyLimit
) {}


package com.pfa.backend.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateGoalSavedAmountRequest(
	@NotNull BigDecimal savedAmount
) {}


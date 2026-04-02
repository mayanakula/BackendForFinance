package com.pfa.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
	@NotBlank String name,
	@NotNull BigDecimal targetAmount,
	BigDecimal savedAmount,
	LocalDate targetDate
) {}


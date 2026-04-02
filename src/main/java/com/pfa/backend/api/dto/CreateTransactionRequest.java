package com.pfa.backend.api.dto;

import com.pfa.backend.domain.Category;
import com.pfa.backend.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
	@NotNull LocalDate txnDate,
	@NotBlank String description,
	String merchant,
	@NotNull BigDecimal amount,
	@NotBlank String currency,
	@NotNull TransactionType type,
	@NotNull Category category
) {}


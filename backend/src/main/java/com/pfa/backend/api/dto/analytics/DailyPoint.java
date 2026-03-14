package com.pfa.backend.api.dto.analytics;

import java.math.BigDecimal;

public record DailyPoint(
	String date, // yyyy-MM-dd
	BigDecimal expense
) {}


package com.pfa.backend.api.dto.analytics;

public record Recommendation(
	String title,
	String detail,
	String type // SPEND | SAVE | BUDGET
) {}


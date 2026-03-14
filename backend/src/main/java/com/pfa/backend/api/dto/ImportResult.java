package com.pfa.backend.api.dto;

public record ImportResult(
	int processed,
	int inserted,
	int duplicatesSkipped,
	int failed
) {}

